package com.url_shortener.domain.dto.url;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UrlRequest")
public record UrlRequestDTO(
        @Schema(description = "Original long URL", example = "https://example.com/page") String url) {
}
