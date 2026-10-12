package com.orbit.server.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 현재 시각을 읽는 시계. 시각에 따라 결과가 달라지는 판정을 테스트에서 고정한 시각으로 확인할 수 있도록 주입받아 쓴다. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    // DB 와 애플리케이션 시각의 기준을 UTC 로 맞춘 설정과 같은 기준을 쓴다
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
