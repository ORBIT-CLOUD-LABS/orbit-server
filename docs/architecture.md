# 아키텍처 (DDD)

도메인 구조는 DDD 를 따른다. 코드 레벨 규칙은 [clean-code.md](clean-code.md), [java-checklist.md](java-checklist.md).

## 패키지 구조

최상위는 `domain`(비즈니스)과 `global`(전역 기반)로 나눈다.
`domain` 아래는 바운디드 컨텍스트로, 각 컨텍스트 안은 기술 역할로 나눈다.
폴더 이름만 보고 어떤 클래스가 있는지 알 수 있게 하기 위해서다.

```
com.orbit.server
├── domain/                     # 바운디드 컨텍스트 모음
│   ├── artifact/               # 바운디드 컨텍스트
│   │   ├── controller/         # Controller
│   │   ├── dto/
│   │   │   ├── request/        # API 요청 DTO
│   │   │   └── response/       # API 응답 DTO
│   │   ├── service/            # 서비스(트랜잭션 경계), Command/Result·조회 결과, 외부 연동 포트, 도메인 서비스
│   │   ├── repository/         # Spring Data Repository 인터페이스
│   │   ├── entity/             # 엔티티, 값 객체, enum, 도메인 이벤트
│   │   └── exception/          # 에러 코드, 커스텀 예외
│   ├── campaign/
│   └── shared/                 # Shared Kernel: 여러 컨텍스트가 같은 의미로 쓰는 값 객체 (하위 패키지 없음)
└── global/                     # 도메인과 무관한 전역 설정·공통 기반
    ├── config/                 # Spring 설정 (JPA, Web, Swagger 등)
    ├── error/                  # 공통 예외 계층, 전역 예외 핸들러, 에러 응답
    ├── security/               # 인증·인가
    ├── common/                 # BaseTimeEntity 등 공통 상위 타입
    └── external/               # 외부 시스템 연동 구현 (시스템별 하위 패키지)
        └── origin/             # Origin File Server(S3 호환)
```

- 컨텍스트 이름은 [요구사항](requirements.md)의 용어(유비쿼터스 언어)를 따른다.
- 새 컨텍스트를 만들기 전에 기존 컨텍스트에 속하는지 먼저 검토한다.
- 필요 없는 하위 패키지는 만들지 않는다. (예: API 가 없는 컨텍스트에는 `controller` 를 두지 않는다)
- 서비스의 입출력(`XxxCommand` / `XxxResult`, 조회 결과 `XxxSummary` / `XxxDetail`)은 `dto` 가 아니라 `service` 에 둔다.
  `dto` 는 API 계약(요청·응답)만 둔다.

### global 규칙

- 비즈니스 규칙을 두지 않는다.
- `global` 은 `domain` 패키지에 의존하지 않는다. (`domain → global` 방향만 허용)
  - 예외: `global/external` 은 `domain` 의 `service` 에 있는 포트(예: `OriginStorage`)를 구현하므로 그 포트와 포트가 쓰는 타입에 의존할 수 있다.
- 두 컨텍스트가 같은 개념을 쓴다고 `global` 로 올리지 않는다. 컨텍스트 경계를 먼저 다시 검토한다.

### Shared Kernel (`domain/shared`)

여러 컨텍스트가 **같은 의미와 같은 규칙**으로 쓰는 도메인 개념을 둔다. (예: 점으로 나눈 숫자 버전 `DottedVersion`)
비즈니스 규칙이라 `global` 에는 둘 수 없고, 한 컨텍스트의 `entity` 에 두면 다른 컨텍스트가 참조할 수 없기 때문이다.

- 상태 없는 값 객체만 둔다. 엔티티·Repository·서비스는 두지 않는다.
- 하위 패키지를 만들지 않는다. 바운디드 컨텍스트가 아니므로 계층으로 나누지 않는다.
- 다른 컨텍스트에 의존하지 않는다. `global` 에만 의존할 수 있다.
- 모든 컨텍스트의 `entity` 를 포함해 어느 계층에서나 쓸 수 있다.
- 바꾸면 모든 컨텍스트에 영향을 주므로 작게 유지한다. 두 컨텍스트가 비슷한 개념을 쓴다는 이유만으로 옮기지 않는다.
  이름이 같아도 컨텍스트마다 규칙(길이 제한 등)이 다르면 각 컨텍스트의 값 객체에 두고, 같은 부분만 Shared Kernel 을 쓴다.

## 의존 방향

```
controller → service → repository → entity
             service → entity
global/external → service   (포트 구현)
```

- `entity` 는 다른 패키지를 모른다. Spring 에 의존하지 않는다. (JPA 매핑 어노테이션만 예외, 아래 참고)
  - 예외: Shared Kernel(`domain/shared`)의 값 객체는 쓸 수 있다.
- `service` 는 외부 시스템을 포트(인터페이스)로만 쓴다. 구현은 `global/external` 에 둔다.
- 목록·검색·대시보드처럼 필요한 컬럼만 읽는 조회는 Repository 의 `@Query` 에서 JPQL 생성자 표현식으로 조회 결과(`XxxSummary` 등)를 바로 만든다.
  조회 결과 타입은 `service` 에 두므로, 이때만 `repository → service` 의존을 허용한다. (예: `ArtifactRepository.findAllSummaries()`)
  - `JpaRepository` 기본 메서드와 헷갈리지 않도록 메서드 이름에 결과 종류를 넣는다. (`findById` 가 아니라 `findSummaryById`)
- 단건 상세는 서비스가 Repository 로 엔티티를 읽어 트랜잭션 안에서 조회 결과(`XxxDetail`)로 변환한다.
  `open-in-view` 가 꺼져 있어 트랜잭션 밖에서는 지연 로딩이 실패하기 때문이다.
