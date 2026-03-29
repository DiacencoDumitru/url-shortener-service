package com.url_shortener.service;

import com.url_shortener.domain.dto.url.UrlResponseDTO;

import java.util.Optional;

public interface IdempotencyService {

    Optional<UrlResponseDTO> lookup(String idempotencyKey, String requestUrl);

    void record(String idempotencyKey, UrlResponseDTO response, long ttlSeconds);
}
