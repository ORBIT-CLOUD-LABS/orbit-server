package com.orbit.server.global.external.origin;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

/** 접속 주소만 확인하므로 실제 요청 없이 SDK 가 만드는 객체 URL 로 검증한다. */
class OriginStorageConfigTest {

    private static final String BUCKET = "orbit-firmware";
    private static final String KEY = "firmware/model-a/1.0.0/firmware.bin";

    @Test
    @DisplayName("endpoint 가 없으면 AWS S3 기본 엔드포인트에 버킷 도메인 방식으로 접속한다")
    void usesAwsDefaultEndpointWithVirtualHostedStyle() {
        OriginProperties properties = new OriginProperties(null, "ap-northeast-2", BUCKET, "key", "secret");

        try (S3Client s3Client = new OriginStorageConfig().originS3Client(properties)) {
            assertThat(objectUrlOf(s3Client))
                    .hasToString("https://orbit-firmware.s3.ap-northeast-2.amazonaws.com/" + KEY);
        }
    }

    @Test
    @DisplayName("endpoint 가 있으면 S3 호환 서버에 경로 방식으로 접속한다")
    void usesCustomEndpointWithPathStyle() {
        OriginProperties properties =
                new OriginProperties(URI.create("http://localhost:8333"), "us-east-1", BUCKET, "key", "secret");

        try (S3Client s3Client = new OriginStorageConfig().originS3Client(properties)) {
            assertThat(objectUrlOf(s3Client)).hasToString("http://localhost:8333/orbit-firmware/" + KEY);
        }
    }

    private URL objectUrlOf(S3Client s3Client) {
        return s3Client.utilities().getUrl(request -> request.bucket(BUCKET).key(KEY));
    }
}
