package com.orbit.server.domain.update.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

// 트랜잭션 밖에서 부르는 경우를 검증하므로 클래스에 @Transactional 을 붙이지 않는다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CheckInRecorderTest {

    @Autowired
    private CheckInRecorder checkInRecorder;

    @Test
    @DisplayName("상태와 결과를 한 트랜잭션에서 기록하도록 체크인 트랜잭션 밖에서는 기록을 거부한다")
    void rejectsRecordingOutsideTransaction() {
        assertThatThrownBy(() -> checkInRecorder.record(1L, new CheckInCommand("vehicle-001", "1.0.0")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
