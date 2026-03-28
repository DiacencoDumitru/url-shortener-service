package com.url_shortener.controller;

import com.url_shortener.domain.dto.stats.UrlClickStatsDTO;
import com.url_shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics")
@RestController
@AllArgsConstructor
public class StatsController {

    private final UrlService urlService;

    @Operation(summary = "Get click statistics for a short ID", responses = {
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "404", description = "Short ID not found")
    })
    @GetMapping("/stats/{id}")
    public ResponseEntity<UrlClickStatsDTO> getClickStats(
            @Parameter(description = "Short identifier") @PathVariable("id") String id) {
        return ResponseEntity.ok(urlService.getClickStats(id));
    }
}
