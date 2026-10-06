# 아키텍처 (DDD)

도메인 구조는 DDD 를 따른다. 코드 레벨 규칙은 [clean-code.md](clean-code.md), [java-checklist.md](java-checklist.md).

## 패키지 구조

최상위는 `domain`(비즈니스)과 `global`(전역 기반)로 나눈다.
`domain` 아래는 바운디드 컨텍스트로, 각 컨텍스트 안은 레이어로 나눈다.

```
com.orbit.server
├── domain/                     # 바운디드 컨텍스트 모음
│   ├── vehicle/                # 바운디드 컨텍스트 (예시)
│   │   ├── model/              # 엔티티, 값 객체, 도메인 서비스, Repository 인터페이스, 도메인 이벤트
│   │   ├── application/        # 유스케이스(트랜잭션 경계), 커맨드/결과 DTO
│   │   ├── infrastructure/     # Repository 구현, 외부 연동 어댑터
│   │   └── presentation/       # Controller, 요청/응답 DTO
│   └── telemetry/
└── global/                     # 도메인과 무관한 전역 설정·공통 기반
    ├── config/                 # Spring 설정 (JPA, Web, Swagger 등)
    ├── error/                  # 공통 예외 계층, 전역 예외 핸들러, 에러 응답
    ├── security/               # 인증·인가
    └── common/                 # BaseTimeEntity 등 공통 상위 타입
```

- 컨텍스트 이름은 예시다. 실제 이름은 [요구사항](requirements.md)의 용어(유비쿼터스 언어)를 따른다.
- 새 컨텍스트를 만들기 전에 기존 컨텍스트에 속하는지 먼저 검토한다.
- 컨텍스트 안의 도메인 레이어는 `domain` 이 아니라 `model` 로 부른다.
  최상위 `domain` 과 겹쳐 `domain.vehicle.domain` 처럼 헷갈리는 것을 막기 위해서다.

### global 규칙

- 비즈니스 규칙을 두지 않는다.
- `global` 은 `domain` 패키지에 의존하지 않는다. (`domain → global` 방향만 허용)
- 두 컨텍스트가 같은 개념을 쓴다고 `global` 로 올리지 않는다. 컨텍스트 경계를 먼저 다시 검토한다.

## 의존 방향

```
presentation → application → model ← infrastructure
```

- model 은 다른 레이어를 모른다. Spring 에 의존하지 않는다. (JPA 매핑 어노테이션만 예외, 아래 참고)
- infrastructure 는 model 의 인터페이스(Repository 등)를 구현한다.
- presentation 은 model 객체를 응답으로 직접 노출하지 않는다. 응답 DTO 로 변환한다.

## 레이어별 책임

| 레이어 | 둔다 | 두지 않는다 |
| --- | --- | --- |
| model | 비즈니스 규칙, 상태 변경, 검증 | 트랜잭션, HTTP, 외부 API 호출 |
| application | 유스케이스 흐름 조율, 트랜잭션, 권한 확인 | 비즈니스 판단 (`if` 로 도메인 규칙 판단) |
| infrastructure | DB 접근, 외부 시스템 어댑터, 메시징 | 비즈니스 규칙 |
| presentation | 요청 검증(형식), DTO 변환, 응답 코드 | 비즈니스 로직 |

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
- 인터페이스는 `model`, 구현은 `infrastructure` 에 둔다.

### 도메인 서비스

- 한 엔티티에 넣기 어색한, 여러 도메인 객체에 걸친 규칙만 둔다.
- 상태를 갖지 않는다.
- `Service` 접미사를 쓰지 않는다. 역할로 이름 짓는다. (`DispatchPolicy`, `RouteCalculator`)
  `XxxService` 는 `application` 의 애플리케이션 서비스만 쓴다. 이름만으로 레이어를 구분하기 위해서다.

### 도메인 이벤트

- 과거형으로 이름 짓는다. (`VehicleDeparted`)
- `model` 에 정의하고, 발행·구독 처리는 `application` 또는 `infrastructure` 에서 한다.

## 컨텍스트 간 통신

- 다른 컨텍스트의 `model`, `infrastructure` 를 직접 참조하지 않는다.
- 동기 호출은 상대 컨텍스트의 `application` 인터페이스를 통한다.
- 즉시 결과가 필요 없으면 도메인 이벤트로 비동기 처리한다.
- 다른 컨텍스트의 Aggregate 는 ID 로만 참조한다.
