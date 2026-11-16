package com.orbit.server.domain.artifact.repository;

import static com.orbit.server.domain.artifact.entity.ArtifactSignatureFixture.SIGNATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 서명을 필수로 바꾸는 V9 마이그레이션이 서명 없는 artifact 행이 남은 환경에서 실패하는지 확인한다. (#89)
 *
 * <p>앱이 쓰는 스키마는 이미 최신 버전이므로, 같은 MySQL 컨테이너에 빈 스키마를 따로 만들어 V8 까지 올린 뒤 행을 넣고 V9 를 적용한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ArtifactSignatureMigrationTest {

    private static final String SCHEMA = "artifact_signature_migration";
    private static final MigrationVersion BEFORE_NOT_NULL = MigrationVersion.fromVersion("8");
    // 스키마를 만들고 지우려면 테스트 DB 사용자에게 없는 권한이 필요하다. 컨테이너는 root 비밀번호를 같은 값으로 둔다
    private static final String ROOT = "root";
    private static final int MYSQL_PORT = 3306;

    @Autowired
    private MySQLContainer mysql;

    @BeforeEach
    void createSchema() throws SQLException {
        execute(serverUrl(), "CREATE DATABASE " + SCHEMA);
    }

    @AfterEach
    void dropSchema() throws SQLException {
        execute(serverUrl(), "DROP DATABASE IF EXISTS " + SCHEMA);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 서명 없는 artifact 행이 남아 있으면 지우거나 채우지 않고 마이그레이션을 실패시킨다")
    void failsWhenUnsignedArtifactRemains() throws SQLException {
        flyway(BEFORE_NOT_NULL).migrate();
        insertArtifact("1.0.0", null, null);

        // MySQL 은 NULL 이 남은 컬럼을 NOT NULL 로 바꾸지 못해 "Invalid use of NULL value" 로 거부한다
        assertThatThrownBy(() -> flyway(MigrationVersion.LATEST).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("V9__make_artifact_signature_not_null.sql")
                .hasMessageContaining("Invalid use of NULL value");
        assertThat(countArtifacts()).isEqualTo(1);
    }

    @Test
    @DisplayName("ORIGIN-POL-001: 모든 artifact 에 서명이 있으면 서명 컬럼을 NOT NULL 로 바꾼다")
    void makesSignatureNotNullWhenAllArtifactsSigned() throws SQLException {
        flyway(BEFORE_NOT_NULL).migrate();
        insertArtifact("1.0.0", SIGNATURE.signature(), SIGNATURE.keyId());

        flyway(MigrationVersion.LATEST).migrate();

        assertThatThrownBy(() -> insertArtifact("1.0.1", null, null)).isInstanceOf(SQLException.class);
        assertThat(countArtifacts()).isEqualTo(1);
    }

    private Flyway flyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(schemaUrl(), ROOT, mysql.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private void insertArtifact(String version, String signature, String keyId) throws SQLException {
        String sql = "INSERT INTO artifact (model, version, sha256, size_bytes, path, signature, key_id)"
                + " VALUES ('model-a', ?, ?, 1, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(schemaUrl(), ROOT, mysql.getPassword());
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);
            statement.setString(2, "a".repeat(64));
            statement.setString(3, "/firmware/model-a/" + version + "/firmware.bin");
            statement.setString(4, signature);
            statement.setString(5, keyId);
            statement.executeUpdate();
        }
    }

    private int countArtifacts() throws SQLException {
        try (Connection connection = DriverManager.getConnection(schemaUrl(), ROOT, mysql.getPassword());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM artifact")) {
            result.next();
            return result.getInt(1);
        }
    }

    private void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, ROOT, mysql.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String serverUrl() {
        return urlOf("");
    }

    private String schemaUrl() {
        return urlOf(SCHEMA);
    }

    // 컨테이너가 TLS 없이 뜨므로 root 의 caching_sha2_password 인증에 공개 키 조회를 허용한다
    private String urlOf(String database) {
        return "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true"
                .formatted(mysql.getHost(), mysql.getMappedPort(MYSQL_PORT), database);
    }
}
