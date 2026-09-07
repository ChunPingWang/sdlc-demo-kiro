package com.example.lifepremium.repository;

import com.example.lifepremium.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RateEntryRepository Tests")
class RateEntryRepositoryTest {

    @Autowired private TestEntityManager    em;
    @Autowired private RateEntryRepository  rateEntryRepository;
    @Autowired private ProductRepository    productRepository;
    @Autowired private RateTableVersionRepository versionRepository;

    private RateTableVersion activeVersion;

    @BeforeEach
    void setUp() {
        Product product = productRepository.save(
                Product.create("LIFE-WL-01", "終身壽險 WL-01"));

        activeVersion = RateTableVersion.create(
                product, 1, LocalDate.of(2026, 1, 1),
                "s3://test/v1.csv",
                java.util.UUID.randomUUID());
        activeVersion.activate(7);
        versionRepository.save(activeVersion);

        // 建立費率明細
        rateEntryRepository.save(RateEntry.create(activeVersion, 35, "M", 20,
                new BigDecimal("12.50")));
        rateEntryRepository.save(RateEntry.create(activeVersion, 35, "F", 20,
                new BigDecimal("11.80")));
        rateEntryRepository.save(RateEntry.create(activeVersion, 0,  "M", 20,
                new BigDecimal("5.20")));
        rateEntryRepository.save(RateEntry.create(activeVersion, 70, "M", 20,
                new BigDecimal("98.50")));

        em.flush();
        em.clear();
    }

    @Test @DisplayName("findEffectiveRate - 男性 35 歲 20 年期 → 12.50")
    void findEffectiveRate_male35y20p_success() {
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-WL-01", 35, "M", 20, LocalDate.now());

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo("12.50");
    }

    @Test @DisplayName("findEffectiveRate - 女性費率不同 → 11.80")
    void findEffectiveRate_female_differentRate() {
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-WL-01", 35, "F", 20, LocalDate.now());

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo("11.80");
    }

    @Test @DisplayName("findEffectiveRate - 年齡下限 0 歲 → 5.20")
    void findEffectiveRate_age0_success() {
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-WL-01", 0, "M", 20, LocalDate.now());

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo("5.20");
    }

    @Test @DisplayName("findEffectiveRate - 年齡上限 70 歲 → 98.50")
    void findEffectiveRate_age70_success() {
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-WL-01", 70, "M", 20, LocalDate.now());

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo("98.50");
    }

    @Test @DisplayName("findEffectiveRate - 商品不存在 → empty")
    void findEffectiveRate_unknownProduct_empty() {
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-XX-99", 35, "M", 20, LocalDate.now());

        assertThat(rate).isEmpty();
    }

    @Test @DisplayName("findEffectiveRate - 未來生效日版本不應被查到")
    void findEffectiveRate_futureVersion_empty() {
        // 建立一個未來才生效的版本
        Product product = productRepository.findByProductCode("LIFE-WL-01").orElseThrow();
        RateTableVersion futureVersion = RateTableVersion.create(
                product, 2, LocalDate.now().plusDays(10),
                "s3://test/v2.csv", java.util.UUID.randomUUID());
        futureVersion.activate(1);
        versionRepository.save(futureVersion);
        rateEntryRepository.save(RateEntry.create(futureVersion, 35, "M", 20,
                new BigDecimal("99.99")));
        em.flush(); em.clear();

        // 今日查詢應取到舊版本費率（12.50），而非未來版本（99.99）
        Optional<BigDecimal> rate = rateEntryRepository.findEffectiveRate(
                "LIFE-WL-01", 35, "M", 20, LocalDate.now());

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo("12.50");
    }
}
