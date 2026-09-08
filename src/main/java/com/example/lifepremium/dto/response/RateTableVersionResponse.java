package com.example.lifepremium.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record RateTableVersionResponse(
        UUID      versionId,
        String    productCode,
        Integer   versionNumber,
        LocalDate effectiveDate,
        String    status,
        Integer   entryCount,
        String    filePath
) {}
