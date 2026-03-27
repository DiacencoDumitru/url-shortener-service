package com.url_shortener;

import com.url_shortener.domain.dto.stats.UrlClickStatsDTO;
import com.url_shortener.domain.dto.url.UrlRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("url-shortener.rate-limit.capacity", () -> 2L);
        registry.add("url-shortener.rate-limit.refill-tokens", () -> 2L);
        registry.add("url-shortener.rate-limit.refill-duration-seconds", () -> 60L);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shortenUrlAndRedirectShouldWork() throws Exception {
        UrlRequestDTO request = new UrlRequestDTO("https://example.com");

        String responseBody = mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UrlResponsePayload payload = objectMapper.readValue(responseBody, UrlResponsePayload.class);

        mockMvc.perform(get(payload.shortenUrl().replace("http://localhost", "")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", request.url()));
    }

    @Test
    void redirectShouldIncrementClickCounterAndExposeStats() throws Exception {
        UrlRequestDTO request = new UrlRequestDTO("https://example.com/stats-flow");

        String responseBody = mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UrlResponsePayload payload = objectMapper.readValue(responseBody, UrlResponsePayload.class);
        String path = URI.create(payload.shortenUrl()).getPath();
        String id = path.startsWith("/") ? path.substring(1) : path;

        mockMvc.perform(get("/" + id))
                .andExpect(status().isFound());
        mockMvc.perform(get("/" + id))
                .andExpect(status().isFound());

        String statsBody = mockMvc.perform(get("/stats/" + id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UrlClickStatsDTO stats = objectMapper.readValue(statsBody, UrlClickStatsDTO.class);
        assertThat(stats.id()).isEqualTo(id);
        assertThat(stats.clicks()).isEqualTo(2L);
    }

    @Test
    void shortenShouldStoreRedirectTargetInRedisCache() throws Exception {
        UrlRequestDTO request = new UrlRequestDTO("https://example.com/cache-test");

        String responseBody = mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UrlResponsePayload payload = objectMapper.readValue(responseBody, UrlResponsePayload.class);
        String path = URI.create(payload.shortenUrl()).getPath();
        String id = path.startsWith("/") ? path.substring(1) : path;

        assertThat(stringRedisTemplate.opsForValue().get("cache:url:redirect:" + id)).isEqualTo(request.url());
    }

    @Test
    void shortenUrlShouldReturnTooManyRequestsWhenRateLimitExceeded() throws Exception {
        UrlRequestDTO request = new UrlRequestDTO("https://example.com");

        mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "client-a"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "client-a"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/shorten-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "client-a"))
                .andExpect(status().isTooManyRequests());
    }

    private record UrlResponsePayload(String url, String shortenUrl) {
    }
}

