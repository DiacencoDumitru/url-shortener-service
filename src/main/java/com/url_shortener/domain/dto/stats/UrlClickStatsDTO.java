package com.url_shortener.domain.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UrlClickStats")
public record UrlClickStatsDTO(
        @Schema(description = "Short link identifier") String id,
        @Schema(description = "Number of successful redirects") long clicks) {
}
