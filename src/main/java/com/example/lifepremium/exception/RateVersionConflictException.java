package com.example.lifepremium.exception;

import java.time.LocalDate;

public class RateVersionConflictException extends BusinessException {
    public RateVersionConflictException(String productCode, LocalDate effectiveDate) {
        super(ErrorCode.RATE_VERSION_CONFLICT,
              String.format("商品 %s 在 %s 已存在費率版本", productCode, effectiveDate));
    }
}
