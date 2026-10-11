package com.orbit.server.domain.artifact.application;

import java.time.Instant;

/**
 * 캠페인 등록 때 고를 수 있도록 보여 주는 Origin 파일 정보. (ORIGIN-REQ-001)
 *
 * <p>조회 전용이라 Artifact 에 getter 를 두지 않고 DB 에서 바로 이 형태로 읽는다.
 *
 * @param artifactId Artifact 의 ID. 캠페인 등록 때 이 값으로 파일을 고른다
 * @param model 차종
 * @param version 소프트웨어 버전
 * @param fileSize 파일 크기(byte)
 * @param sha256 파일의 SHA-256
 * @param path Origin 경로
 * @param createdAt 등록 시각
 */
public record ArtifactSummary(
        Long artifactId, String model, String version, long fileSize, String sha256, String path, Instant createdAt) {}
