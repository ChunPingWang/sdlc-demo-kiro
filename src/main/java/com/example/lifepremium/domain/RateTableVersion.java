package com.example.lifepremium.domain;

import com.example.lifepremium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 費率表版本
 * Table: rate_table_versions
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "rate_table_versions",
    indexes = {
        @Index(name = "idx_rate_versions_product_code", columnList = "product_code"),
        @Index(name = "uq_rate_versions_product_effective",
               columnList = "product_code, effective_date", unique = true)
    }
)
public class RateTableVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VersionStatus status;

    @Column(name = "entry_count", nullable = false)
    private Integer entryCount;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @OneToMany(mappedBy = "rateTableVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateEntry> rateEntries = new ArrayList<>();

    public static RateTableVersion create(
            Product product, int versionNumber, LocalDate effectiveDate,
            String filePath, UUID uploadedBy) {
        RateTableVersion v = new RateTableVersion();
        v.product = product;
        v.productCode = product.getProductCode();
        v.versionNumber = versionNumber;
        v.effectiveDate = effectiveDate;
        v.status = VersionStatus.PENDING;
        v.entryCount = 0;
        v.filePath = filePath;
        v.uploadedBy = uploadedBy;
        return v;
    }

    public void activate(int entryCount) {
        this.status = VersionStatus.ACTIVE;
        this.entryCount = entryCount;
    }

    public enum VersionStatus {
        PENDING, ACTIVE, SUPERSEDED
    }
}
