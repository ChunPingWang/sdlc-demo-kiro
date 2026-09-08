package com.example.lifepremium.exception;

/** BR-002：保額須介於 100 萬至 5000 萬 */
public class AmountOutOfRangeException extends BusinessException {
    public AmountOutOfRangeException(int amount) {
        super(ErrorCode.AMOUNT_OUT_OF_RANGE,
              String.format("保額須介於 100 萬至 5000 萬，輸入值：%d 萬", amount));
    }
}
