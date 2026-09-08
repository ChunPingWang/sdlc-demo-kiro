package com.example.lifepremium.repository;

import com.example.lifepremium.domain.CalculationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface CalculationRecordRepository extends JpaRepository<CalculationRecord, UUID> {

    /** 業務員查詢自己的試算紀錄（含日期篩選）。 */
    @Query("""
        SELECT r FROM CalculationRecord r
        WHERE r.agentId = :agentId
          AND r.createdAt BETWEEN :from AND :to
        ORDER BY r.createdAt DESC
        """)
    Page<CalculationRecord> findByAgentIdAndDateRange(
            @Param("agentId") UUID agentId,
            @Param("from")    Instant from,
            @Param("to")      Instant to,
            Pageable pageable
    );

    /** 管理員查詢全量試算紀錄（含日期篩選）。 */
    @Query("""
        SELECT r FROM CalculationRecord r
        WHERE r.createdAt BETWEEN :from AND :to
        ORDER BY r.createdAt DESC
        """)
    Page<CalculationRecord> findAllByDateRange(
            @Param("from") Instant from,
            @Param("to")   Instant to,
            Pageable pageable
    );

    long countByAgentId(UUID agentId);
}
