package com.example.lifepremium.repository;

import com.example.lifepremium.domain.RateTableVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateTableVersionRepository extends JpaRepository<RateTableVersion, UUID> {

    boolean existsByProductCodeAndEffectiveDate(String productCode, LocalDate effectiveDate);

    Optional<RateTableVersion> findTopByProductCodeAndStatusOrderByEffectiveDateDesc(
            String productCode, RateTableVersion.VersionStatus status);

    Page<RateTableVersion> findByProductCodeOrderByEffectiveDateDesc(
            String productCode, Pageable pageable);

    int countByProductCode(String productCode);
}
