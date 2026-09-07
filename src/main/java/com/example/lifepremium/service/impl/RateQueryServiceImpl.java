package com.example.lifepremium.service.impl;

import com.example.lifepremium.exception.RateNotFoundException;
import com.example.lifepremium.repository.RateEntryRepository;
import com.example.lifepremium.service.RateQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateQueryServiceImpl implements RateQueryService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX  = "rate:";

    private final RateEntryRepository   rateEntryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public BigDecimal findRate(String productCode, int age, String gender, int paymentPeriod) {
        String cacheKey = buildKey(productCode, age, gender, paymentPeriod);

        // 1. 查 Redis
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Rate cache hit: key={}", cacheKey);
            return new BigDecimal(cached.toString());
        }

        log.debug("Rate cache miss: key={}", cacheKey);

        // 2. 查 DB
        BigDecimal rate = rateEntryRepository
            .findEffectiveRate(productCode, age, gender, paymentPeriod, LocalDate.now())
            .orElseThrow(() -> new RateNotFoundException(productCode, age, gender, paymentPeriod));

        // 3. 回寫 Redis
        redisTemplate.opsForValue().set(cacheKey, rate.toPlainString(), CACHE_TTL);
        return rate;
    }

    @Override
    public void evictByProductCode(String productCode) {
        String pattern = KEY_PREFIX + productCode + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} rate cache keys for product: {}", keys.size(), productCode);
        }
    }

    private String buildKey(String productCode, int age, String gender, int paymentPeriod) {
        return KEY_PREFIX + productCode + ":" + age + ":" + gender + ":" + paymentPeriod;
    }
}
