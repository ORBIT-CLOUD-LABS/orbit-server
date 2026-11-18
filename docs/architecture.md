# 아키텍처 (DDD)

패키지 구조, 의존 방향, 주입, DTO 경계는 [spring-java/architecture.md](spring-java/architecture.md),
엔티티·Repository·트랜잭션은 [spring-java/data-jpa.md](spring-java/data-jpa.md) 를 기본으로 따른다.
이 문서에는 orbit 이 그 위에 더한 것만 적는다. 도메인 구조는 DDD 를 따르고, 원본의 "기능" 은 orbit 에서 바운디드 컨텍스트다.
원본과 겹치면 이 문서가 우선한다.

## 패키지 구조

```
com.orbit.server
├── domain/                     # 바운디드 컨텍스트 모음
│   ├── artifact/               # 바운디드 컨텍스트
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/        # API 요청 DTO
│   │   │   └── response/       # API 응답 DTO
│   │   ├── service/            # 서비스(트랜잭션 경계), Command/Result·조회 결과, 외부 연동 포트, 도메인 서비스
│   │   ├── repository/
│   │   ├── domain/             # 엔티티, 값 객체, enum, 도메인 이벤트
│   │   └── exception/          # 에러 코드, 커스텀 예외
│   ├── campaign/
│   └── shared/                 # Shared Kernel: 여러 컨텍스트가 같은 의미로 쓰는 값 객체 (하위 패키지 없음)
└── global/                     # 도메인과 무관한 전역 설정·공통 기반
    ├── config/                 # Spring 설정 (JPA, Web, Swagger 등)
    ├── error/                  # 공통 예외 계층, 전역 예외 핸들러, 에러 응답
    ├── security/               # 인증·인가
    ├── common/                 # 공통 응답(ApiResponse, SuccessCode)과 공통 검증
    └── external/               # 외부 시스템 연동 구현 (시스템별 하위 패키지)
        └── origin/             # Origin 저장소 (AWS S3, 로컬은 S3 호환 SeaweedFS)
```

- 컨텍스트 안의 `domain` 은 최상위 `domain` 과 이름이 같다. 경로는 `com.orbit.server.domain.<컨텍스트>.domain` 이 된다.
  이 문서에서 계층을 말할 때 `domain` 은 컨텍스트 안의 `domain` 을 뜻한다.
- 컨텍스트 이름은 [요구사항](requirements.md)의 용어(유비쿼터스 언어)를 따른다.
- 새 컨텍스트를 만들기 전에 기존 컨텍스트에 속하는지 먼저 검토한다.
- 서비스의 입출력(`XxxCommand` / `XxxResult`, 조회 결과 `XxxSummary` / `XxxDetail`)은 `dto` 가 아니라 `service` 에 둔다.
  `dto` 는 API 계약(요청·응답)만 둔다.
- `global/external` 은 `service` 의 포트(예: `OriginStorage`)를 구현하므로, 그 포트(interface·`record`)와 `domain`·`exception` 에만 의존할 수 있다.
  유스케이스 서비스나 도메인 서비스를 직접 부르지 않는다.

### Shared Kernel (`domain/shared`)

여러 컨텍스트가 **같은 의미와 같은 규칙**으로 쓰는 도메인 개념을 둔다. (예: 점으로 나눈 숫자 버전 `DottedVersion`)
비즈니스 규칙이라 `global` 에는 둘 수 없고, 한 컨텍스트의 `domain` 에 두면 다른 컨텍스트가 참조할 수 없기 때문이다.

- 상태 없는 값 객체만 둔다. 엔티티·Repository·서비스는 두지 않는다.
- 하위 패키지를 만들지 않는다. 바운디드 컨텍스트가 아니므로 계층으로 나누지 않는다.
- 다른 컨텍스트에 의존하지 않는다. `global` 에만 의존할 수 있다.
- 모든 컨텍스트의 `domain` 을 포함해 어느 계층에서나 쓸 수 있다. (`domain` 이 다른 패키지를 모른다는 원본 규칙의 예외)
- 바꾸면 모든 컨텍스트에 영향을 주므로 작게 유지한다. 두 컨텍스트가 비슷한 개념을 쓴다는 이유만으로 옮기지 않는다.
  이름이 같아도 컨텍스트마다 규칙(길이 제한 등)이 다르면 각 컨텍스트의 값 객체에 두고, 같은 부분만 Shared Kernel 을 쓴다.

## 의존 방향

원본의 의존 방향에 아래만 더한다.

