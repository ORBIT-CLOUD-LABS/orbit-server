package com.orbit.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 주기 작업 실행을 켠다. 어떤 작업을 언제 돌릴지는 작업을 가진 컨텍스트가 등록한다. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfig {}
