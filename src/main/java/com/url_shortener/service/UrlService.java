package com.url_shortener.service;

import com.url_shortener.domain.dto.stats.UrlClickStatsDTO;
import com.url_shortener.domain.dto.url.UrlRequestDTO;
import com.url_shortener.domain.dto.url.UrlResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public interface UrlService {

    UrlResponseDTO shortenUrl(UrlRequestDTO data, HttpServletRequest request, String idempotencyKey);
    HttpHeaders redirect(String id);

    UrlClickStatsDTO getClickStats(String id);
}
