package com.example.lifepremium.service.impl;

import com.example.lifepremium.domain.Product;
import com.example.lifepremium.domain.RateEntry;
import com.example.lifepremium.domain.RateTableVersion;
import com.example.lifepremium.dto.response.PageResponse;
import com.example.lifepremium.dto.response.RateTableVersionResponse;
import com.example.lifepremium.exception.*;
import com.example.lifepremium.repository.ProductRepository;
import com.example.lifepremium.repository.RateEntryRepository;
import com.example.lifepremium.repository.RateTableVersionRepository;
import com.example.lifepremium.service.RateTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateTableServiceImpl implements RateTableService {

    private static final int BATCH_SIZE = 100;

    private final ProductRepository          productRepository;
    private final RateTableVersionRepository versionRepository;
    private final RateEntryRepository        entryRepository;
    private final CacheManager               cacheManager;

    @Override
    @Transactional
    public RateTableVersionResponse upload(
            MultipartFile file, String productCode,
            LocalDate effectiveDate, UUID adminId) {

        // 生效日不可早於今天
        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_EFFECTIVE_DATE,
                    "生效日期不可早於今日") {};
        }

        // 衝突檢查
        if (versionRepository.existsByProductCodeAndEffectiveDate(productCode, effectiveDate)) {
            throw new RateVersionConflictException(productCode, effectiveDate);
        }

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        // 版本序號
        int versionNumber = versionRepository.countByProductCode(productCode) + 1;
        String filePath   = String.format("s3://rate-tables/%s/v%d.csv", productCode, versionNumber);

        // 建立版本（PENDING）
        RateTableVersion version = RateTableVersion.create(
                product, versionNumber, effectiveDate, filePath, adminId);
        versionRepository.save(version);

        // 解析 CSV
        List<RateEntry> entries = parseCsv(file, version);

        // 批次 INSERT（每批 BATCH_SIZE 筆）
        for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
            entryRepository.saveAll(
                    entries.subList(i, Math.min(i + BATCH_SIZE, entries.size())));
        }

        // 版本狀態更新為 ACTIVE
        version.activate(entries.size());

        // 清除費率快取
        evictRateCache(productCode);

        log.info("費率表上傳完成: productCode={}, versionId={}, entryCount={}",
                productCode, version.getId(), entries.size());

        return toVersionResponse(version);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RateTableVersionResponse> listVersions(String productCode, Pageable pageable) {
        return PageResponse.of(
                versionRepository
                        .findByProductCodeOrderByEffectiveDateDesc(productCode, pageable)
                        .map(this::toVersionResponse)
        );
    }

    // ── Private Helpers ──

    private List<RateEntry> parseCsv(MultipartFile file, RateTableVersion version) {
        List<RateEntry> entries = new ArrayList<>();
        int lineNumber = 1;
        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader("age", "gender", "payment_period", "rate")
                .setSkipHeaderRecord(true)
                .build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            for (CSVRecord record : parser) {
                lineNumber++;
                try {
                    int        age     = Integer.parseInt(record.get("age").trim());
                    String     gender  = record.get("gender").trim().toUpperCase();
                    int        period  = Integer.parseInt(record.get("payment_period").trim());
                    BigDecimal rate    = new BigDecimal(record.get("rate").trim());

                    if (!gender.matches("M|F"))
                        throw new IllegalArgumentException("gender 僅接受 M 或 F");
                    if (rate.compareTo(BigDecimal.ZERO) <= 0)
                        throw new IllegalArgumentException("rate 須大於 0");

                    entries.add(RateEntry.create(version, age, gender, period, rate));
                } catch (NumberFormatException | IllegalArgumentException e) {
                    throw new InvalidCsvFormatException(lineNumber, e.getMessage());
                }
            }
        } catch (InvalidCsvFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCsvFormatException(lineNumber, e.getMessage());
        }
        return entries;
    }

    private void evictRateCache(String productCode) {
        var cache = cacheManager.getCache("rates");
        if (cache != null) {
            cache.clear();   // 清除全部費率快取（生產環境可改用 pattern evict）
            log.debug("費率快取已清除: productCode={}", productCode);
        }
    }

    private RateTableVersionResponse toVersionResponse(RateTableVersion v) {
        return new RateTableVersionResponse(
                v.getId(), v.getProductCode(), v.getVersionNumber(),
                v.getEffectiveDate(), v.getStatus().name(),
                v.getEntryCount(), v.getFilePath());
    }
}
