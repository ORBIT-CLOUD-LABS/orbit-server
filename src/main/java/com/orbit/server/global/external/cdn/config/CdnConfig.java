package com.orbit.server.global.external.cdn.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** CDN 설정을 읽는다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CdnProperties.class)
public class CdnConfig {}
