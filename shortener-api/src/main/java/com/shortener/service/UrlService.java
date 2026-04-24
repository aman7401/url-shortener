package com.shortener.service;

import com.shortener.dto.ShortenRequest;
import com.shortener.dto.ShortenResponse;
import com.shortener.model.Url;
import com.shortener.repository.SequenceRepository;
import com.shortener.repository.UrlRepository;
import com.shortener.util.Base62Encoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class UrlService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final UrlRepository urlRepository;
    private final SequenceRepository sequenceRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AnalyticsService analyticsService;

    public UrlService(UrlRepository urlRepository,
                      SequenceRepository sequenceRepository,
                      RedisTemplate<String, String> redisTemplate,
                      AnalyticsService analyticsService) {
        this.urlRepository = urlRepository;
        this.sequenceRepository = sequenceRepository;
        this.redisTemplate = redisTemplate;
        this.analyticsService = analyticsService;
    }

    public ShortenResponse shorten(ShortenRequest request) {
        long sequence = sequenceRepository.nextValue();
        String code = Base62Encoder.encode(sequence);

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
}
