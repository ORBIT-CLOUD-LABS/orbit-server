package com.orbit.server.global.external.origin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Origin File Server 에 접속하는 S3 클라이언트 설정. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OriginProperties.class)
class OriginStorageConfig {

    // SeaweedFS 같은 S3 호환 서버는 버킷별 하위 도메인(virtual-hosted style)을 지원하지 않으므로 경로 방식으로 접근한다
    @Bean
    S3Client originS3Client(OriginProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }
}
