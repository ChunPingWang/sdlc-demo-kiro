package com.example.lifepremium.controller;

import com.example.lifepremium.dto.request.RateTableUploadRequest;
import com.example.lifepremium.dto.response.ApiResponse;
import com.example.lifepremium.dto.response.RateTableUploadResponse;
import com.example.lifepremium.service.RateTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 費率表管理 API
 * SD §8.3  POST /api/v1/rate-tables
 */
@RestController
@RequestMapping("/api/v1/rate-tables")
@RequiredArgsConstructor
@Tag(name = "Rate Table", description = "費率表版本管理")
public class RateTableController {

    private final RateTableService rateTableService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "上傳費率表", description = "上傳 CSV 費率表並建立新版本（僅 ADMIN）")
    public ResponseEntity<ApiResponse<RateTableUploadResponse>> uploadRateTable(
            @RequestPart("file") MultipartFile file,
            @RequestParam String productCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestHeader("X-Admin-Id") UUID adminId
    ) {
        RateTableUploadRequest request = new RateTableUploadRequest(productCode, effectiveDate);
        RateTableUploadResponse response = rateTableService.uploadRateTable(request, file, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
