package com.example.lifepremium.repository;

import com.example.lifepremium.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);
}
