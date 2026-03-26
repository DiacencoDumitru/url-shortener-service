package com.diacencodumitru.url_shortener.service.impl;

import com.diacencodumitru.url_shortener.exceptions.RateLimitExceededException;
import com.diacencodumitru.url_shortener.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RedisTokenBucketRateLimiter implements RateLimiterService {

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            """
                    local key = KEYS[1]
                    local now = tonumber(ARGV[1])
                    local capacity = tonumber(ARGV[2])
                    local refillRate = tonumber(ARGV[3])
                    local ttlMillis = tonumber(ARGV[4])
                    local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill')
                    local tokens = tonumber(bucket[1])
                    local lastRefill = tonumber(bucket[2])
                    if tokens == nil or lastRefill == nil then
                      tokens = capacity
                      lastRefill = now
                    end
                    local elapsed = now - lastRefill
                    if elapsed > 0 then
                      tokens = math.min(capacity, tokens + elapsed * refillRate)
                      lastRefill = now
                    end
                    if tokens < 1 then
                      redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
                      redis.call('PEXPIRE', key, ttlMillis)
                      return 0
                    end
                    tokens = tokens - 1
                    redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
                    redis.call('PEXPIRE', key, ttlMillis)
                    return 1
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Value("${url-shortener.rate-limit.capacity}")
    private long capacity;

    @Value("${url-shortener.rate-limit.refill-tokens}")
    private long refillTokens;

    @Value("${url-shortener.rate-limit.refill-duration-seconds}")
    private long refillDurationSeconds;

    @Override
    public void consumeTokenOrThrow(String clientId) {
        long nowMillis = Instant.now().toEpochMilli();
        double refillRatePerMillis = (double) refillTokens / Duration.ofSeconds(refillDurationSeconds).toMillis();
        long ttlMillis = Duration.ofSeconds(refillDurationSeconds).multipliedBy(2).toMillis();
        String key = "rate-limit:shorten-url:" + clientId;

        Long allowed = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                Collections.singletonList(key),
                Long.toString(nowMillis),
                Long.toString(capacity),
                Double.toString(refillRatePerMillis),
                Long.toString(ttlMillis)
        );

        if (allowed == null || allowed == 0L) {
            throw new RateLimitExceededException(clientId);
        }
    }
}
