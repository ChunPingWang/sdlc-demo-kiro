package com.example.lifepremium.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalculationRecordResponse(
        UUID       id,
        String     productCode,
        Integer    insuredAge,
        String     insuredGender,
        Integer    insuredAmount,
        Integer    paymentPeriod,
        BigDecimal rateUsed,
        Integer    annualPremium,
        Integer    monthlyPremium,
        String     status,
        String     failureReason,
        Instant    createdAt
) {}
