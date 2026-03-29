package com.url_shortener.service.impl;

import com.url_shortener.domain.dto.stats.UrlClickStatsDTO;
import com.url_shortener.domain.dto.url.UrlRequestDTO;
import com.url_shortener.domain.dto.url.UrlResponseDTO;
import com.url_shortener.entities.UrlEntity;
import com.url_shortener.exceptions.InvalidIdempotencyKeyException;
import com.url_shortener.exceptions.UrlNotFoundException;
import com.url_shortener.repository.UrlRepository;
import com.url_shortener.service.ClickCounterService;
import com.url_shortener.service.IdempotencyService;
import com.url_shortener.service.RateLimiterService;
import com.url_shortener.service.RedirectUrlCacheService;
import com.url_shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final RateLimiterService rateLimiterService;
    private final RedirectUrlCacheService redirectUrlCacheService;
    private final ClickCounterService clickCounterService;
    private final IdempotencyService idempotencyService;

    @Value("${url-shortener.expiration-minutes:1}")
    private long expirationMinutes;

    @Value("${url-shortener.idempotency.max-key-length:255}")
    private int idempotencyMaxKeyLength;

    @Override
    public UrlResponseDTO shortenUrl(UrlRequestDTO data, HttpServletRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey != null) {
            Optional<UrlResponseDTO> replay = idempotencyService.lookup(normalizedKey, data.url());
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        String clientId = extractClientId(request);
        rateLimiterService.consumeTokenOrThrow(clientId);

        String id;

        do {
            id = RandomStringUtils.randomAlphanumeric(5, 10);
        } while (urlRepository.existsById(id));

        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        urlRepository.save(new UrlEntity(id, data.url(), expiredAt));
        long ttlSeconds = Math.max(1L, Duration.between(LocalDateTime.now(), expiredAt).getSeconds());
        redirectUrlCacheService.put(id, data.url(), ttlSeconds);
        clickCounterService.initCounter(id, ttlSeconds);

        String redirectUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/" + id)
                .replaceQuery(null)
                .build()
                .toUriString();

        UrlResponseDTO response = new UrlResponseDTO(data.url(), redirectUrl);
        if (normalizedKey != null) {
            idempotencyService.record(normalizedKey, response, ttlSeconds);
        }
        return response;
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > idempotencyMaxKeyLength) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key exceeds maximum length of " + idempotencyMaxKeyLength);
        }
        return trimmed;
    }

    private String extractClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public HttpHeaders redirect(String id) {
        return redirectUrlCacheService.get(id)
                .map(url -> {
                    clickCounterService.recordClick(id);
                    return redirectHeaders(url);
                })
                .orElseGet(() -> {
                    UrlEntity entity = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException(id));
                    redirectUrlCacheService.put(id, entity.getUrl(), ttlSecondsUntilExpiry(entity.getExpiredAt()));
                    clickCounterService.recordClick(id);
                    return redirectHeaders(entity.getUrl());
                });
    }

    @Override
    public UrlClickStatsDTO getClickStats(String id) {
        if (!urlRepository.existsById(id)) {
            throw new UrlNotFoundException(id);
        }
        long clicks = clickCounterService.getCount(id).orElse(0L);
        return new UrlClickStatsDTO(id, clicks);
    }

    private HttpHeaders redirectHeaders(String targetUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        return headers;
    }

    private long ttlSecondsUntilExpiry(LocalDateTime expiredAt) {
        long seconds = Duration.between(LocalDateTime.now(), expiredAt).getSeconds();
        return Math.max(1L, seconds);
    }
}
