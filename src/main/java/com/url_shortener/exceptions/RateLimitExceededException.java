package com.url_shortener.exceptions;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String clientId) {
        super("Rate limit exceeded for client " + clientId);
    }
}
