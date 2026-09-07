package com.example.lifepremium.service;

import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;

import java.util.UUID;

/**
 * 保費試算服務介面（FR-CALC-001, FR-CALC-002）
 *
 * @param agentId 業務員 ID；訪客傳入 null，不保存試算紀錄
 */
public interface PremiumCalculationService {

    PremiumCalculateResponse calculate(PremiumCalculateRequest request, UUID agentId);
}
