package com.orbit.server.domain.edge.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Edge 선택 규칙의 설정을 읽는다. {@code global} 은 {@code domain} 타입에 의존할 수 없으므로 설정을 쓰는 컨텍스트에 둔다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EdgeSelectionProperties.class)
class EdgeSelectionConfig {}
