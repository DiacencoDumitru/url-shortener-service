package com.url_shortener.service.impl;

import com.url_shortener.service.RedirectUrlCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisRedirectUrlCacheService implements RedirectUrlCacheService {

    private static final String KEY_PREFIX = "cache:url:redirect:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> get(String id) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String id, String url, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + id, url, Duration.ofSeconds(ttlSeconds));
    }
}
