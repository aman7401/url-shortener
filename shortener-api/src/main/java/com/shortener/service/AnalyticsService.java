package com.shortener.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final String CLICK_EVENTS_KEY = "click_events";

    private final RedisTemplate<String, String> redisTemplate;

    public AnalyticsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordClick(String code) {
        redisTemplate.opsForList().rightPush(CLICK_EVENTS_KEY, code);
    }
}
