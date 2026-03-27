package com.url_shortener.service.impl;

import com.url_shortener.service.ClickCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisClickCounterService implements ClickCounterService {

    private static final String KEY_PREFIX = "stats:url:clicks:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void initCounter(String id, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + id, "0", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void recordClick(String id) {
        redisTemplate.opsForValue().increment(KEY_PREFIX + id);
    }

    @Override
    public Optional<Long> getCount(String id) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }
}
