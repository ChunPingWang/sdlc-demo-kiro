package com.example.lifepremium.exception;

/** BR-003：繳費年期僅接受 10、20、30、99 */
public class InvalidPaymentPeriodException extends BusinessException {
    public InvalidPaymentPeriodException(int period) {
        super(ErrorCode.INVALID_PAYMENT_PERIOD,
              String.format("繳費年期僅接受 10、20、30、99，輸入值：%d", period));
    }
}
