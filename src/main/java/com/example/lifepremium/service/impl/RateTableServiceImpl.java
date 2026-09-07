package com.example.lifepremium.service.impl;

import com.example.lifepremium.domain.Product;
import com.example.lifepremium.domain.RateEntry;
import com.example.lifepremium.domain.RateTableVersion;
import com.example.lifepremium.dto.request.RateTableUploadRequest;
import com.example.lifepremium.dto.response.RateTableUploadResponse;
import com.example.lifepremium.exception.InvalidCsvFormatException;
import com.example.lifepremium.exception.InvalidPaymentPeriodException;
import com.example.lifepremium.exception.RateVersionConflictException;
import com.example.lifepremium.repository.ProductRepository;
import com.example.lifepremium.repository.RateEntryRepository;
import com.example.lifepremium.repository.RateTableVersionRepository;
import com.example.lifepremium.service.RateTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateTableServiceImpl implements RateTableService {

    private static final int BATCH_SIZE = 100;
    private static final Set<Integer> VALID_PERIODS = Set.of(10, 20, 30, 99);

    private final ProductRepository productRepository;
    private final RateTableVersionRepository versionRepository;
    private final RateEntryRepository entryRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public RateTableUploadResponse uploadRateTable(
            RateTableUploadRequest request, MultipartFile file, UUID adminId) {

        // 1. effectiveDate 須 >= 今日
        if (request.effectiveDate().isBefore(LocalDate.now())) {
            throw new com.example.lifepremium.exception.BusinessException(
                com.example.lifepremium.exception.ErrorCode.INVALID_EFFECTIVE_DATE,
                "費率生效日期不得早於今日：" + request.effectiveDate()
            ) {};
        }

        // 2. 衝突檢查
        if (versionRepository.existsByProductCodeAndEffectiveDate(
                request.productCode(), request.effectiveDate())) {
            throw new RateVersionConflictException(request.productCode(), request.effectiveDate());
        }

        // 3. 取得或建立商品
        Product product = productRepository.findByProductCode(request.productCode())
            .orElseGet(() -> productRepository.save(
                Product.create(request.productCode(), request.productCode())
            ));

        // 4. 解析 CSV
        List<RateEntry> entries = parseCsv(file, null); // version 於建立後設定

        // 5. 計算版本號
        int versionNumber = versionRepository.countByProductCode(request.productCode()) + 1;

        // 6. 建立版本紀錄（先以 PENDING 狀態）
        String filePath = String.format("s3://rate-tables/%s/v%d.csv",
                                        request.productCode(), versionNumber);
        RateTableVersion version = RateTableVersion.create(
            product, versionNumber, request.effectiveDate(), filePath, adminId
        );
        version = versionRepository.save(version);

        // 7. 批次 INSERT 費率明細
        final RateTableVersion savedVersion = version;
        List<RateEntry> toSave = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            CSVRecord placeholder = null; // entries 已解析完畢，重建 RateEntry 並綁定 version
        }
        // 重新解析並綁定版本（CSV 已驗證，直接重建 Entity）
        List<RateEntry> boundEntries = parseCsvWithVersion(file, savedVersion);
        int total = 0;
        List<RateEntry> batch = new ArrayList<>();
        for (RateEntry e : boundEntries) {
            batch.add(e);
            if (batch.size() == BATCH_SIZE) {
                entryRepository.saveAll(batch);
                total += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            entryRepository.saveAll(batch);
            total += batch.size();
        }

        // 8. 版本轉為 ACTIVE
        version.activate(total);

        // 9. 清除該商品費率快取
        evictProductRateCache(request.productCode());

        log.info("費率表上傳完成: productCode={}, versionId={}, entryCount={}", 
                 request.productCode(), version.getId(), total);

        return new RateTableUploadResponse(
            version.getId(),
            request.productCode(),
            request.effectiveDate(),
            total,
            filePath
        );
    }

    /** 解析 CSV 並驗證格式（不綁定 version） */
    private List<RateEntry> parseCsv(MultipartFile file, RateTableVersion version) {
        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader("age", "gender", "payment_period", "rate")
                .setSkipHeaderRecord(true)
                .build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<RateEntry> result = new ArrayList<>();
            for (CSVRecord record : parser) {
                validateCsvRecord(record);
                if (version != null) {
                    result.add(RateEntry.create(
                        version,
                        Integer.parseInt(record.get("age").trim()),
                        record.get("gender").trim(),
                        Integer.parseInt(record.get("payment_period").trim()),
                        new BigDecimal(record.get("rate").trim())
                    ));
                }
            }
            return result;
        } catch (InvalidCsvFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCsvFormatException("CSV 讀取失敗：" + e.getMessage());
        }
    }

    /** 解析 CSV 並綁定 version（正式建立 Entity） */
    private List<RateEntry> parseCsvWithVersion(MultipartFile file, RateTableVersion version) {
        return parseCsv(file, version);
    }

    private void validateCsvRecord(CSVRecord record) {
        try {
            int age = Integer.parseInt(record.get("age").trim());
            String gender = record.get("gender").trim();
            int period = Integer.parseInt(record.get("payment_period").trim());
            BigDecimal rate = new BigDecimal(record.get("rate").trim());

            if (age < 0 || age > 120) {
                throw new InvalidCsvFormatException("年齡超出範圍（0-120）：" + age);
            }
            if (!gender.equals("M") && !gender.equals("F")) {
                throw new InvalidCsvFormatException("性別只允許 M 或 F：" + gender);
            }
            if (!VALID_PERIODS.contains(period)) {
                throw new InvalidCsvFormatException("繳費年期只允許 10/20/30/99：" + period);
            }
            if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidCsvFormatException("費率須大於 0：" + rate);
            }
        } catch (NumberFormatException e) {
            throw new InvalidCsvFormatException("數值格式錯誤（行 " + record.getRecordNumber() + "）：" + e.getMessage());
        }
    }

    private void evictProductRateCache(String productCode) {
        String pattern = String.format("rate:%s:*", productCode);
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("清除費率快取：pattern={}, count={}", pattern, keys.size());
        }
    }
}
