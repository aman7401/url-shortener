package com.shortener.service;

import com.shortener.dto.ShortenRequest;
import com.shortener.dto.ShortenResponse;
import com.shortener.model.Url;
import com.shortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock private UrlRepository urlRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private AnalyticsService analyticsService;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private UrlService urlService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shorten_savesUrlAndReturnsShortCode() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://google.com");

        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse response = urlService.shorten(request);

        assertThat(response.getShortCode()).hasSize(6);
        assertThat(response.getShortUrl()).startsWith("/r/");
        verify(urlRepository).save(any(Url.class));
        verify(valueOps).set(anyString(), eq("https://google.com"), any());
    }

    @Test
    void resolve_returnsUrlFromCache_whenCacheHit() {
        when(valueOps.get("abc123")).thenReturn("https://google.com");

        String result = urlService.resolve("abc123");

        assertThat(result).isEqualTo("https://google.com");
        verify(urlRepository, never()).findById(any());
        verify(analyticsService).recordClick("abc123");
    }

    @Test
    void resolve_queriesDb_whenCacheMiss() {
        when(valueOps.get("abc123")).thenReturn(null);

        Url url = new Url();
        url.setCode("abc123");
        url.setOriginalUrl("https://google.com");
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(url));

        String result = urlService.resolve("abc123");

        assertThat(result).isEqualTo("https://google.com");
        verify(urlRepository).findById("abc123");
        verify(analyticsService).recordClick("abc123");
    }

    @Test
    void resolve_throws404_whenCodeNotFound() {
        when(valueOps.get("missing")).thenReturn(null);
        when(urlRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolve("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("URL not found");
    }
}
