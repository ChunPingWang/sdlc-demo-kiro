package com.example.lifepremium.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 保費試算回應 DTO（FR-CALC-001）
 *
 * annualPremium  = ROUND(insuredAmount / 1000 × rate)
 * monthlyPremium = ROUND(annualPremium / 12 × 1.03)   (BR-005)
 */
public record PremiumCalculateResponse(
    UUID          calculationId,
    String        productCode,
    Integer       insuredAmount,
    Integer       paymentPeriod,
    BigDecimal    rateUsed,
    Integer       annualPremium,
    Integer       monthlyPremium
) {}
