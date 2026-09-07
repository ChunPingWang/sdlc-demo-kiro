package com.example.lifepremium.fixture;

import com.example.lifepremium.domain.*;
import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 測試資料工廠
 * 所有測試類別統一從此取得 fixture，避免 hard-coded 測試資料散落。
 */
public class PremiumFixture {

    public static final String   PRODUCT_CODE    = "LIFE-WL-01";
    public static final int      DEFAULT_AGE     = 35;
    public static final String   GENDER_MALE     = "M";
    public static final String   GENDER_FEMALE   = "F";
    public static final int      DEFAULT_AMOUNT  = 1000;   // 萬元
    public static final int      DEFAULT_PERIOD  = 20;
    public static final BigDecimal MALE_RATE     = new BigDecimal("12.50");
    public static final BigDecimal FEMALE_RATE   = new BigDecimal("11.80");
    public static final int      ANNUAL_MALE     = 125000;
    public static final int      MONTHLY_MALE    = 10729;
    public static final UUID     AGENT_ID        = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // ── Request ──

    public static PremiumCalculateRequest aValidRequest() {
        return new PremiumCalculateRequest(
                PRODUCT_CODE, DEFAULT_AGE, GENDER_MALE, DEFAULT_AMOUNT, DEFAULT_PERIOD);
    }

    public static PremiumCalculateRequest aRequestWithAge(int age) {
        return new PremiumCalculateRequest(
                PRODUCT_CODE, age, GENDER_MALE, DEFAULT_AMOUNT, DEFAULT_PERIOD);
    }

    public static PremiumCalculateRequest aRequestWithAmount(int amount) {
        return new PremiumCalculateRequest(
                PRODUCT_CODE, DEFAULT_AGE, GENDER_MALE, amount, DEFAULT_PERIOD);
    }

    public static PremiumCalculateRequest aRequestWithPeriod(int period) {
        return new PremiumCalculateRequest(
                PRODUCT_CODE, DEFAULT_AGE, GENDER_MALE, DEFAULT_AMOUNT, period);
    }

    public static PremiumCalculateRequest aRequestWithProductCode(String productCode) {
        return new PremiumCalculateRequest(
                productCode, DEFAULT_AGE, GENDER_MALE, DEFAULT_AMOUNT, DEFAULT_PERIOD);
    }

    // ── Response ──

    public static PremiumCalculateResponse aSuccessResponse() {
        return new PremiumCalculateResponse(
                UUID.randomUUID(), PRODUCT_CODE, DEFAULT_AMOUNT,
                DEFAULT_PERIOD, MALE_RATE, ANNUAL_MALE, MONTHLY_MALE);
    }

    // ── Domain ──

    public static Product aProduct() {
        return Product.create(PRODUCT_CODE, "終身壽險 WL-01");
    }

    public static RateEntry aRateEntry(RateTableVersion version) {
        return RateEntry.create(version, DEFAULT_AGE, GENDER_MALE, DEFAULT_PERIOD, MALE_RATE);
    }
}
