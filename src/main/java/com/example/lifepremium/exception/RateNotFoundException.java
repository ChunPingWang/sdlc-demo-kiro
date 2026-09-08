package com.example.lifepremium.exception;

public class RateNotFoundException extends BusinessException {
    public RateNotFoundException(String productCode, int age, String gender, int period) {
        super(ErrorCode.RATE_NOT_FOUND,
              String.format("查無費率資料：productCode=%s, age=%d, gender=%s, period=%d",
                            productCode, age, gender, period));
    }
}
