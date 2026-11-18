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
| `POST` | 생성, 상태 전이 | 생성은 `201` 과 `Location` 헤더, 상태 전이는 `200`·`204` |
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
public record CreateOrderRequest(@NotNull Long productId, @Positive int quantity) {}

@PostMapping("/orders")
public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = orderService.create(request.productId(), request.quantity());
    return ResponseEntity.created(URI.create("/orders/" + response.id())).body(response);
}
```

## 오류 응답

- 오류 응답은 RFC 9457 Problem Details 형식으로 통일한다. Spring 의 `ProblemDetail` 을 쓴다.
- `spring.mvc.problemdetails.enabled: true` 로 Spring 이 던지는 기본 예외(검증 실패 등)도 같은 형식으로 응답한다.
- 기능 예외는 `@RestControllerAdvice` 한 곳에서 상태 코드와 오류 코드로 바꾼다.
- 오류 코드(`ORDER_NOT_CANCELABLE`)를 함께 내려 클라이언트가 메시지 문구가 아니라 코드로 분기하게 한다.
- 응답에 스택 트레이스, SQL, 내부 클래스 이름을 넣지 않는다. 예상하지 못한 예외는 로그로 남기고 일반 메시지만 돌려준다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotCancelableException.class)
    public ProblemDetail handle(OrderNotCancelableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setProperty("code", "ORDER_NOT_CANCELABLE");
        return problem;
    }
}
```

## 목록과 페이징

- 개수가 늘어날 수 있는 목록은 페이징한다. `Pageable` 을 받고 최대 크기를 제한한다. (`spring.data.web.pageable.max-page-size`)
- `Page` 객체를 그대로 응답하지 않는다. 내용과 페이지 정보를 담은 응답 DTO 로 감싼다.
