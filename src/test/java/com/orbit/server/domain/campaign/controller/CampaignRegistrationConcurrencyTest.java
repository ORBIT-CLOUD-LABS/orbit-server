package com.orbit.server.domain.campaign.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.orbit.server.TestcontainersConfiguration;
import com.orbit.server.domain.artifact.entity.Artifact;
import com.orbit.server.domain.artifact.entity.ArtifactFile;
import com.orbit.server.domain.artifact.entity.ArtifactRelease;
import com.orbit.server.domain.artifact.repository.ArtifactRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 동시 등록은 요청마다 별도 트랜잭션이 커밋돼야 재현되므로, 테스트를 트랜잭션으로 묶지 않고 실제 요청 흐름으로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CampaignRegistrationConcurrencyTest {

    private static final int REQUEST_COUNT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long artifactId;

    @BeforeEach
    void setUp() {
        ArtifactRelease release = new ArtifactRelease("model-a", "1.0.1");
        Artifact artifact = new Artifact(release, new ArtifactFile("a".repeat(64), 1, release.originPath()));
        artifactId = artifactRepository.save(artifact).id();
    }

    // 트랜잭션 롤백에 기대지 않으므로 다른 DB 테스트에 데이터가 남지 않게 지운다. 대상 조건 테이블은 FK 의 ON DELETE CASCADE 로 함께 지워진다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM campaign");
        jdbcTemplate.update("DELETE FROM artifact");
    }

    @Test
    @DisplayName("CAMPAIGN-POL-001: 같은 캠페인 등록 요청이 동시에 오면 한 건만 201 로 등록하고 나머지는 409 로 거절한다")
    void registersOnlyOneOfConcurrentRequests() throws Exception {
        List<Integer> statuses = sendConcurrently(this::register);

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM campaign", Integer.class))
                .isEqualTo(1);
    }

    // 모든 요청이 출발선에서 기다리게 한 뒤 한꺼번에 풀어 중복 확인 구간이 겹치게 한다
    private static List<Integer> sendConcurrently(Callable<Integer> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> waitingRequest = () -> {
            start.await();
            return request.call();
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT)) {
            List<Future<Integer>> futures = IntStream.range(0, REQUEST_COUNT)
                    .mapToObj(i -> executor.submit(waitingRequest))
                    .toList();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        }
    }

    private int register() throws Exception {
        String body = """
                {"name": "campaign", "artifactId": %d, "targetVersion": "1.0.1",
                 "target": {"model": "model-a", "hwVersions": ["hw-1"], "regions": ["KR"]},
                 "startAt": "2026-10-01T00:00:00Z", "endAt": "2026-10-02T00:00:00Z"}""".formatted(artifactId);
        return mockMvc.perform(post("/api/v1/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
