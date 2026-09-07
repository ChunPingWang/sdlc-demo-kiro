package com.example.lifepremium.dto.request;

import jakarta.validation.constraints.*;

/**
 * 保費試算請求 DTO（FR-CALC-001）
 * 注意：age/insuredAmount/paymentPeriod 的業務規則（BR-001~BR-003）
 * 在 PremiumCalculationService 層驗證，Controller 只做格式驗證。
 */
public record PremiumCalculateRequest(

    @NotBlank(message = "商品代碼不可為空")
    @Size(max = 20, message = "商品代碼最長 20 字元")
    String productCode,

    @NotNull(message = "年齡不可為空")
    @Min(value = 0,   message = "年齡不可小於 0")
    @Max(value = 120, message = "年齡不可大於 120")
    Integer age,

    @NotBlank(message = "性別不可為空")
    @Pattern(regexp = "^[MF]$", message = "性別須為 M 或 F")
    String gender,

    @NotNull(message = "保額不可為空")
    @Positive(message = "保額須為正整數")
    Integer insuredAmount,

    @NotNull(message = "繳費年期不可為空")
    Integer paymentPeriod

) {}
