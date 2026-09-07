package com.example.lifepremium.domain;

import com.example.lifepremium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 費率明細
 * Table: rate_entries
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "rate_entries",
    indexes = @Index(
        name = "idx_rate_entries_lookup",
        columnList = "version_id, age, gender, payment_period"
    )
)
public class RateEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private RateTableVersion rateTableVersion;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Column(name = "payment_period", nullable = false)
    private Integer paymentPeriod;

    @Column(name = "rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    public static RateEntry create(
            RateTableVersion version, int age, String gender,
            int paymentPeriod, BigDecimal rate) {
        RateEntry e = new RateEntry();
        e.rateTableVersion = version;
        e.age = age;
        e.gender = gender;
        e.paymentPeriod = paymentPeriod;
        e.rate = rate;
        return e;
    }
}
