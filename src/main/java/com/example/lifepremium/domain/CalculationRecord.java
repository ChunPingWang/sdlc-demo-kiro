package com.example.lifepremium.domain;

import com.example.lifepremium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 試算紀錄 Entity。
 * 對應資料表：calculation_records
 * 業務員試算必記錄；訪客（agentId=null）不記錄。
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "calculation_records",
    indexes = {
        @Index(name = "idx_calc_records_agent_id",
               columnList = "agent_id, created_at"),
        @Index(name = "idx_calc_records_created_at",
               columnList = "created_at")
    }
)
public class CalculationRecord extends BaseEntity {

    /** 業務員 ID，訪客為 NULL */
    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(name = "insured_age", nullable = false)
    private Integer insuredAge;

    @Column(name = "insured_gender", nullable = false, length = 1)
    private String insuredGender;

    /** 保額（萬元） */
    @Column(name = "insured_amount", nullable = false)
    private Integer insuredAmount;

    @Column(name = "payment_period", nullable = false)
    private Integer paymentPeriod;

    /** 套用費率，失敗時為 NULL */
    @Column(name = "rate_used", precision = 10, scale = 4)
    private BigDecimal rateUsed;

    /** 年繳保費（元），失敗時為 NULL */
    @Column(name = "annual_premium")
    private Integer annualPremium;

    /** 月繳保費（元），失敗時為 NULL */
    @Column(name = "monthly_premium")
    private Integer monthlyPremium;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalculationStatus status;

    /** 失敗原因代碼，如 AGE_OUT_OF_RANGE */
    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    /** 成功試算建立 */
    public static CalculationRecord success(UUID agentId, String productCode,
                                            int insuredAge, String insuredGender,
                                            int insuredAmount, int paymentPeriod,
                                            BigDecimal rateUsed,
                                            int annualPremium, int monthlyPremium) {
        CalculationRecord r = new CalculationRecord();
        r.agentId = agentId;
        r.productCode = productCode;
        r.insuredAge = insuredAge;
        r.insuredGender = insuredGender;
        r.insuredAmount = insuredAmount;
        r.paymentPeriod = paymentPeriod;
        r.rateUsed = rateUsed;
        r.annualPremium = annualPremium;
        r.monthlyPremium = monthlyPremium;
        r.status = CalculationStatus.SUCCESS;
        return r;
    }

    /** 失敗試算建立 */
    public static CalculationRecord failure(UUID agentId, String productCode,
                                            int insuredAge, String insuredGender,
                                            int insuredAmount, int paymentPeriod,
                                            String failureReason) {
        CalculationRecord r = new CalculationRecord();
        r.agentId = agentId;
        r.productCode = productCode;
        r.insuredAge = insuredAge;
        r.insuredGender = insuredGender;
        r.insuredAmount = insuredAmount;
        r.paymentPeriod = paymentPeriod;
        r.status = CalculationStatus.FAILED;
        r.failureReason = failureReason;
        return r;
    }

    public enum CalculationStatus {
        SUCCESS, FAILED
    }
}
