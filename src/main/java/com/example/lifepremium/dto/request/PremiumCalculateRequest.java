package com.example.lifepremium.dto.request;

import jakarta.validation.constraints.*;

/**
 * 保費試算請求 DTO
 * 對應 POST /api/v1/premium/calculate
 * 業務規則驗證（BR-001~BR-003）在 Service 層處理；此處只做格式驗證。
 */
public record PremiumCalculateRequest(

        @NotBlank(message = "商品代碼不可空白")
        @Size(max = 20, message = "商品代碼長度不可超過 20 字元")
        String productCode,

        @NotNull(message = "年齡不可為空")
        @Min(value = 0,   message = "年齡不可小於 0")
        @Max(value = 120, message = "年齡不可大於 120")
        Integer age,

        @NotBlank(message = "性別不可空白")
        @Pattern(regexp = "M|F", message = "性別僅接受 M 或 F")
        String gender,

        @NotNull(message = "保額不可為空")
        @Positive(message = "保額須為正整數")
        Integer insuredAmount,

        @NotNull(message = "繳費年期不可為空")
        Integer paymentPeriod

) {}
