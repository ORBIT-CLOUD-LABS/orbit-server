package com.orbit.server.global.security;

import com.orbit.server.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 차량 API 요청의 {@code Authorization: Bearer {vehicle-token}} 이 경로의 {@code vehicleId} 에 발급한 유효 토큰이면 요청을
 * 그 차량으로 인증한다. (VEHICLE-POL-003)
 *
 * <p>헤더와 경로에서 값을 꺼내는 일만 하고, 토큰 확인은 {@link VehicleTokenAuthenticator} 에 맡긴다. 확인에 실패하면 다음 필터로
 * 넘기지 않고 {@code 401 UNAUTHORIZED} 나 {@code 403 VEHICLE_MISMATCH} 로 바로 응답한다. 차량은 이 코드로 재등록 여부를
 * 정하므로, 인가 단계의 공통 코드(AUTHENTICATION_REQUIRED)로 바뀌면 안 되기 때문이다.
 *
 * <p>필터에서 난 오류는 {@code @RestControllerAdvice} 를 거치지 않으므로, {@link ErrorResponseAuthenticationEntryPoint} 와
 * 같은 이유로 {@link HandlerExceptionResolver} 를 통해 전역 예외 처리기로 넘긴다.
 */
class VehicleTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String VEHICLE_ROLE = "VEHICLE";
    static final String VEHICLE_ID = "vehicleId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final RequestMatcher vehiclePaths;
    private final VehicleTokenAuthenticator authenticator;
    private final HandlerExceptionResolver exceptionResolver;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    /**
     * @param vehiclePaths 차량 토큰으로 인증할 경로. 경로별 인증 규칙을 한 곳에서 관리하도록 보안 설정이 정해 넘기며, 경로 변수
     *     {@code vehicleId} 를 추출할 수 있어야 한다
     * @param authenticator 차량 토큰 확인 포트
     * @param exceptionResolver 전역 예외 처리기로 위임하는 MVC 예외 해석기
     */
    VehicleTokenAuthenticationFilter(
            RequestMatcher vehiclePaths,
            VehicleTokenAuthenticator authenticator,
            HandlerExceptionResolver exceptionResolver) {
        this.vehiclePaths = vehiclePaths;
        this.authenticator = authenticator;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !vehiclePaths.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            authenticate(request);
        } catch (RuntimeException e) {
            // 필터 밖으로 던지면 공통 오류 형식을 벗어나므로, 인증 실패뿐 아니라 경로 vehicleId 형식 오류(400)나 저장소
            // 장애(500)도 종류를 가리지 않고 전역 예외 처리기에 넘긴다
            exceptionResolver.resolveException(request, response, null, e);
            return;
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String token = bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        String vehicleId = vehicleId(request);
        authenticator.authenticate(token, vehicleId);
        authenticateAsVehicle(vehicleId);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    // 토큰 차량과 비교할 대상이 없으면 아무 차량의 토큰이나 통과하게 되므로, vehicleId 없는 경로에 이 필터를 건 설정 실수를
    // 요청 처리 전에 드러낸다
    private String vehicleId(HttpServletRequest request) {
        String vehicleId = vehiclePaths.matcher(request).getVariables().get(VEHICLE_ID);
        if (vehicleId == null) {
            throw new IllegalStateException("차량 토큰 인증 경로에는 {" + VEHICLE_ID + "} 가 있어야 합니다.");
        }
        return vehicleId;
    }

    // 인증 정보에 토큰 원문을 담지 않아 SecurityContext 가 로그·디버그 출력에 찍혀도 토큰이 남지 않는다
    private void authenticateAsVehicle(String vehicleId) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                vehicleId, null, AuthorityUtils.createAuthorityList("ROLE_" + VEHICLE_ROLE));
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
    }
}
