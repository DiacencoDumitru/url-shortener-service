package com.url_shortener.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url_shortener.domain.dto.url.UrlResponseDTO;
import com.url_shortener.exceptions.IdempotencyKeyConflictException;
import com.url_shortener.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    public static final String KEY_PREFIX = "idempotency:shorten:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<UrlResponseDTO> lookup(String idempotencyKey, String requestUrl) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        if (json == null) {
            return Optional.empty();
        }
        try {
            UrlResponseDTO stored = objectMapper.readValue(json, UrlResponseDTO.class);
            if (!stored.url().equals(requestUrl)) {
                throw new IdempotencyKeyConflictException(
                        "Idempotency-Key was already used with a different request body");
            }
            return Optional.of(stored);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Override
    public void record(String idempotencyKey, UrlResponseDTO response, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue()
                    .set(KEY_PREFIX + idempotencyKey, json, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotency payload", e);
        }
    }
}
