# Spring 구조 · 설정

Spring Boot 4(Spring Framework 7) 기준이다. Java 코드 규칙과 검증 명령은 Java 프리셋 문서를 따른다.
이미 다른 구조를 쓰는 프로젝트는 기존 구조를 따르고, 새로 만드는 부분만 이 문서에 맞춘다.

## 패키지

기능(도메인) 단위로 먼저 나누고, 그 안을 레이어로 나눈다.

```text
com.example.app/
  order/            기능 하나
    controller/     HTTP 요청·응답
    dto/            요청·응답 DTO
    service/        유스케이스, 트랜잭션 경계
    repository/     Spring Data 인터페이스
    domain/         엔티티, 값 객체, 도메인 규칙
    exception/      이 기능의 예외
  member/
  global/           설정, 전역 예외 처리, 보안 같은 기술 기반
```

- 필요 없는 하위 패키지는 만들지 않는다. (API 가 없는 기능에는 `controller/` 를 두지 않는다)
- `global` 에는 비즈니스 규칙을 두지 않는다. 두 기능이 같은 개념을 쓴다고 `global` 로 올리지 않고, 기능 경계를 먼저 다시 본다.
- 외부 시스템 연동(결제, 메일 등)은 서비스가 쓰는 인터페이스를 `service` 에 두고, 구현은 `global/external/<시스템>` 에 둔다.

## 의존 방향

```text
controller → service → repository
                │          │
                └──▶ domain ◀┘
```

- `domain` 은 다른 레이어를 모른다. Spring 에 의존하지 않는다. (JPA 매핑 어노테이션만 예외)
- `global` 은 기능 패키지에 의존하지 않는다.
- 기능 사이에 즉시 결과가 필요 없는 일은 도메인 이벤트(`ApplicationEventPublisher`, `@TransactionalEventListener`)로 나눈다.
- 기능끼리 순환하지 않는다. 역방향이 필요하면 도메인 이벤트로 끊는다.
- 의존 방향, 클래스 위치, 기능 간 순환은 설치된 `src/test/java/LayerArchitectureTest.java` 가 검사한다. 패키지 크기는 Java 프리셋의 `PackageSizeTest` 가 검사한다.
  설치한 뒤 `LayerArchitectureTest` 의 `FEATURES_PACKAGE`, `GLOBAL_PACKAGE` 를 프로젝트 패키지로 바꾼다. → [testing.md](testing.md)
- 순환 허용 목록(`LayerArchitectureTest` 의 `ignoreDependency`), 큰 패키지 상한(`PackageSizeTest` 의 `KNOWN_LARGE_PACKAGES`), ArchUnit 기준선(`archunit_store`)은 늘리거나 고치지 않는다.
  넘으면 패키지를 나누고, 역방향 의존은 도메인 이벤트로 끊는다.

## 빈과 주입

- 생성자로 주입한다. 필드 주입(`@Autowired` 필드)을 쓰지 않는다. 생성자가 하나면 `@Autowired` 를 붙이지 않는다.
- 주입받는 필드는 `private final` 이다. Lombok 을 쓰면 `@RequiredArgsConstructor` 로 생성자를 만든다.
- 구현이 하나뿐인 서비스에 인터페이스를 만들지 않는다. (`XxxServiceImpl` 금지)
- 현재 시각은 `Clock` 빈을 주입받아 쓴다. 테스트에서 시각을 고정할 수 있다.

## DTO 경계

- 컨트롤러는 엔티티를 요청으로 받거나 응답으로 돌려주지 않는다. 지연 로딩 예외, 순환 참조, 내부 필드 노출이 생긴다.
- 요청·응답 DTO 는 `record` 로 만든다.
- 엔티티 → 응답 변환은 DTO 의 정적 팩토리(`OrderResponse.from(order)`)에 둔다. 엔티티가 DTO 를 모르게 한다.

```java
public record OrderResponse(Long id, String status, int totalPrice) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus().name(), order.totalPrice());
    }
}
```

## 설정

- 설정 값은 `@ConfigurationProperties` 와 `record` 로 묶어 받는다. `@Value` 를 여러 클래스에 흩어 두지 않는다.
- `@Validated` 와 Bean Validation 으로 애플리케이션 시작 시점에 검증한다. 값이 틀리면 뜨지 않는다.
- 비밀값은 설정 파일에 적지 않고 `${DB_PASSWORD}` 처럼 환경변수를 참조한다.
- 환경별 설정은 `application-<profile>.yml` 로 나누고, 운영 값은 환경변수로 덮어쓴다.

```java
@Validated
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(@NotBlank String baseUrl, @NotNull Duration timeout) {}
```

- 애플리케이션 클래스에 `@ConfigurationPropertiesScan` 을 붙여 등록한다.

## null

- Spring Boot 4 는 JSpecify 로 null 가능성을 표시한다. null 이 될 수 있는 반환값·파라미터에는 `org.jspecify.annotations.Nullable` 을 붙인다.
- Spring 의 `org.springframework.lang.Nullable` 은 새로 쓰지 않는다.
