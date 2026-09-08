package com.example.lifepremium.service;

import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;

import java.util.UUID;

public interface PremiumCalculationService {

    /**
     * 執行保費試算。
     *
     * @param request  試算輸入參數
     * @param agentId  業務員 ID（訪客傳 null，不保存試算紀錄）
     * @return 試算結果（含年繳、月繳保費）
     */
    PremiumCalculateResponse calculate(PremiumCalculateRequest request, UUID agentId);
}
