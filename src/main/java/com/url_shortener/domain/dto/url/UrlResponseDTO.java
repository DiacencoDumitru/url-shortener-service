package com.url_shortener.domain.dto.url;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UrlResponse")
public record UrlResponseDTO(
        @Schema(description = "Original long URL") String url,
        @Schema(description = "Full short URL for redirects") String shortenUrl) {

}
