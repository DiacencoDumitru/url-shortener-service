package com.url_shortener.service;

import java.util.Optional;

public interface RedirectUrlCacheService {

    Optional<String> get(String id);

    void put(String id, String url, long ttlSeconds);
}
