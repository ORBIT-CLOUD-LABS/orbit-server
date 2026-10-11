package com.orbit.server.domain.artifact.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.domain.artifact.application.OriginUploadException;
import com.orbit.server.domain.artifact.model.ArtifactFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * 실제 SeaweedFS 에 S3 API 로 업로드되는지 확인한다.
 *
 * <p>Mock 테스트로는 path-style 접근, 키 형식, Content-Length 처리처럼 서버가 실제로 받아들이는지를 알 수 없어 컨테이너를 띄운다.
 */
class S3OriginStorageIntegrationTest {

    // 운영 Origin 과 같은 버전으로 맞춘다
    private static final DockerImageName SEAWEEDFS_IMAGE = DockerImageName.parse("chrislusf/seaweedfs:4.48");
    private static final int S3_PORT = 8333;
    private static final String BUCKET = "orbit-firmware";
    private static final byte[] CONTENT = "firmware-binary".getBytes(StandardCharsets.UTF_8);
    // printf 'firmware-binary' | shasum -a 256
    private static final String CONTENT_SHA256 = "4924584e5b634794b60d87f7cfc6ed84086489968f9cfde652c095d41d063ee0";
    private static final ArtifactFile FILE =
            new ArtifactFile(CONTENT_SHA256, CONTENT.length, "/firmware/model-a/1.0.0/firmware.bin");

    private static final String ACCESS_KEY = "test-access-key";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String S3_CONFIG_PATH = "/etc/seaweedfs/s3.json";

    // SDK 는 항상 서명해서 요청하는데, 인증 설정이 없는 SeaweedFS 는 서명된 요청을 거부하므로 운영처럼 접근 키를 등록한다
    private static final String S3_CONFIG = """
            {"identities": [{
              "name": "orbit-server",
              "credentials": [{"accessKey": "%s", "secretKey": "%s"}],
              "actions": ["Admin", "Read", "Write", "List"]
            }]}
            """.formatted(ACCESS_KEY, SECRET_KEY);

    private static final GenericContainer<?> SEAWEEDFS = new GenericContainer<>(SEAWEEDFS_IMAGE)
            .withCopyToContainer(Transferable.of(S3_CONFIG), S3_CONFIG_PATH)
            .withCommand("server", "-s3", "-s3.config=" + S3_CONFIG_PATH)
            .withExposedPorts(S3_PORT)
            .waitingFor(Wait.forHttp("/").forPort(S3_PORT).forStatusCodeMatching(status -> status < 500));

    private static S3Client s3Client;

    @BeforeAll
    static void startSeaweedfs() {
        SEAWEEDFS.start();
        s3Client = new OriginStorageConfig().originS3Client(propertiesOf(SECRET_KEY));
        s3Client.createBucket(request -> request.bucket(BUCKET));
    }

    @AfterAll
    static void stopSeaweedfs() {
        s3Client.close();
        SEAWEEDFS.stop();
    }

    @Test
    @DisplayName("ORIGIN-POL-001: Origin 경로에서 앞의 '/' 를 뗀 키로 SeaweedFS 버킷에 파일을 올린다")
    void uploadsToSeaweedfs() {
        S3OriginStorage originStorage = new S3OriginStorage(s3Client, propertiesOf(SECRET_KEY));

        originStorage.upload(FILE, new ByteArrayResource(CONTENT));

        byte[] stored = s3Client.getObjectAsBytes(
                        request -> request.bucket(BUCKET).key("firmware/model-a/1.0.0/firmware.bin"))
                .asByteArray();
        assertThat(stored).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: 올린 바이트가 계산한 해시와 다르면 Origin 이 저장하지 않고 업로드 실패로 알린다")
    void rejectsContentNotMatchingSha256() {
        S3OriginStorage originStorage = new S3OriginStorage(s3Client, propertiesOf(SECRET_KEY));
        ArtifactFile file = new ArtifactFile(CONTENT_SHA256, CONTENT.length, "/firmware/model-a/2.0.0/firmware.bin");
        // 해시를 계산한 뒤 내용이 바뀐 상황: 크기는 같고 마지막 바이트만 다르다
        byte[] changed = "firmware-binarx".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> originStorage.upload(file, new ByteArrayResource(changed)))
                .isInstanceOf(OriginUploadException.class);
        assertThatThrownBy(() -> s3Client.headObject(
                        request -> request.bucket(BUCKET).key("firmware/model-a/2.0.0/firmware.bin")))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("ORIGIN-REQ-001: Origin 이 접근 키를 거부하면 업로드 실패로 알린다")
    void throwsUploadFailureWhenCredentialsRejected() {
        OriginProperties wrongSecret = propertiesOf("wrong-secret-key");
        try (S3Client rejectedClient = new OriginStorageConfig().originS3Client(wrongSecret)) {
            S3OriginStorage originStorage = new S3OriginStorage(rejectedClient, wrongSecret);

            assertThatThrownBy(() -> originStorage.upload(FILE, new ByteArrayResource(CONTENT)))
                    .isInstanceOf(OriginUploadException.class);
        }
    }

    private static OriginProperties propertiesOf(String secretKey) {
        URI endpoint = URI.create("http://" + SEAWEEDFS.getHost() + ":" + SEAWEEDFS.getMappedPort(S3_PORT));
        return new OriginProperties(endpoint, "us-east-1", BUCKET, ACCESS_KEY, secretKey);
    }
}
