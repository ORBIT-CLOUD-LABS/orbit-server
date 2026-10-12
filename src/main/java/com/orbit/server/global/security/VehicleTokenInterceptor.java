package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 차량 API 요청의 {@code Authorization: Bearer {vehicle-token}} 을 경로의 {@code vehicleId} 와 함께 확인한다.
 * (VEHICLE-POL-003)
 *
 * <p>헤더와 경로에서 값을 꺼내는 일만 하고, 토큰 확인은 {@link VehicleTokenAuthenticator} 에 맡긴다.
 */
class VehicleTokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VEHICLE_ID = "vehicleId";

    private final VehicleTokenAuthenticator authenticator;

    /**
     * @param authenticator 차량 토큰 확인 포트
     */
    VehicleTokenInterceptor(VehicleTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * @throws BusinessException 토큰이 없거나 Bearer 형식이 아니거나 유효하지 않으면 UNAUTHORIZED, 다른 차량의 토큰이면
     *     VEHICLE_MISMATCH
     * @throws IllegalStateException 차량 API 경로에 {@code vehicleId} 가 없는 경우
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 컨트롤러가 없는 경로는 정적 리소스 핸들러가 404 로 끝내므로 보호할 대상이 없다
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        authenticator.authenticate(token, vehicleId(request));
        return true;
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    // 토큰 차량과 비교할 대상이 없으면 인증을 건너뛰게 되므로, vehicleId 없는 차량 API 를 만든 실수를 요청 처리 전에 드러낸다
    @SuppressWarnings("unchecked")
    private String vehicleId(HttpServletRequest request) {
        Map<String, String> variables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables == null || !variables.containsKey(VEHICLE_ID)) {
            throw new IllegalStateException("차량 토큰 인증 경로에는 {" + VEHICLE_ID + "} 가 있어야 합니다.");
        }
        return variables.get(VEHICLE_ID);
    }
}
