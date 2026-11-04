package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edge;

/**
 * Edge 가 지금 파일을 배포할 수 있는 상태인지 한 번 확인한다.
 *
 * <p>Edge 는 외부 시스템(nginx)이고 확인 방식(HTTP {@code /healthz})은 연동 구현의 일이므로 인터페이스로 두고, 구현은 global/external 에
 * 둔다.
 */
public interface EdgeHealthProbe {

    /**
     * 응답이 없거나 오류여도 예외를 던지지 않는다. 한 Edge 의 장애가 다른 Edge 확인을 막지 않게 하기 위해서다.
     *
     * @param edge 확인할 Edge
     * @return 정상 응답을 받으면 true, 연결 실패·타임아웃·오류 응답이면 false
     */
    boolean isUp(Edge edge);
}
