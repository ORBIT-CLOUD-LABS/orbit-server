# 도메인 설계 (DDD)

orbit 의 도메인 구조는 DDD 를 따른다. [spring-java/architecture.md](spring-java/architecture.md) 의 "기능" 하나가 바운디드 컨텍스트 하나다.
패키지 구조·의존 방향·레이어 책임은 [spring-java/architecture.md](spring-java/architecture.md),
엔티티·Repository 는 [spring-java/data-jpa.md](spring-java/data-jpa.md) 에 있다. 이 문서는 그 문서들에 없는 DDD 설계 규칙만 둔다.

## Shared Kernel (`domain/shared`)

여러 컨텍스트가 **같은 의미와 같은 규칙**으로 쓰는 도메인 개념을 둔다. (예: 점으로 나눈 숫자 버전 `DottedVersion`)
비즈니스 규칙이라 `global` 에는 둘 수 없고, 한 컨텍스트의 `domain` 에 두면 다른 컨텍스트가 참조할 수 없기 때문이다.

- 상태 없는 값 객체만 둔다. 엔티티·Repository·서비스는 두지 않는다.
- 하위 패키지를 만들지 않는다. 바운디드 컨텍스트가 아니므로 레이어로 나누지 않는다.
- 다른 컨텍스트에 의존하지 않는다. `global` 에만 의존할 수 있다.
- 모든 컨텍스트의 `domain` 을 포함해 어느 레이어에서나 쓸 수 있다.
- 바꾸면 모든 컨텍스트에 영향을 주므로 작게 유지한다. 두 컨텍스트가 비슷한 개념을 쓴다는 이유만으로 옮기지 않는다.
  이름이 같아도 컨텍스트마다 규칙(길이 제한 등)이 다르면 각 컨텍스트의 값 객체에 두고, 같은 부분만 Shared Kernel 을 쓴다.

## Aggregate

- 외부에서는 루트를 통해서만 내부 객체를 변경한다.
- 다른 Aggregate 는 객체가 아니라 ID 로 참조한다.
- 한 트랜잭션에서 하나의 Aggregate 만 수정한다. 여러 개가 필요하면 도메인 이벤트로 나눈다.
- Aggregate 는 작게 유지한다. 함께 일관성을 지켜야 하는 것만 묶는다.

## 도메인 서비스

- 한 엔티티에 넣기 어색한, 여러 도메인 객체에 걸친 규칙만 둔다.
- 상태를 갖지 않는다. Spring 에 의존하지 않는다.
- `service` 에 두되 `Service` 접미사를 쓰지 않는다. 역할로 이름 짓는다. (`DispatchPolicy`, `RouteCalculator`)
  `XxxService` 는 유스케이스를 조율하는 서비스만 쓴다. 이름만으로 둘을 구분하기 위해서다.

## 도메인 이벤트

- 과거형으로 이름 짓는다. (`VehicleDeparted`)
- `domain/event` 에 정의하고, 발행은 `service` 에서, 구독은 `service/listener` 에서 한다.

## 컨텍스트 간 통신

- 다른 컨텍스트의 `domain`, `repository` 를 직접 참조하지 않는다.
- 동기 호출은 상대 컨텍스트의 `service` 를 통한다.
- 즉시 결과가 필요 없으면 도메인 이벤트로 비동기 처리한다.
- 다른 컨텍스트의 Aggregate 는 ID 로만 참조한다.
