package com.diacencodumitru.url_shortener.exceptions;

import lombok.Getter;

@Getter
public class UrlNotFoundException extends RuntimeException {
    private final String urlId;

    public UrlNotFoundException(String urlId) {
        super(String.format("URL %s not found ", urlId));
        this.urlId = urlId;
    }
}
