package com.example.lifepremium.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 統一錯誤碼（對應 SD 文件 13.1 BusinessException 層次結構）
 */
@Getter
public enum ErrorCode {

    // ── 共用 ──
    VALIDATION_FAILED          (HttpStatus.BAD_REQUEST,            "VALIDATION_FAILED"),
    INTERNAL_SERVER_ERROR      (HttpStatus.INTERNAL_SERVER_ERROR,  "INTERNAL_SERVER_ERROR"),

    // ── 保費試算 BR-001~BR-003 ──
    AGE_OUT_OF_RANGE           (HttpStatus.UNPROCESSABLE_ENTITY,   "AGE_OUT_OF_RANGE"),
    AMOUNT_OUT_OF_RANGE        (HttpStatus.UNPROCESSABLE_ENTITY,   "AMOUNT_OUT_OF_RANGE"),
    INVALID_PAYMENT_PERIOD     (HttpStatus.UNPROCESSABLE_ENTITY,   "INVALID_PAYMENT_PERIOD"),
    RATE_NOT_FOUND             (HttpStatus.NOT_FOUND,              "RATE_NOT_FOUND"),

    // ── 費率表上傳 ──
    RATE_VERSION_CONFLICT      (HttpStatus.CONFLICT,               "RATE_VERSION_CONFLICT"),
    INVALID_CSV_FORMAT         (HttpStatus.BAD_REQUEST,            "INVALID_CSV_FORMAT"),
    INVALID_EFFECTIVE_DATE     (HttpStatus.UNPROCESSABLE_ENTITY,   "INVALID_EFFECTIVE_DATE"),
    PRODUCT_NOT_FOUND          (HttpStatus.NOT_FOUND,              "PRODUCT_NOT_FOUND");

    private final HttpStatus httpStatus;
    private final String     code;

    ErrorCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code       = code;
    }
}
