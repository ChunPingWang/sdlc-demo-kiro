package com.example.lifepremium.exception;

public class InvalidCsvFormatException extends BusinessException {
    public InvalidCsvFormatException(int lineNumber, String reason) {
        super(ErrorCode.INVALID_CSV_FORMAT,
              String.format("CSV 格式錯誤（第 %d 行）：%s", lineNumber, reason));
    }
}