- 목록·검색·대시보드처럼 필요한 컬럼만 읽는 조회는 Repository 의 `@Query` 에서 JPQL 생성자 표현식으로 조회 결과(`XxxSummary` 등)를 바로 만든다.
  조회 결과 타입은 `service` 에 두므로, 이때만 `repository → service` 의존을 허용한다. (예: `ArtifactRepository.findAllSummaries()`)
  - `JpaRepository` 기본 메서드와 헷갈리지 않도록 메서드 이름에 결과 종류를 넣는다. (`findById` 가 아니라 `findSummaryById`)
- `exception` 은 어느 패키지에서든 쓸 수 있다.

## 패키지별 책임

| 패키지 | 둔다 | 두지 않는다 |
| --- | --- | --- |
| domain | 비즈니스 규칙, 상태 변경, 검증 | 트랜잭션, HTTP, 외부 API 호출 |
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

[spring-java/data-jpa.md](spring-java/data-jpa.md) 의 엔티티 규칙에 아래를 더한다.

- `protected` 기본 생성자와 클래스 단위 `@Getter` 는 `@Embeddable` 클래스에도 똑같이 적용한다.
- 새 엔티티는 처음부터 클래스 단위 `@Getter` 를 붙인다. 아직 없는 기존 엔티티는 그 엔티티를 변경할 때 맞춘다.
- 그대로 내보내면 안 되는 필드는 같은 이름의 getter 를 직접 만든다. Lombok 은 이미 있는 메서드를 만들지 않는다.
  필드에 직접 만든 이유를 주석으로 남긴다.
  - 컬렉션: 수정할 수 없는 뷰를 돌려준다. (`CampaignTarget.getHwVersions()`)
  - Hibernate 가 null 로 채울 수 있는 `@Embedded`: 기본값으로 바꿔 돌려준다. (`CampaignCondition.getCurrentVersionRange()`)
- 밖으로 내보낼 이유가 없는 민감한 필드(토큰 해시 등)는 `@Getter(AccessLevel.NONE)` 으로 막는다.

### Repository

[spring-java/data-jpa.md](spring-java/data-jpa.md) 의 Repository 규칙에 아래를 더한다.

- `JpaRepository` 의 삭제·일괄 저장 메서드도 노출되지만, 도메인 규칙상 지우지 않는 Aggregate 에서는 쓰지 않는다. 리뷰에서 확인한다.
- `@Immutable` 뷰처럼 쓰기가 없는 대상은 쓰기 메서드가 없는 `Repository<Xxx, ID>` 를 상속한다. (`VehicleStatusViewRepository`)
- 값 객체를 컬럼 값으로 풀어야 하는 쿼리는 `default` 메서드로 감싸고, 서비스는 `default` 메서드를 쓴다. (`UpdateResultRepository.record`)

### 도메인 서비스

- 한 엔티티에 넣기 어색한, 여러 도메인 객체에 걸친 규칙만 둔다.
- 상태를 갖지 않는다. Spring 에 의존하지 않는다.
- `service` 에 두되 `Service` 접미사를 쓰지 않는다. 역할로 이름 짓는다. (`DispatchPolicy`, `RouteCalculator`)
  `XxxService` 는 유스케이스를 조율하는 서비스만 쓴다. 이름만으로 둘을 구분하기 위해서다.

### 도메인 이벤트

- 과거형으로 이름 짓는다. (`VehicleDeparted`)
- `domain` 에 정의하고, 발행·구독 처리는 `service` 에서 한다.

## 컨텍스트 간 통신

- 다른 컨텍스트의 `domain`, `repository` 를 직접 참조하지 않는다.
- 동기 호출은 상대 컨텍스트의 `service` 를 통한다.
- 즉시 결과가 필요 없으면 도메인 이벤트로 비동기 처리한다.
- 다른 컨텍스트의 Aggregate 는 ID 로만 참조한다.

## 검사

원본 문서는 기본 패키지의 `LayerArchitectureTest`·`PackageSizeTest`·`CodingRulesTest`·`SpringCodingRulesTest` 가 규칙을 검사한다고 안내한다.
orbit 은 이 테스트들을 #136 에서 들여온다. 그 전까지 의존 규칙은 `com.orbit.server.ArchitectureTest` 가 검사한다.

- 컨텍스트 간 내부 참조 금지와 `global/external` 의 포트 규칙은 원본 테스트에 없는 orbit 규칙이라 orbit 테스트로 따로 검사한다.
- ArchUnit 기준선(`archunit_store`)과 순환 허용 목록은 늘리지 않는다. 넘으면 패키지를 나누고, 역방향 의존은 도메인 이벤트로 끊는다.
