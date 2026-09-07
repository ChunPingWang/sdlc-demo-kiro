package com.example.lifepremium.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RateTableVersionResponse(
    UUID      versionId,
    String    productCode,
    int       versionNumber,
    LocalDate effectiveDate,
    String    status,
    int       entryCount,
    String    filePath,
    Instant   createdAt
) {}
