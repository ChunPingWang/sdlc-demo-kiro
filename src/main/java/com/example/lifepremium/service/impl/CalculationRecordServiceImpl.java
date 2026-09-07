package com.example.lifepremium.service.impl;

import com.example.lifepremium.domain.CalculationRecord;
import com.example.lifepremium.dto.response.CalculationRecordResponse;
import com.example.lifepremium.dto.response.PageResponse;
import com.example.lifepremium.repository.CalculationRecordRepository;
import com.example.lifepremium.service.CalculationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculationRecordServiceImpl implements CalculationRecordService {

    private final CalculationRecordRepository recordRepository;

    @Override
    public PageResponse<CalculationRecordResponse> query(
            UUID agentId, boolean isAdmin,
            LocalDate fromDate, LocalDate toDate,
            Pageable pageable) {

        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // FR-RECORD-001：Admin 查全部，Agent 只查自己
        Page<CalculationRecord> page = isAdmin
                ? recordRepository.findAll(from, to, pageable)
                : recordRepository.findByAgentId(agentId, from, to, pageable);

        return PageResponse.of(page.map(this::toResponse));
    }

    private CalculationRecordResponse toResponse(CalculationRecord r) {
        return new CalculationRecordResponse(
                r.getId(),
                r.getProductCode(),
                r.getInsuredAge(),
                r.getInsuredGender(),
                r.getInsuredAmount(),
                r.getPaymentPeriod(),
                r.getAnnualPremium(),
                r.getMonthlyPremium(),
                r.getStatus().name(),
                r.getCreatedAt()
        );
    }
}
