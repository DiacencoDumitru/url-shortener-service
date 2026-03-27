package com.url_shortener.service;

public interface RateLimiterService {

    void consumeTokenOrThrow(String clientId);
}
