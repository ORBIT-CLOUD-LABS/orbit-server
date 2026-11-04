package com.orbit.server.domain.edge.service;

import com.orbit.server.domain.edge.entity.Edges;

/**
 * 배포 설정으로 정한 CDN Edge 목록을 조회한다. 헬스체크(#85), 매니페스트의 Edge 선택, 대시보드 Edge 상태 API(#88)가 쓴다.
 *
 * <p>Edge 목록은 관리자 API 나 DB 가 아니라 배포 설정에만 있다. 목록을 어디서 읽는지는 외부 연동의 일이므로 인터페이스로 두고, 구현은
 * global/external 에 둔다.
 */
public interface EdgeCatalog {

    /**
     * @return 서버가 뜰 때 읽어 검증을 마친 Edge 목록. 서버가 떠 있는 동안 바뀌지 않는다
     */
    Edges edges();
}
