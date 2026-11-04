package com.orbit.server.domain.edge.entity;

import com.orbit.server.global.common.Preconditions;
import com.orbit.server.global.error.InvalidValueException;
import java.net.URI;

/**
 * 차량에 업데이트 파일을 배포하는 CDN Edge 하나. Edge 안의 Pod 여러 개는 하나의 Edge 로 본다.
 *
 * <p>Edge 는 DB 가 아니라 배포 설정으로만 정의한다. 그래서 id 로 구분하는 엔티티지만 JPA 엔티티로 두지 않는다.
 *
 * @param id Edge 를 구분하는 값. 공백 불가, 50자 이하
 * @param name 대시보드에 보여 줄 이름. 공백 불가
 * @param baseUrl 차량이 파일을 받는 주소. null 불가
 * @param coordinates Edge 의 대표 좌표. null 불가
 */
public record Edge(String id, String name, URI baseUrl, Coordinates coordinates) {

    // 차량 상태의 배정 Edge 컬럼(vehicle_state.assigned_edge_id VARCHAR(50))에 저장되므로 그 길이를 넘지 않게 한다
    private static final int MAX_ID_LENGTH = 50;

    /**
     * @throws InvalidValueException id·이름이 비어 있거나, id 가 50자를 넘거나, 주소·좌표가 없는 경우
     */
    public Edge {
        Preconditions.requireText(id, MAX_ID_LENGTH, "Edge id");
        requireNotBlank(name);
        requireNotNull(baseUrl, "Edge 주소");
        requireNotNull(coordinates, "Edge 좌표");
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