- `controller` 는 `entity` 를 응답으로 직접 노출하지 않는다. `dto/response` 로 변환한다.
- `exception` 은 어느 패키지에서든 쓸 수 있다.

## 패키지별 책임

| 패키지 | 둔다 | 두지 않는다 |
| --- | --- | --- |
| entity | 비즈니스 규칙, 상태 변경, 검증 | 트랜잭션, HTTP, 외부 API 호출 |
| service | 유스케이스 흐름 조율, 트랜잭션, 권한 확인 | 비즈니스 판단 (`if` 로 도메인 규칙 판단) |
| repository | DB 접근 | 비즈니스 규칙 |
| global/external | 외부 시스템 연동 구현 | 비즈니스 규칙 |
| controller, dto | 요청 검증(형식), DTO 변환, 응답 코드 | 비즈니스 로직 |

## 전술 패턴

### Aggregate

- 외부에서는 루트를 통해서만 내부 객체를 변경한다.
- 다른 Aggregate 는 객체가 아니라 ID 로 참조한다.
- 한 트랜잭션에서 하나의 Aggregate 만 수정한다. 여러 개가 필요하면 도메인 이벤트로 나눈다.
- Aggregate 는 작게 유지한다. 함께 일관성을 지켜야 하는 것만 묶는다.

### 엔티티

- 도메인 모델과 JPA 엔티티는 같은 클래스로 쓴다. 도메인 클래스에 `@Entity` 를 붙인다.
- JPA 용 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 로 만든다. 엔티티와 `@Embeddable` 클래스 모두 같다.
  JPA 만 쓰도록 `protected` 로 막고, 객체는 검증을 거치는 생성자로만 만든다.
- setter 를 두지 않는다. 상태 변경은 의도를 드러내는 메서드로. (`changeStatus()` 가 아니라 `depart()`)
- getter 는 클래스에 Lombok `@Getter` 를 붙여 JavaBean 형식(`getXxx()`)으로 만든다. 엔티티와 `@Embeddable` 클래스 모두 같다.
  응답 변환 같은 읽기에만 쓰고, 판단은 엔티티 메서드로 한다.
- 새 엔티티는 처음부터 클래스 단위 `@Getter` 를 붙인다. 아직 없는 기존 엔티티는 그 엔티티를 변경할 때 맞춘다.
- 그대로 내보내면 안 되는 필드는 같은 이름의 getter 를 직접 만든다. Lombok 은 이미 있는 메서드를 만들지 않는다.
  필드에 직접 만든 이유를 주석으로 남긴다.
  - 컬렉션: 수정할 수 없는 뷰를 돌려준다. (`CampaignTarget.getHwVersions()`)
  - Hibernate 가 null 로 채울 수 있는 `@Embedded`: 기본값으로 바꿔 돌려준다. (`CampaignCondition.getCurrentVersionRange()`)
- 밖으로 내보낼 이유가 없는 민감한 필드(토큰 해시 등)는 `@Getter(AccessLevel.NONE)` 으로 막는다.
- `@Setter`, `@Data`, `@ToString`, `@EqualsAndHashCode` 는 엔티티에 쓰지 않는다.
  지연 로딩·순환 참조로 예외가 나거나, 영속 상태에 따라 동등성이 바뀌기 때문이다.

### 값 객체

- 불변. `record` 또는 `@Embeddable` 로 만든다.
- 생성 시점에 검증한다. 유효하지 않은 값 객체는 존재할 수 없다.

### Repository

- Aggregate 루트 단위로만 만든다. 이름은 `XxxRepository` 로 하고 `JpaRepository<Xxx, ID>` 를 바로 상속한다. 서비스는 이 인터페이스를 주입받는다.
- 별도의 포트 인터페이스와 구현(`XxxJpaRepository`)으로 나누지 않는다.
  `JpaRepository` 의 삭제·일괄 저장 메서드도 노출되지만, 도메인 규칙상 지우지 않는 Aggregate 에서는 쓰지 않는다. 리뷰에서 확인한다.
- `@Immutable` 뷰처럼 쓰기가 없는 대상은 쓰기 메서드가 없는 `Repository<Xxx, ID>` 를 상속한다. (`VehicleStatusViewRepository`)
- 값 객체를 컬럼 값으로 풀어야 하는 쿼리는 `default` 메서드로 감싸고, 서비스는 `default` 메서드를 쓴다. (`UpdateResultRepository.record`)
- 서비스 단위 테스트는 Repository 를 Mockito 로 대체한다. 쿼리 자체는 Testcontainers 를 쓰는 Repository 테스트로 확인한다.

### 도메인 서비스

- 한 엔티티에 넣기 어색한, 여러 도메인 객체에 걸친 규칙만 둔다.
- 상태를 갖지 않는다. Spring 에 의존하지 않는다.
- `service` 에 두되 `Service` 접미사를 쓰지 않는다. 역할로 이름 짓는다. (`DispatchPolicy`, `RouteCalculator`)
  `XxxService` 는 유스케이스를 조율하는 서비스만 쓴다. 이름만으로 둘을 구분하기 위해서다.

### 도메인 이벤트

- 과거형으로 이름 짓는다. (`VehicleDeparted`)
- `entity` 에 정의하고, 발행·구독 처리는 `service` 에서 한다.

## 컨텍스트 간 통신

- 다른 컨텍스트의 `entity`, `repository` 를 직접 참조하지 않는다.
- 동기 호출은 상대 컨텍스트의 `service` 를 통한다.
- 즉시 결과가 필요 없으면 도메인 이벤트로 비동기 처리한다.
- 다른 컨텍스트의 Aggregate 는 ID 로만 참조한다.
