package com.diacencodumitru.url_shortener.service.impl;

import com.diacencodumitru.url_shortener.domain.dto.url.UrlRequestDTO;
import com.diacencodumitru.url_shortener.domain.dto.url.UrlResponseDTO;
import com.diacencodumitru.url_shortener.entities.UrlEntity;
import com.diacencodumitru.url_shortener.exceptions.UrlNotFoundException;
import com.diacencodumitru.url_shortener.repository.UrlRepository;
import com.diacencodumitru.url_shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;

    @Value("${url-shortener.expiration-minutes:1}")
    private long expirationMinutes;

    @Override
    public UrlResponseDTO shortenUrl(UrlRequestDTO data, HttpServletRequest request) {
        String id;

        do {
            id = RandomStringUtils.randomAlphanumeric(5, 10);
        } while (urlRepository.existsById(id));

        urlRepository.save(new UrlEntity(id, data.url(), LocalDateTime.now().plusMinutes(expirationMinutes)));

        String redirectUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/" + id)
                .replaceQuery(null)
                .build()
                .toUriString();

        return new UrlResponseDTO(data.url(), redirectUrl);
    }

    @Override
    public HttpHeaders redirect(String id) {
        UrlEntity url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException(id));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getUrl()));
        return headers;
    }
}
