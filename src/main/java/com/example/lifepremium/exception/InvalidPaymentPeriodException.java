package com.example.lifepremium.exception;

/** BR-003：繳費年期須為 10、20、30、99 之一 */
public class InvalidPaymentPeriodException extends BusinessException {
    public InvalidPaymentPeriodException(int period) {
        super(ErrorCode.INVALID_PAYMENT_PERIOD,
              String.format("繳費年期須為 10、20、30 或 99 年，輸入值：%d", period));
    }
}
