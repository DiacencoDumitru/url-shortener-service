package com.url_shortener.controller;

import com.url_shortener.domain.dto.url.UrlRequestDTO;
import com.url_shortener.domain.dto.url.UrlResponseDTO;
import com.url_shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "URLs")
@RestController
@AllArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @Operation(summary = "Create a short URL", responses = {
            @ApiResponse(responseCode = "200", description = "Short URL created"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/shorten-url")
    public ResponseEntity<UrlResponseDTO> shortenUrl(@RequestBody UrlRequestDTO data, HttpServletRequest request) {
        return ResponseEntity.ok(urlService.shortenUrl(data, request));
    }

    @Operation(summary = "Redirect to the original URL", responses = {
            @ApiResponse(responseCode = "302", description = "Redirect to target"),
            @ApiResponse(responseCode = "404", description = "Short ID not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Void> redirect(@Parameter(description = "Short identifier") @PathVariable("id") String id) {
        HttpHeaders headers = urlService.redirect(id);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
