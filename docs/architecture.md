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
│   │   ├── repository/         # Repository 인터페이스, Spring Data 구현
│   │   ├── entity/             # 엔티티, 값 객체, enum, 도메인 이벤트
│   │   └── exception/          # 에러 코드, 커스텀 예외
│   └── campaign/
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

## 의존 방향

```
controller → service → repository → entity
             service → entity
global/external → service   (포트 구현)
```

- `entity` 는 다른 패키지를 모른다. Spring 에 의존하지 않는다. (JPA 매핑 어노테이션만 예외, 아래 참고)
- `service` 는 외부 시스템을 포트(인터페이스)로만 쓴다. 구현은 `global/external` 에 둔다.
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
- JPA 용 기본 생성자는 `protected` 로 막는다.
- setter 를 두지 않는다. 상태 변경은 의도를 드러내는 메서드로. (`changeStatus()` 가 아니라 `depart()`)

### 값 객체

- 불변. `record` 또는 `@Embeddable` 로 만든다.
- 생성 시점에 검증한다. 유효하지 않은 값 객체는 존재할 수 없다.

### Repository

- Aggregate 루트 단위로만 만든다.
- 인터페이스와 Spring Data 구현을 `repository` 에 함께 둔다.
  구현은 package-private 으로 숨겨 `JpaRepository` 의 삭제·일괄 저장 같은 메서드를 밖에서 호출하지 못하게 한다.

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
