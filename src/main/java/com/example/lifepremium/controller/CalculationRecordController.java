package com.example.lifepremium.controller;

import com.example.lifepremium.dto.response.ApiResponse;
import com.example.lifepremium.dto.response.CalculationRecordResponse;
import com.example.lifepremium.dto.response.PageResponse;
import com.example.lifepremium.service.CalculationRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 試算紀錄查詢 API（FR-RECORD-001）
 * GET /api/v1/calculation-records
 */
@RestController
@RequestMapping("/api/v1/calculation-records")
@RequiredArgsConstructor
@Tag(name = "Calculation Record API", description = "試算紀錄查詢")
public class CalculationRecordController {

    private final CalculationRecordService recordService;

    @GetMapping
    @Operation(summary = "查詢試算紀錄",
               description = "業務員只查詢自己的紀錄；Admin 可查詢所有紀錄")
    public ResponseEntity<ApiResponse<PageResponse<CalculationRecordResponse>>> query(
            @RequestHeader("X-Agent-Id") UUID agentId,
            @RequestHeader(value = "X-Roles", defaultValue = "") String roles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(90);
        LocalDate to   = toDate   != null ? toDate   : LocalDate.now();
        boolean isAdmin = List.of(roles.split(",")).contains("ROLE_ADMIN");

        PageResponse<CalculationRecordResponse> result =
                recordService.query(agentId, isAdmin, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
