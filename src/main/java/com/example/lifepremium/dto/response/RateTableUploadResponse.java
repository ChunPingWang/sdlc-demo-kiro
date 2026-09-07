package com.example.lifepremium.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record RateTableUploadResponse(
    UUID      versionId,
    String    productCode,
    LocalDate effectiveDate,
    Integer   entryCount,
    String    filePath
) {}
