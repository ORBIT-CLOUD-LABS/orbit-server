package com.orbit.server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * DB 테스트가 쓰는 MySQL 컨테이너.
 *
 * <p>스키마가 MySQL 전용 기능(검사 제약의 REGEXP_LIKE, 생성 컬럼, 세션 시간대)에 의존하므로 대체 DB 가 아닌 실제 MySQL 을 띄운다.
 * 컨테이너를 Bean 으로 두어 같은 설정의 테스트끼리 컨텍스트 캐시로 한 번만 띄운다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // 기존 CI 테스트 DB 와 같은 버전으로 맞춘다
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(MYSQL_IMAGE);
    }
}
