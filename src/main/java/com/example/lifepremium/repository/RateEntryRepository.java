package com.example.lifepremium.repository;

import com.example.lifepremium.domain.RateEntry;
import com.example.lifepremium.domain.RateTableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateEntryRepository extends JpaRepository<RateEntry, UUID> {

    /**
     * 查詢最新生效費率（Cache Miss 時使用）。
     * 依 effective_date DESC 取第一筆 ACTIVE 版本對應費率。
     */
    @Query("""
        SELECT r.rate FROM RateEntry r
        JOIN r.rateTableVersion v
        WHERE v.productCode  = :productCode
          AND r.age          = :age
          AND r.gender       = :gender
          AND r.paymentPeriod= :paymentPeriod
          AND v.status       = com.example.lifepremium.domain.RateTableVersion$VersionStatus.ACTIVE
          AND v.effectiveDate <= CURRENT_DATE
        ORDER BY v.effectiveDate DESC
        LIMIT 1
        """)
    Optional<BigDecimal> findEffectiveRate(
            @Param("productCode")   String productCode,
            @Param("age")           int age,
            @Param("gender")        String gender,
            @Param("paymentPeriod") int paymentPeriod
    );

    void deleteByRateTableVersion(RateTableVersion version);
}
