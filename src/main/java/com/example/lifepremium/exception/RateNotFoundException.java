package com.example.lifepremium.exception;

public class RateNotFoundException extends BusinessException {
    public RateNotFoundException(String productCode, int age, String gender, int period) {
        super(ErrorCode.RATE_NOT_FOUND,
              String.format("查無費率資料：商品=%s, 年齡=%d, 性別=%s, 繳費年期=%d",
                            productCode, age, gender, period));
    }
}
