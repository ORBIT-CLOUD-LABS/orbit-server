package com.orbit.server.global.external.cdn;

import com.orbit.server.domain.edge.entity.Edge;
import com.orbit.server.domain.edge.service.EdgeHealthProbe;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Edge nginx 의 {@code {baseUrl}/healthz} 를 호출해 2xx 면 정상으로 본다.
 *
 * <p>Spring 이 관리하는 {@code RestClient.Builder} 를 쓰지 않는다. 헬스체크 호출이 주기마다 HTTP 클라이언트 메트릭으로 쌓여 서버의 실제
 * 외부 호출 지표를 흐리지 않게 하기 위해서다. 상태 변화는 {@code orbit_cdn_edge_healthy} 로 따로 남긴다.
 */
@Slf4j
@Component
class HttpEdgeHealthProbe implements EdgeHealthProbe {

    private static final String HEALTH_PATH = "/healthz";
    private static final String TRAILING_SLASHES = "/+$";

    private final RestClient restClient;

    HttpEdgeHealthProbe(EdgeHealthProbeProperties properties) {
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory(properties.timeout()))
                .build();
    }

    @Override
    public boolean isUp(Edge edge) {
        try {
            return restClient
                    .get()
                    .uri(healthUrl(edge))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RestClientException e) {
            // 실패는 매 주기 반복될 수 있어 상태가 바뀔 때만 EdgeHealthBoard 가 경고로 남긴다. 원인은 필요할 때 debug 로 본다.
            // 연결 거부·타임아웃처럼 원인이 메시지에 드러나므로, 주기마다 스택트레이스가 쌓이지 않게 메시지만 남긴다
            log.debug("Edge 헬스체크에 실패했습니다: edgeId={}, reason={}", edge.id(), e.getMessage());
            return false;
        }
    }

    // 설정한 주소 끝의 '/' 를 떼어 '//healthz' 가 되지 않게 한다
    private static String healthUrl(Edge edge) {
        return edge.baseUrl().toString().replaceFirst(TRAILING_SLASHES, "") + HEALTH_PATH;
    }

    // 연결과 응답을 각각 제한해 응답 없는 Edge 하나가 주기 전체를 붙잡지 않게 한다
    private static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
