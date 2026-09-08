package com.example.lifepremium.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 保費試算回應 DTO
 * annualPremium  = ROUND(insuredAmount / 1000 × rate)        單位：元
 * monthlyPremium = ROUND(annualPremium / 12 × 1.03)          單位：元（BR-005）
 */
public record PremiumCalculateResponse(
        UUID           calculationId,
        String         productCode,
        Integer        insuredAmount,
        Integer        paymentPeriod,
        BigDecimal     rateUsed,
        Integer        annualPremium,
        Integer        monthlyPremium
) {}
