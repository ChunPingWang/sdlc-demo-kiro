package com.example.lifepremium.repository;

import com.example.lifepremium.domain.RateEntry;
import com.example.lifepremium.domain.RateTableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateEntryRepository extends JpaRepository<RateEntry, UUID> {

    /**
     * 查詢最新生效的費率（對應 SD 文件 5.1 節 JPQL 設計）
     * 取 effectiveDate <= today 且 status=ACTIVE 的最新版本費率
     */
    @Query("""
        SELECT r.rate FROM RateEntry r
        JOIN r.rateTableVersion v
        WHERE v.productCode  = :productCode
          AND r.age           = :age
          AND r.gender        = :gender
          AND r.paymentPeriod = :paymentPeriod
          AND v.effectiveDate <= :today
          AND v.status        = 'ACTIVE'
        ORDER BY v.effectiveDate DESC
        LIMIT 1
        """)
    Optional<BigDecimal> findEffectiveRate(
            @Param("productCode")   String productCode,
            @Param("age")           int    age,
            @Param("gender")        String gender,
            @Param("paymentPeriod") int    paymentPeriod,
            @Param("today")         LocalDate today);

    List<RateEntry> findByRateTableVersion(RateTableVersion version);
}
