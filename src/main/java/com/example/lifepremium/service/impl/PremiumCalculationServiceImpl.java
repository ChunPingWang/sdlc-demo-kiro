package com.example.lifepremium.service.impl;

import com.example.lifepremium.domain.CalculationRecord;
import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;
import com.example.lifepremium.exception.*;
import com.example.lifepremium.repository.CalculationRecordRepository;
import com.example.lifepremium.repository.RateEntryRepository;
import com.example.lifepremium.service.PremiumCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumCalculationServiceImpl implements PremiumCalculationService {

    private static final int    MIN_AGE    = 0;
    private static final int    MAX_AGE    = 70;
    private static final int    MIN_AMOUNT = 100;
    private static final int    MAX_AMOUNT = 5000;
    private static final Set<Integer> VALID_PERIODS = Set.of(10, 20, 30, 99);
    private static final BigDecimal MONTHLY_FACTOR = new BigDecimal("1.03");

    private final RateEntryRepository       rateEntryRepository;
    private final CalculationRecordRepository recordRepository;

    @Override
    @Transactional
    public PremiumCalculateResponse calculate(PremiumCalculateRequest req, UUID agentId) {
        log.info("試算開始: productCode={}, age={}, gender={}, amount={}, period={}, agentId={}",
                req.productCode(), req.age(), req.gender(),
                req.insuredAmount(), req.paymentPeriod(), agentId);

        // ── 業務規則驗證（BR-001 ~ BR-003）──
        if (req.age() < MIN_AGE || req.age() > MAX_AGE) {
            throw new AgeOutOfRangeException(req.age());
        }
        if (req.insuredAmount() < MIN_AMOUNT || req.insuredAmount() > MAX_AMOUNT) {
            throw new AmountOutOfRangeException(req.insuredAmount());
        }
        if (!VALID_PERIODS.contains(req.paymentPeriod())) {
            throw new InvalidPaymentPeriodException(req.paymentPeriod());
        }

        // ── 費率查詢（Cache-Aside 由 RateQueryService/Cache 層處理）──
        BigDecimal rate = lookupRate(req.productCode(), req.age(), req.gender(), req.paymentPeriod());

        // ── 保費計算（BR-004 / BR-005）──
        // annualPremium  = ROUND(insuredAmount / 1000 × rate)
        // monthlyPremium = ROUND(annualPremium / 12 × 1.03)
        int annualPremium = BigDecimal.valueOf(req.insuredAmount())
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        int monthlyPremium = BigDecimal.valueOf(annualPremium)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .multiply(MONTHLY_FACTOR)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        // ── 試算紀錄（業務員才保存）──
        CalculationRecord record = CalculationRecord.success(
                agentId, req.productCode(),
                req.age(), req.gender(), req.insuredAmount(), req.paymentPeriod(),
                rate, annualPremium, monthlyPremium);

        if (agentId != null) {
            recordRepository.save(record);
            log.info("試算紀錄已保存: calculationId={}, annualPremium={}", record.getId(), annualPremium);
        }

        return new PremiumCalculateResponse(
                record.getId(),
                req.productCode(),
                req.insuredAmount(),
                req.paymentPeriod(),
                rate,
                annualPremium,
                monthlyPremium
        );
    }

    /**
     * 費率查詢（先走 Spring Cache / Redis，Cache Miss 再查 DB）。
     * Cache key: rate::{productCode}::{age}::{gender}::{paymentPeriod}
     */
    @Cacheable(value = "rates",
               key = "#productCode + ':' + #age + ':' + #gender + ':' + #paymentPeriod")
    public BigDecimal lookupRate(String productCode, int age, String gender, int paymentPeriod) {
        return rateEntryRepository
                .findEffectiveRate(productCode, age, gender, paymentPeriod)
                .orElseThrow(() -> new RateNotFoundException(productCode, age, gender, paymentPeriod));
    }
}
