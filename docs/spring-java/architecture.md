# Spring 구조 · 설정

Spring Boot 4(Spring Framework 7) 기준이다. Java 코드 규칙과 검증 명령은 Java 프리셋 문서를 따른다.
이미 다른 구조를 쓰는 프로젝트는 기존 구조를 따르고, 새로 만드는 부분만 이 문서에 맞춘다.

## 패키지

기능(도메인) 단위로 먼저 나누고, 그 안을 레이어로 나눈다. orbit 에서 기능 하나는 DDD 의 바운디드 컨텍스트다. → [../architecture.md](../architecture.md)

```text
com.orbit.server/
  domain/                 바운디드 컨텍스트 모음
    artifact/             컨텍스트 하나
      controller/         HTTP 요청·응답
      dto/
        request/          API 요청 DTO
        response/         API 응답 DTO
      service/            유스케이스(트랜잭션 경계), 외부 연동 포트, 도메인 서비스
        command/          서비스 입력 (XxxCommand)
        result/           서비스 출력·조회 결과 (XxxResult, XxxDetail)
        config/           이 컨텍스트의 설정 (XxxConfig, XxxProperties)
        listener/         이벤트 리스너 (XxxListener)
        scheduler/        주기 작업 등록 (XxxScheduler)
      repository/         Spring Data 인터페이스
      domain/             엔티티, 값 객체, enum
        event/            도메인 이벤트
      exception/          이 컨텍스트의 오류 코드, 예외
    campaign/
    shared/               Shared Kernel: 여러 컨텍스트가 같은 의미로 쓰는 값 객체 (하위 패키지 없음)
  global/                 설정, 전역 예외 처리, 보안 같은 기술 기반
    config/               Spring 설정 (JPA, Web, Swagger 등)
    error/                공통 예외 계층, 전역 예외 핸들러, 오류 코드
    security/             인증·인가 설정과 차량 인증 포트
      filter/             인증 필터
      handler/            인증·인가 실패를 공통 오류 형식으로 응답
    common/               공통 응답(ApiResponse, SuccessCode)과 공통 검증
    external/<시스템>/     외부 시스템 연동 구현 (origin: AWS S3, 로컬은 S3 호환 SeaweedFS)
      config/             이 연동의 설정
```

- 컨텍스트 안의 `domain` 은 최상위 `domain` 과 이름이 같다. 경로는 `com.orbit.server.domain.<컨텍스트>.domain` 이 된다.
  이 문서에서 레이어를 말할 때 `domain` 은 컨텍스트 안의 `domain` 을 뜻한다.
- 컨텍스트 이름은 [요구사항](../requirements.md)의 용어(유비쿼터스 언어)를 따른다. 새 컨텍스트를 만들기 전에 기존 컨텍스트에 속하는지 먼저 검토한다.
- 쓰지 않는 레이어는 만들지 않는다. (API 가 없는 기능에는 `controller/` 를 두지 않는다)
- 서비스의 입출력은 `dto` 가 아니라 `service` 의 `command/`(`XxxCommand`), `result/`(`XxxResult`, 조회 결과 `XxxSummary` / `XxxDetail`)에 둔다. `dto` 는 API 계약(요청·응답)만 둔다.
- 레이어 안에서 역할이 다른 클래스는 클래스가 적어도 역할 하위 패키지로 나눈다. 쓰는 역할만 만든다. → [../java/package-structure.md](../java/package-structure.md)
  `service` 의 역할 이름은 `command/`(입력), `result/`(출력·조회 결과), `config/`(설정), `listener/`(이벤트 구독), `scheduler/`(주기 작업)를 기본으로 쓴다. → 아래 "클래스를 추가할 때"
  여기에 맞지 않는 역할은 역할을 나타내는 이름을 붙인다. 예: `monitoring` 의 `stream/`(SSE 구독자 관리·변경분 전송 같은 전송 기반. 주기 등록은 `scheduler/`), `count/`(GROUP BY 집계 행과 그 일급 컬렉션)
  역할 하위 패키지에는 그 역할의 클래스만 둔다. 유스케이스 서비스는 `service` 루트에 두고 역할 하위 패키지를 쓴다. (`VehicleStatusStreamService` 는 루트, 전송 기반은 `stream/`)
  여러 역할 패키지가 함께 쓰는 enum·값 객체는 역할 패키지가 아니라 컨텍스트의 `domain` 에 둔다. (`UpdateFailureReason`)
  검색 조건처럼 입력이지만 커맨드가 아닌 record 는 `service` 에 그대로 둔다.
