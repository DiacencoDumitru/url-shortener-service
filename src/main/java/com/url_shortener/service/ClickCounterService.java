package com.url_shortener.service;

import java.util.Optional;

public interface ClickCounterService {

    void initCounter(String id, long ttlSeconds);

    void recordClick(String id);

    Optional<Long> getCount(String id);
}
