package com.shortener.service;

import com.shortener.dto.ShortenRequest;
import com.shortener.dto.ShortenResponse;
import com.shortener.model.Url;
import com.shortener.repository.UrlRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class UrlService {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AnalyticsService analyticsService;
    private final SecureRandom random = new SecureRandom();

    public UrlService(UrlRepository urlRepository,
                      RedisTemplate<String, String> redisTemplate,
                      AnalyticsService analyticsService) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
        this.analyticsService = analyticsService;
    }

    public ShortenResponse shorten(ShortenRequest request) {
        String code = generateCode();
        Url url = new Url();
        url.setCode(code);
        url.setOriginalUrl(request.getUrl());
        urlRepository.save(url);

        redisTemplate.opsForValue().set(code, request.getUrl(), CACHE_TTL);

        return new ShortenResponse(code, "/r/" + code);
    }

    public String resolve(String code) {
        String cached = redisTemplate.opsForValue().get(code);
        if (cached != null) {
            analyticsService.recordClick(code);
            return cached;
        }

        Url url = urlRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        redisTemplate.opsForValue().set(code, url.getOriginalUrl(), CACHE_TTL);
        analyticsService.recordClick(code);
        return url.getOriginalUrl();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