- 레이어 안에 기능 하위 패키지(`service/alert/`)를 만들지 않는다. 레이어 안에서 새 도메인 명사가 보이면 최상위 기능 패키지로 꺼낸다.
  역할 이름을 붙였더라도 유스케이스·스케줄·결과를 한 기능으로 묶어 담으면 기능 하위 패키지다.
- 하위 패키지로 나누느라 package-private 멤버를 못 쓰게 되면 쓰는 쪽에 필요한 멤버만 `public` 으로 연다.
  예: 결과 record 의 팩토리(`CampaignDetail.of`), 설정에서 만드는 구현의 생성자(`SecureLinkSigner`).
  - 연 멤버는 같은 컨텍스트의 다른 레이어와 그 포트를 구현하는 `global/external` 에서도 부를 수 있다. 다른 컨텍스트는 그 엔티티를 참조할 수 없어(`ContextBoundaryTest`) 엔티티를 인자로 받는 팩토리를 부를 수 없다.
  - 테스트를 대상 클래스와 같은 패키지로 옮기면서 `@Configuration` 클래스를 테스트에서 띄워야 하면 그 설정 클래스는 `public` 으로 연다. (`SecurityConfig`, `CdnConfig`)
  - 테스트 때문에 동작 메서드를 열지는 않는다. 그 메서드를 검증하는 테스트를 대상 클래스의 패키지로 옮긴다. (`SecureLinkSigner.sign`)
- `global` 에는 비즈니스 규칙을 두지 않는다. 두 기능이 같은 개념을 쓴다고 `global` 로 올리지 않고, 기능 경계를 먼저 다시 본다.
  여러 컨텍스트가 같은 의미와 규칙으로 쓰는 값 객체는 `domain/shared` 에 둔다. → [../architecture.md](../architecture.md) 의 Shared Kernel
- 외부 시스템 연동(결제, 메일 등)은 서비스가 쓰는 인터페이스를 `service` 에 두고, 구현은 `global/external/<시스템>` 에 둔다.
  구현은 그 포트(interface·`record`)와 `domain`·`exception` 에만 의존한다. 유스케이스 서비스나 도메인 서비스를 직접 부르지 않는다.

## 클래스를 추가할 때

패키지는 커진 뒤에 나누지 않고, 클래스를 추가할 때마다 위치를 정해서 자라게 한다. 위에서부터 답한다.

1. **어느 컨텍스트의 개념인가?** 기존 컨텍스트에 없는 도메인 명사면 새 컨텍스트를 만든다. 경계가 애매하면 사람에게 묻는다.
   여러 컨텍스트가 같은 의미와 규칙으로 쓰는 값 객체는 어느 한쪽에 두거나 복사하지 않고 `domain/shared` 에 둔다.
2. **어느 레이어인가?** 아래 이름 규칙의 접미사로 정한다.
3. **같은 목적의 클래스가 있는가?** 있으면 함께 두고, 없으면 레이어 안에 목적 이름의 하위 패키지를 만든다.
   기존 클래스도 함께 옮겨야 하면 파일 이동만 하는 별도 커밋으로 옮긴다. → [../java/package-structure.md](../java/package-structure.md) 의 "재구성 작업 방식"
4. **새 의존이 기존 컨텍스트 의존의 역방향인가?** 그러면 순환이다. 아래 "순환을 끊는 순서" 를 따른다.

