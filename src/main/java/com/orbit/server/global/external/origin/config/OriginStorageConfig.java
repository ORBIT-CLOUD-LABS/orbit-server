package com.orbit.server.global.external.origin.config;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/** Origin 저장소에 접속하는 S3 클라이언트 설정. 운영은 AWS S3, 로컬 시험은 S3 호환 서버에 접속한다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OriginProperties.class)
public class OriginStorageConfig {

    // 주소를 따로 주지 않으면 SDK 가 리전으로 AWS 기본 엔드포인트를 정하고 버킷 도메인(virtual-hosted style) 방식으로 접근한다
    @Bean
    public S3Client originS3Client(OriginProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        properties.customEndpoint().ifPresent(endpoint -> connectToCompatibleServer(builder, endpoint));
        return builder.build();
    }

    // SeaweedFS 같은 S3 호환 서버는 버킷별 하위 도메인(virtual-hosted style)을 지원하지 않으므로 경로 방식으로 접근한다
    private void connectToCompatibleServer(S3ClientBuilder builder, URI endpoint) {
        builder.endpointOverride(endpoint).forcePathStyle(true);
    }
}
