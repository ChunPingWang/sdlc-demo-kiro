package com.example.lifepremium.service;

import java.math.BigDecimal;

public interface RateQueryService {

    /**
     * 依 Cache-Aside 策略查詢有效費率。
     * Cache Miss 時查詢 DB 並回寫 Redis。
     *
     * @return 費率（每千元保額）
     * @throws com.example.lifepremium.exception.RateNotFoundException 查無費率時拋出
     */
    BigDecimal findRate(String productCode, int age, String gender, int paymentPeriod);

    /** 清除指定商品的所有費率快取（費率表上傳後呼叫） */
    void evictByProductCode(String productCode);
}
