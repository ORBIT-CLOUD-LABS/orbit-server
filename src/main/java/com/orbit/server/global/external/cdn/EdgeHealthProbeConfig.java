package com.orbit.server.global.external.cdn;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Edge 헬스체크 호출 설정을 읽는다. Edge 목록 설정({@link CdnConfig})과 따로 두어 각 설정을 독립적으로 검증한다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EdgeHealthProbeProperties.class)
class EdgeHealthProbeConfig {}
