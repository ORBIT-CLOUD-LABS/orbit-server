package com.orbit.server.global.config;

import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;

/**
 * API 문서의 Tag 이름·설명·표시 순서를 한 곳에서 정한다.
 *
 * <p>컨트롤러는 {@code @Tag(name = ApiTags.XXX)} 로 이름 상수만 참조한다. 어노테이션 값에는 컴파일 시점 상수만 들어가므로 이름은 문자열 상수로
 * 두고, 설명과 순서는 {@link #DEFINITIONS} 에 둔다. Swagger UI 는 이 순서대로 그룹을 보여 준다.
 */
public final class ApiTags {

    public static final String ADMIN_ARTIFACT = "관리자 · 업데이트 파일";
    public static final String ADMIN_CAMPAIGN = "관리자 · 캠페인";
    public static final String DASHBOARD_VEHICLE = "대시보드 · 차량 상태";
    public static final String VEHICLE_REGISTRATION = "차량 · 등록";
    public static final String VEHICLE_UPDATE = "차량 · 업데이트";

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(ADMIN_ARTIFACT, "Origin 에 올린 업데이트 파일(Artifact) 등록·조회"),
            new Definition(ADMIN_CAMPAIGN, "업데이트 캠페인 등록·조회"),
            new Definition(DASHBOARD_VEHICLE, "차량 상태 스냅샷 조회와 변경분 SSE 스트림"),
            new Definition(VEHICLE_REGISTRATION, "차량 등록과 차량별 토큰 발급"),
            new Definition(VEHICLE_UPDATE, "체크인으로 업데이트 대상을 확인하고 매니페스트로 다운로드 정보를 받는다"));

    private ApiTags() {}

    /** 문서 생성 결과가 공유 객체를 바꾸지 않도록 부를 때마다 새 Tag 목록을 만든다. */
    static List<Tag> ordered() {
        return DEFINITIONS.stream().map(Definition::toTag).toList();
    }

    private record Definition(String name, String description) {

        Tag toTag() {
            return new Tag().name(name).description(description);
        }
    }
}
