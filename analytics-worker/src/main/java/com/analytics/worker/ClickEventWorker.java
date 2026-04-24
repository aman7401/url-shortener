package com.analytics.worker;

import com.analytics.model.Click;
import com.analytics.repository.ClickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ClickEventWorker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ClickEventWorker.class);
    private static final String CLICK_EVENTS_KEY = "click_events";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ClickRepository clickRepository;

    public ClickEventWorker(RedisTemplate<String, String> redisTemplate,
                            ClickRepository clickRepository) {
        this.redisTemplate = redisTemplate;
        this.clickRepository = clickRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Analytics worker started. Waiting for click events...");
        while (true) {
            try {
                String code = redisTemplate.opsForList()
                        .leftPop(CLICK_EVENTS_KEY, TIMEOUT);

                if (code != null) {
                    log.info("Recording click for code: {}", code);
                    saveClick(code);
                } else {
                    log.debug("No events, still listening...");
                }
            } catch (Exception e) {
                log.error("Error processing click event: {}", e.getMessage());
            }
        }
    }

    private void saveClick(String code) {
        Click click = new Click();
        click.setCode(code);
        clickRepository.save(click);
    }
}
