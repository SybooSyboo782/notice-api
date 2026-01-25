package syboo.notice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {
    // 테스트 전체에서 공용으로 쓸 기준 시간
    public static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 25, 20, 0);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(
                FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
    }
}
