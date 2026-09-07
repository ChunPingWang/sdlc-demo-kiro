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
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * 保費試算核心邏輯（BR-001 ~ BR-005）
 *
 * 計算公式（BR-004, BR-005）：
 *   annualPremium  = ROUND(insuredAmount / 1000.0 × rate)
 *   monthlyPremium = ROUND(annualPremium / 12.0 × 1.03)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PremiumCalculationServiceImpl implements PremiumCalculationService {

    private static final int    MIN_AGE    = 0;
    private static final int    MAX_AGE    = 70;
    private static final int    MIN_AMOUNT = 100;   // 萬元
    private static final int    MAX_AMOUNT = 5000;  // 萬元
    private static final Set<Integer> VALID_PERIODS = Set.of(10, 20, 30, 99);
    private static final double MONTHLY_LOADING    = 1.03;

    private final RateEntryRepository       rateEntryRepository;
    private final CalculationRecordRepository calculationRecordRepository;

    @Override
    @Transactional
    public PremiumCalculateResponse calculate(PremiumCalculateRequest request, UUID agentId) {
        log.info("保費試算開始: productCode={}, age={}, gender={}, amount={}, period={}, agentId={}",
                request.productCode(), request.age(), request.gender(),
                request.insuredAmount(), request.paymentPeriod(), agentId);

        // ── BR-001：年齡驗證 ──
        if (request.age() < MIN_AGE || request.age() > MAX_AGE) {
            throw new AgeOutOfRangeException(request.age());
        }

        // ── BR-002：保額驗證 ──
        if (request.insuredAmount() < MIN_AMOUNT || request.insuredAmount() > MAX_AMOUNT) {
            throw new AmountOutOfRangeException(request.insuredAmount());
        }

        // ── BR-003：繳費年期驗證 ──
        if (!VALID_PERIODS.contains(request.paymentPeriod())) {
            throw new InvalidPaymentPeriodException(request.paymentPeriod());
        }

        // ── 費率查詢（Cache-Aside，見 RateQueryService）──
        BigDecimal rate = findRate(
                request.productCode(), request.age(),
                request.gender(),      request.paymentPeriod());

        // ── BR-004：年繳保費 = ROUND(保額 / 1000 × 費率) ──
        int annualPremium = Math.round(
                (float) (request.insuredAmount() / 1000.0 * rate.doubleValue()));

        // ── BR-005：月繳保費 = ROUND(年繳 / 12 × 1.03) ──
        int monthlyPremium = Math.round(
                (float) (annualPremium / 12.0 * MONTHLY_LOADING));

        // ── 保存試算紀錄（業務員才保存，訪客 agentId=null 不保存）──
        UUID calculationId = null;
        if (agentId != null) {
            CalculationRecord record = CalculationRecord.success(
                    agentId, request.productCode(),
                    request.age(), request.gender(),
                    request.insuredAmount(), request.paymentPeriod(),
                    rate, annualPremium, monthlyPremium);
            calculationId = calculationRecordRepository.save(record).getId();
        }

        log.info("保費試算完成: annualPremium={}, monthlyPremium={}, id={}",
                annualPremium, monthlyPremium, calculationId);

        return new PremiumCalculateResponse(
                calculationId,
                request.productCode(),
                request.insuredAmount(),
                request.paymentPeriod(),
                rate,
                annualPremium,
                monthlyPremium
        );
    }

    /**
     * Cache-Aside 費率查詢
     * Key: rate:{productCode}:{age}:{gender}:{paymentPeriod}
     */
    @Cacheable(
        value  = "rates",
        key    = "#productCode + ':' + #age + ':' + #gender + ':' + #paymentPeriod",
        unless = "#result == null"
    )
    public BigDecimal findRate(String productCode, int age, String gender, int paymentPeriod) {
        return rateEntryRepository
                .findEffectiveRate(productCode, age, gender, paymentPeriod, LocalDate.now())
                .orElseThrow(() -> new RateNotFoundException(productCode, age, gender, paymentPeriod));
    }
}
