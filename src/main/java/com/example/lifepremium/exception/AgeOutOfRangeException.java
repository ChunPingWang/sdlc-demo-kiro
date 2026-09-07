package com.example.lifepremium.exception;

/** BR-001：被保人年齡須介於 0 至 70 歲 */
public class AgeOutOfRangeException extends BusinessException {
    public AgeOutOfRangeException(int age) {
        super(ErrorCode.AGE_OUT_OF_RANGE,
              String.format("被保人年齡須介於 0 至 70 歲，輸入值：%d", age));
    }
}
