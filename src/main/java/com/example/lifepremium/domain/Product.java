package com.example.lifepremium.domain;

import com.example.lifepremium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 保險商品
 * Table: products
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "products",
    indexes = @Index(name = "uq_products_product_code", columnList = "product_code", unique = true)
)
public class Product extends BaseEntity {

    @Column(name = "product_code", nullable = false, length = 20, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    public static Product create(String productCode, String productName) {
        Product p = new Product();
        p.productCode = productCode;
        p.productName = productName;
        p.status = ProductStatus.ACTIVE;
        return p;
    }

    public enum ProductStatus {
        ACTIVE, INACTIVE
    }
}
