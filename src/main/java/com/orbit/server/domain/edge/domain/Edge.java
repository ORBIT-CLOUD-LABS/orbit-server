package com.orbit.server.domain.edge.domain;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 차량에 업데이트 파일을 배포하는 CDN Edge 하나. Edge 안의 Pod 여러 개는 하나의 Edge 로 본다.
 *
 * <p>Edge 는 DB 가 아니라 배포 설정으로만 정의한다. 그래서 id 로 구분하는 엔티티지만 JPA 엔티티로 두지 않는다.
 *
 * @param id Edge 를 구분하는 값. 공백 불가, 50자 이하
 * @param name 대시보드에 보여 줄 이름. 공백 불가
 * @param baseUrl 차량이 파일을 받는 주소. 호스트가 있는 http·https 절대 주소
 * @param coordinates Edge 의 대표 좌표. null 불가
 */
public record Edge(String id, String name, URI baseUrl, Coordinates coordinates) {

    // 차량 상태의 배정 Edge 컬럼(vehicle_state.assigned_edge_id VARCHAR(50))에 저장되므로 그 길이를 넘지 않게 한다
    private static final int MAX_ID_LENGTH = 50;
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    /**
     * @throws InvalidValueException id·이름이 비어 있거나, id 가 50자를 넘거나, 주소가 http·https 절대 주소가 아니거나, 좌표가 없는 경우
     */
    public Edge {
        Preconditions.requireText(id, MAX_ID_LENGTH, "Edge id");
        requireNotBlank(name);
        requireHttpUrl(baseUrl);
        requireNotNull(coordinates, "Edge 좌표");
    }

    // 'edge-b:30081' 처럼 scheme 을 빠뜨린 값도 URI 로는 만들어지므로(scheme 'edge-b'), 헬스체크·다운로드 URL 에서 터지기 전에 막는다
    private static void requireHttpUrl(URI baseUrl) {
        requireNotNull(baseUrl, "Edge 주소");
        String scheme = baseUrl.getScheme();
        if (scheme == null || !HTTP_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)) || baseUrl.getHost() == null) {
            throw new InvalidValueException("Edge 주소는 호스트가 있는 http·https 절대 주소여야 합니다.");
        }
    }

    private static void requireNotBlank(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Edge 이름은 비어 있을 수 없습니다.");
        }
    }

    private static void requireNotNull(Object value, String name) {
        if (value == null) {
            throw new InvalidValueException(name + " 은(는) 비어 있을 수 없습니다.");
        }
    }
}
