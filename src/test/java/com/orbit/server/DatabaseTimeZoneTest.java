package com.orbit.server;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseTimeZoneTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("DB 세션 시간대를 UTC 로 고정해 DB 기본값 시각도 UTC 로 채운다")
    void fixesSessionTimeZoneToUtc() {
        Object sessionTimeZone =
                em.createNativeQuery("SELECT @@session.time_zone").getSingleResult();

        assertThat(sessionTimeZone).isEqualTo("+00:00");
    }
}
