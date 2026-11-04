package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Coordinates;
import com.orbit.server.domain.edge.entity.Edge;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpEdgeHealthProbeTest {

    private static final Duration TIMEOUT = Duration.ofMillis(300);
    private static final Coordinates SEOUL = new Coordinates(37.5665, 126.9780);

    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<Duration> delay = new AtomicReference<>(Duration.ZERO);
    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private final HttpEdgeHealthProbe probe = new HttpEdgeHealthProbe(new EdgeHealthProbeProperties(TIMEOUT));

    private HttpServer server;

    @BeforeEach
    void startEdge() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void stopEdge() {
        server.stop(0);
    }

    @Test
    @DisplayName("Edge 의 /healthz 가 200 이면 정상으로 본다")
    void upWhenHealthzReturnsOk() {
        assertThat(probe.isUp(edgeAt(baseUrl()))).isTrue();
        assertThat(requestedPath.get()).isEqualTo("/healthz");
    }

    @Test
    @DisplayName("주소 끝에 '/' 가 있어도 /healthz 를 호출한다")
    void callsHealthzWithTrailingSlash() {
        probe.isUp(edgeAt(baseUrl() + "/"));

        assertThat(requestedPath.get()).isEqualTo("/healthz");
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 404, 500, 503})
    @DisplayName("Edge 가 2xx 가 아닌 상태로 응답하면 실패로 본다")
    void downWhenStatusIsNot2xx(int code) {
        status.set(code);

        assertThat(probe.isUp(edgeAt(baseUrl()))).isFalse();
    }

    @Test
    @DisplayName("Edge 가 타임아웃 안에 응답하지 않으면 기다리지 않고 실패로 본다")
    void downWhenResponseTimesOut() {
        delay.set(TIMEOUT.multipliedBy(5));
        long startedAt = System.nanoTime();

        boolean up = probe.isUp(edgeAt(baseUrl()));

        assertThat(up).isFalse();
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(TIMEOUT.multipliedBy(3));
    }

    @Test
    @DisplayName("Edge 에 연결할 수 없으면 실패로 본다")
    void downWhenConnectionRefused() throws IOException {
        assertThat(probe.isUp(edgeAt("http://127.0.0.1:" + closedPort()))).isFalse();
    }

    private void respond(HttpExchange exchange) throws IOException {
        requestedPath.set(exchange.getRequestURI().getPath());
        sleep(delay.get());
        exchange.sendResponseHeaders(status.get(), -1);
        exchange.close();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static Edge edgeAt(String baseUrl) {
        return new Edge("edge-seoul", "서울", URI.create(baseUrl), SEOUL);
    }

    // 잠깐 열었다 닫은 포트는 바로 다른 프로세스가 쓰지 않는 한 연결이 거부된다
    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