| 이름 | 위치 |
| --- | --- |
| `XxxController` | `controller/` |
| `XxxRequest`, `XxxResponse` | `dto/request/`, `dto/response/` |
| `XxxService`, 도메인 서비스(`XxxPolicy` 등), 외부 연동 포트 | `service/` |
| `XxxCommand` | `service/command/` |
| `XxxResult`, 조회 결과(`XxxSummary`, `XxxDetail`) | `service/result/` |
| `XxxListener`, `XxxScheduler` | `service/listener/`, `service/scheduler/` |
| 컨텍스트 전용 `@Configuration`·`@ConfigurationProperties` (`XxxConfig`, `XxxProperties`) | `service/config/`. 여러 컨텍스트가 쓰는 기술 설정은 `global/config/` |
| `XxxRepository` | `repository/` |
| 엔티티, 값 객체, enum | `domain/` |
| 도메인 이벤트. 일어난 일을 과거형으로 짓는다 (`VehicleDeparted`) | `domain/event/` |
| `XxxException`, `XxxErrorCode` | `exception/` |

이름 규칙에 없는 역할은 접미사를 정하고 같은 방식으로 하위 패키지를 만든다. 하위 패키지 이름은 목적(`event`, `listener`, `policy`)으로 짓고, `util`·`common`·`etc` 처럼 목적이 없는 이름은 쓰지 않는다.

## 의존 방향

```text
controller → service → repository
                │          │
                └──▶ domain ◀┘
```

- `domain` 은 다른 레이어를 모른다. Spring 에 의존하지 않는다. (JPA 매핑 어노테이션만 예외)
  - 예외: Shared Kernel(`domain/shared`)의 값 객체는 쓸 수 있다.
- `global` 은 기능 패키지에 의존하지 않는다. (`global/external` 은 위 패키지 절의 포트 규칙만 예외)
- `exception` 은 어느 레이어에서든 쓸 수 있다.
- 목록·검색·대시보드처럼 필요한 컬럼만 읽는 조회는 Repository 의 `@Query` 에서 JPQL 생성자 표현식으로 조회 결과(`XxxSummary` 등)를 바로 만든다.
  조회 결과 타입은 `service` 에 두므로, `repository → service` 의존은 service 의 `record`(조회 결과, 집계 행, 검색 조건)에만 허용한다. (예: `ArtifactRepository.findAllSummaries()`)
  - 서비스·포트·정책·일급 컬렉션 같은 class 는 참조하지 않는다. repository 는 행을 record 로 돌려주고, 감싸거나 계산하는 일은 service 가 한다. (`CampaignResultCounts` 는 `CampaignStatsService` 가 만든다)
  - 허용 기준을 하위 패키지(`result/`, `count/` …)가 아니라 `record` 로 잡아, 역할 하위 패키지가 늘어도 예외를 넓히지 않는다. `ContextBoundaryTest` 가 검사한다.
  - `JpaRepository` 기본 메서드와 헷갈리지 않도록 메서드 이름에 결과 종류를 넣는다. (`findById` 가 아니라 `findSummaryById`)
- 다른 컨텍스트의 `domain`·`repository` 를 직접 참조하지 않는다. → [../architecture.md](../architecture.md) 의 컨텍스트 간 통신
- 기능 사이에 즉시 결과가 필요 없는 일은 도메인 이벤트(`ApplicationEventPublisher`, `@TransactionalEventListener`)로 나눈다.
- 기능끼리 순환하지 않는다. 순환이 생기면 아래 순서로 끊는다. 순환을 피하려고 기능을 한 패키지로 합치지 않는다.
- 의존 방향, 클래스 위치, 기능 간 순환은 `src/test/java/LayerArchitectureTest.java` 가 검사한다. 패키지 크기는 Java 프리셋의 `PackageSizeTest` 가 검사한다.
  orbit 은 `FEATURES_PACKAGE` 를 `com.orbit.server.domain`, `GLOBAL_PACKAGE` 를 `com.orbit.server.global` 로 두었다. → [testing.md](testing.md)
  - 컨텍스트 간 내부 참조 금지, `global/external` 의 포트 규칙, `repository → service` 의 record 제한은 템플릿에 없는 orbit 규칙이라 `com.orbit.server.ContextBoundaryTest` 가 따로 검사한다.
    `LayerArchitectureTest` 와 같은 규칙을 `ContextBoundaryTest` 에 두지 않는다. 같은 위반이 두 번 보고된다.
    예외는 `global/external` 규칙 하나다. 템플릿 규칙은 `service` 레이어 전체를 허용하는 근사라 orbit 은 더 엄격한 규칙을 함께 둔다.
    그래서 `external` 이 `controller`·`repository` 를 쓰면 두 테스트가 함께 실패한다. 유스케이스·도메인 서비스를 직접 부르면 `ContextBoundaryTest` 만 실패한다.
