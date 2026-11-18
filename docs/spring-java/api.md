# API

Spring MVC 로 REST API 를 만들 때의 규칙이다.

## 컨트롤러

- 컨트롤러는 요청 검증, 서비스 호출, 응답 변환만 한다. 업무 규칙은 서비스와 도메인에 둔다.
- 요청·응답은 DTO(record)로 받고 돌려준다. → [architecture.md](architecture.md)
- 컨트롤러마다 try-catch 로 예외를 응답으로 바꾸지 않는다. 전역 예외 처리에 맡긴다.

## URL 과 메서드

- 자원은 복수 명사로 쓴다. (`/orders`, `/orders/{orderId}`) URL 에 동사를 넣지 않는다. (`/getOrders`)
- CRUD 로 표현하기 어려운 상태 전이는 하위 경로로 쓴다. (`POST /orders/{orderId}/cancel`)

| 메서드 | 용도 | 성공 응답 |
| --- | --- | --- |
| `GET` | 조회 | `200` |
| `POST` | 생성, 상태 전이 | 생성은 `201` 과 생성한 ID 를 담은 `ApiResponse` 본문(`Location` 헤더는 두지 않는다), 상태 전이는 `200`·`204` |
| `PUT` | 전체 교체 | `200`·`204` |
| `PATCH` | 일부 수정 | `200`·`204` |
| `DELETE` | 삭제 | `204` |

| 실패 상황 | 상태 코드 |
| --- | --- |
| 요청 형식·값 검증 실패 | `400` |
| 인증 없음 | `401` |
| 권한 없음 | `403` |
| 자원 없음 | `404` |
| 현재 상태와 충돌 (이미 취소된 주문 취소 등) | `409` |
| 서버 오류 | `500` |

## 요청 검증

- 요청 DTO 필드에 Bean Validation(`@NotBlank`, `@Positive`, `@Size` 등)을 붙이고, 컨트롤러 파라미터에 `@Valid` 를 붙인다.
- 형식 검증(필수, 길이, 범위)은 DTO 에서, 업무 규칙 검증(재고, 상태)은 도메인·서비스에서 한다.

```java
public record RegisterCampaignRequest(@NotBlank String name, @NotNull Long artifactId, ...) {}

@PostMapping
public ResponseEntity<ApiResponse<CampaignRegistrationResponse>> register(
        @Valid @RequestBody RegisterCampaignRequest request) {
    Long campaignId = campaignRegistrationService.register(request.toCommand());
    return ResponseEntity.status(SuccessCode.CREATED.getStatus())
            .body(ApiResponse.success(SuccessCode.CREATED, new CampaignRegistrationResponse(campaignId)));
}
```

## 응답 형식

성공·실패 모두 공통 응답 `ApiResponse`(`global/common`)로 감싼다. 응답 형식은 API 계약이라 바꾸면 클라이언트가 깨진다.
RFC 9457 Problem Details(`ProblemDetail`)는 쓰지 않는다.

```json
{ "isSuccess": false, "code": "CAMPAIGN_NOT_FOUND", "message": "캠페인이 없습니다.", "data": null, "error": null }
```

- 성공은 `ApiResponse.success(data)`(200) 또는 `ApiResponse.success(SuccessCode.CREATED, data)` 처럼 성공 코드를 지정한다.
- 클라이언트는 메시지 문구가 아니라 `code` 로 분기한다. `error` 는 추가 정보가 필요해질 때를 위해 자리만 두었고 지금은 항상 `null` 이다.

## 오류 응답

- 오류 코드는 `ErrorCode` 를 구현한 enum 으로 정한다. 공통 오류는 `global/error` 의 `CommonErrorCode`, 기능 오류는 컨텍스트 `exception` 의 `XxxErrorCode` 에 둔다.
  각 상수에 HTTP 상태와 메시지를 함께 적는다.
- 기능 예외는 `BusinessException(errorCode)` 로 던진다. 원인 예외가 있으면 `BusinessException(errorCode, cause)` 로 cause 를 유지한다.
  외부 연동 포트처럼 메서드가 던지는 예외를 계약으로 드러내야 할 때는 `BusinessException` 을 상속한 예외 클래스를 만든다.
  (`OriginStorage` 의 `OriginFileExistsException`, `OriginUploadException`)
- `GlobalExceptionHandler`(`@RestControllerAdvice`) 한 곳에서 `ErrorCode` 를 상태 코드와 `ApiResponse` 로 바꾼다.
  Spring 이 던지는 기본 예외(검증 실패, 본문 해석 실패 등)도 같은 형식으로 응답한다.
- 컨트롤러 메서드에는 응답할 수 있는 오류 코드를 `@ApiErrorCodes` 로 적어 API 문서(Swagger)에 표시한다. 공통 오류(400, 401, 500)는 적지 않아도 붙는다.
- 응답에 스택 트레이스, SQL, 내부 클래스 이름을 넣지 않는다. 예상하지 못한 예외는 로그로 남기고 일반 메시지만 돌려준다.

```java
@Getter
public enum CampaignErrorCode implements ErrorCode {
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "캠페인이 없습니다."),
    ...
}

// 서비스
Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND));

// 컨트롤러
@ApiErrorCodes(type = CampaignErrorCode.class, codes = {"CAMPAIGN_NOT_FOUND"})
```

## 목록과 페이징

- 개수가 늘어날 수 있는 목록은 페이징한다. `Pageable` 을 받고 최대 크기를 제한한다. (`spring.data.web.pageable.max-page-size`)
- `Page` 객체를 그대로 응답하지 않는다. 내용과 페이지 정보를 담은 응답 DTO 로 감싼다.
