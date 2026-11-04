package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.edge.entity.Edges;
import com.orbit.server.domain.edge.service.EdgeCatalog;
import org.springframework.stereotype.Component;

/**
 * 배포 설정({@code orbit.cdn.edges})의 Edge 목록. 서버가 뜰 때 한 번 읽어 메모리에 둔다.
 *
 * <p>목록을 빈 생성 시점에 만들어 검증하므로, 잘못된 설정이면 첫 요청이 아니라 기동에서 실패한다. Spring 은 설정을 기동 때 한 번만 읽으므로
 * Edge 를 바꾸려면 Pod 를 다시 띄워야 한다.
 */
@Component
class ConfiguredEdgeCatalog implements EdgeCatalog {

    private final Edges edges;

    ConfiguredEdgeCatalog(CdnProperties properties) {
        this.edges = properties.toEdges();
    }

    @Override
    public Edges edges() {
        return edges;
    }
}
