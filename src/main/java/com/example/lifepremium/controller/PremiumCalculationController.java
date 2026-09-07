package com.example.lifepremium.controller;

import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.ApiResponse;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;
import com.example.lifepremium.service.PremiumCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 保費試算 API（FR-CALC-001）
 * POST /api/v1/premium/calculate
 *
 * 認證選用：有 X-Agent-Id Header 視為業務員（保存紀錄），無則視為訪客。
 */
@RestController
@RequestMapping("/api/v1/premium")
@RequiredArgsConstructor
@Tag(name = "Premium Calculation API", description = "壽險保費試算")
public class PremiumCalculationController {

    private final PremiumCalculationService calculationService;

    @PostMapping("/calculate")
    @Operation(summary = "保費試算", description = "計算年繳及月繳保費，業務員登入後試算結果自動保存")
    public ResponseEntity<ApiResponse<PremiumCalculateResponse>> calculate(
            @Valid @RequestBody PremiumCalculateRequest request,
            @RequestHeader(value = "X-Agent-Id", required = false) UUID agentId
    ) {
        PremiumCalculateResponse result = calculationService.calculate(request, agentId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
