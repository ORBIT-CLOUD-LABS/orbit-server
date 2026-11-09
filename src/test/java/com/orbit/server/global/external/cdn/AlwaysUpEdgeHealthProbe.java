package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.service.EdgeHealthProbe;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 테스트에서 실제 HTTP 헬스체크({@link HttpEdgeHealthProbe}) 대신 쓰는 probe. 모든 Edge 를 정상으로 본다.
 *
 * <p>테스트 프로필의 Edge 주소는 접속되지 않는 가짜 주소라 실제로 확인하면 항상 실패한다. Spring 7 테스트 컨텍스트는 테스트 클래스마다
 * 헬스체크 스케줄을 다시 시작하므로, 실패가 쌓여 Edge 가 비정상이 되면 Edge 선택 테스트가 클래스 실행 순서에 따라 깨진다.
 *
 * <p>테스트 소스의 {@code @Component} 는 {@code @SpringBootTest} 의 컴포넌트 스캔에 포함되므로 모든 테스트 컨텍스트에 따로 등록하지 않아도
 * 된다. 헬스체크 판정 자체는 {@code EdgeHealthBoard}·{@code EdgeHealthCheckService} 단위 테스트가 검증한다.
 */
@Primary
@Component
class AlwaysUpEdgeHealthProbe implements EdgeHealthProbe {

    @Override
    public boolean isUp(Edge edge) {
        return true;
    }
}
