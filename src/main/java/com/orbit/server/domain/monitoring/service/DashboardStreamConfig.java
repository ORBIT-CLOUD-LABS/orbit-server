package com.orbit.server.domain.monitoring.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 대시보드 SSE 설정을 읽는다. {@code global} 은 {@code domain} 타입에 의존할 수 없으므로 설정을 쓰는 컨텍스트에 둔다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DashboardStreamProperties.class)
class DashboardStreamConfig {}
