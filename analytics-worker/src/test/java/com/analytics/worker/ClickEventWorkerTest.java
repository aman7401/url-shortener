package com.analytics.worker;

import com.analytics.model.Click;
import com.analytics.repository.ClickRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventWorkerTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ClickRepository clickRepository;
    @Mock private ListOperations<String, String> listOps;

    @InjectMocks private ClickEventWorker worker;

    @Test
    void saveClick_persistsCorrectCode() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop(eq("click_events"), any(Duration.class)))
                .thenReturn("abc123")
                .thenThrow(new RuntimeException("stop loop"));

        try {
            worker.run();
        } catch (RuntimeException ignored) {}

        ArgumentCaptor<Click> captor = ArgumentCaptor.forClass(Click.class);
        verify(clickRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("abc123");
    }

    @Test
    void noEvents_doesNotSaveToDb() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop(eq("click_events"), any(Duration.class)))
                .thenReturn(null)
                .thenThrow(new RuntimeException("stop loop"));

        try {
            worker.run();
        } catch (RuntimeException ignored) {}

        verify(clickRepository, never()).save(any());
    }
}
