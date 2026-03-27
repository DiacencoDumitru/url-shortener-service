package com.url_shortener.controller;

import com.url_shortener.domain.dto.stats.UrlClickStatsDTO;
import com.url_shortener.service.UrlService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StatsController {

    private final UrlService urlService;

    @GetMapping("/stats/{id}")
    public ResponseEntity<UrlClickStatsDTO> getClickStats(@PathVariable("id") String id) {
        return ResponseEntity.ok(urlService.getClickStats(id));
    }
}
