package com.diacencodumitru.url_shortener.service;

public interface RateLimiterService {

    void consumeTokenOrThrow(String clientId);
}