- 순환 허용 목록(`LayerArchitectureTest` 의 `ignoreDependency`), 큰 패키지 상한(`PackageSizeTest` 의 `KNOWN_LARGE_PACKAGES`), ArchUnit 기준선(`archunit_store`)은 늘리거나 고치지 않는다.
  넘으면 패키지를 나누고, 순환은 아래 순서로 끊는다.

### 순환을 끊는 순서

1. **의존 방향을 한쪽으로 정리한다.** 상위 개념이 하위 개념을 부르고 반대로는 부르지 않는다.
   예: `campaign` 은 `artifact` 를 부르고, `artifact` 는 `campaign` 을 모른다. 하위 컨텍스트에 필요한 값은 상위 컨텍스트가 인자로 넘긴다.
2. **두 컨텍스트가 함께 쓰는 개념을 별도 컨텍스트로 꺼낸다.** 두 컨텍스트가 그 컨텍스트만 바라보게 해서 서로를 모르게 한다.
   같은 의미와 규칙의 상태 없는 값 객체라면 `domain/shared` 에 둔다. → [../architecture.md](../architecture.md) 의 Shared Kernel
3. **즉시 결과가 필요 없는 일만 도메인 이벤트로 끊는다.** 결과를 돌려받으려고 이벤트를 주고받지 않는다.

어느 방법도 맞지 않으면 컨텍스트 경계가 잘못 나뉜 신호다. 혼자 정하지 않고 사람에게 묻는다.

## 레이어별 책임

| 레이어 | 둔다 | 두지 않는다 |
| --- | --- | --- |
| domain | 비즈니스 규칙, 상태 변경, 검증 | 트랜잭션, HTTP, 외부 API 호출 |
| service | 유스케이스 흐름 조율, 트랜잭션, 권한 확인 | 비즈니스 판단 (`if` 로 도메인 규칙 판단) |
| repository | DB 접근 | 비즈니스 규칙 |
| global/external | 외부 시스템 연동 구현 | 비즈니스 규칙 |
| controller, dto | 요청 검증(형식), DTO 변환, 응답 코드 | 비즈니스 로직 |

## 서비스가 커질 때

컨텍스트마다 서비스 하나(`OrderService`)로 시작하고, 커지면 유스케이스 단위로 나눈다.

- 나눌 신호: 생성자 의존이 5개를 넘는다. 또는 서로 다른 필드·의존만 쓰는 메서드 묶음이 한 클래스에 있다.
- 나누는 단위: 함께 바뀌는 유스케이스끼리 묶는다. 예: `OrderPlaceService`(주문·결제 요청), `OrderCancelService`(취소·환불), `OrderQueryService`(조회)
- 나눈 서비스가 서로를 부르기 시작하면 공통 로직을 도메인 객체나 `XxxPolicy` 로 내린다.
- 검사는 하지 않는다. 의존 수만으로는 나눌지 판단할 수 없어서 신호로만 쓴다.

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

- 설정을 쓰는 컨텍스트의 `XxxConfig` 클래스에 `@EnableConfigurationProperties(XxxProperties.class)` 를 붙여 등록한다. (`CheckInConfig`)
  애플리케이션 클래스에 `@ConfigurationPropertiesScan` 을 붙이지 않는다. 등록 방식이 두 가지로 갈리기 때문이다.

## null

- Spring Boot 4 는 JSpecify 로 null 가능성을 표시한다. null 이 될 수 있는 반환값·파라미터에는 `org.jspecify.annotations.Nullable` 을 붙인다.
- Spring 의 `org.springframework.lang.Nullable` 은 새로 쓰지 않는다.
