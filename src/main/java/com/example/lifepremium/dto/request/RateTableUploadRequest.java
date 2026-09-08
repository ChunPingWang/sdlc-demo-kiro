package com.example.lifepremium.dto.request;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 費率表上傳請求（Multipart Form）
 * 對應 POST /api/v1/rate-tables
 */
public record RateTableUploadRequest(

        @NotNull(message = "CSV 檔案不可為空")
        MultipartFile file,

        @NotBlank(message = "商品代碼不可空白")
        @Size(max = 20)
        String productCode,

        @NotNull(message = "生效日期不可為空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate effectiveDate

) {}
