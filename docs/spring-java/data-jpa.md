# JPA · 트랜잭션

Spring Data JPA(Hibernate) 를 쓰는 프로젝트의 규칙이다.

## 엔티티

- 도메인 모델과 JPA 엔티티는 같은 클래스로 쓴다. 매핑 전용 클래스를 따로 두지 않는다.
- JPA 용 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 로 만든다. 엔티티와 `@Embeddable` 클래스 모두 같다.
  JPA 만 쓰도록 `protected` 로 막고, 객체는 검증을 거치는 생성자로만 만든다.
- setter 를 두지 않는다. 상태는 의도를 드러내는 메서드로 바꾼다. (`changeStatus()` 가 아니라 `cancel()`)
- getter 는 응답 변환 같은 읽기에만 쓰고, 판단은 엔티티 메서드로 한다. 클래스에 Lombok `@Getter` 를 붙여 `getXxx()` 로 만든다. 엔티티와 `@Embeddable` 클래스 모두 같다.
  - 새 엔티티는 처음부터 클래스 단위 `@Getter` 를 붙인다. 아직 없는 기존 엔티티는 그 엔티티를 변경할 때 맞춘다.
  - 그대로 내보내면 안 되는 필드는 같은 이름의 getter 를 직접 만든다. Lombok 은 이미 있는 메서드를 만들지 않는다. 필드에 직접 만든 이유를 주석으로 남긴다.
    컬렉션은 수정할 수 없는 뷰를 돌려준다. (`CampaignTarget.getHwVersions()`)
    Hibernate 가 null 로 채울 수 있는 `@Embedded` 는 기본값으로 바꿔 돌려준다. (`CampaignCondition.getCurrentVersionRange()`)
  - 밖으로 내보낼 이유가 없는 민감한 필드(토큰 해시 등)는 `@Getter(AccessLevel.NONE)` 으로 막는다.
- 엔티티에 `@Setter`, `@Data`, `@ToString`, `@EqualsAndHashCode` 를 쓰지 않는다. 지연 로딩 필드를 건드리거나 순환 참조로 터진다.
- enum 컬럼은 `@Enumerated(EnumType.STRING)` 으로 저장한다. `ORDINAL` 은 상수 순서를 바꾸면 데이터가 틀어진다.
- 값 객체는 `@Embeddable` 로 매핑한다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    public void cancel() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_CANCELABLE);
        }
        status = OrderStatus.CANCELED;
    }
}
```

## 연관관계

- 모든 연관관계를 지연 로딩(`fetch = FetchType.LAZY`)으로 둔다. `@ManyToOne`, `@OneToOne` 은 기본값이 즉시 로딩이라 직접 적는다.
- 양방향 연관관계는 반대쪽에서 탐색할 이유가 있을 때만 만든다.
- `cascade`, `orphanRemoval` 은 같은 Aggregate 안의 부모·자식 사이에만 쓴다.

## N+1 과 조회

- 목록을 읽은 뒤 연관 엔티티를 쓰면 행마다 쿼리가 나간다(N+1). fetch join(`join fetch`)이나 `@EntityGraph` 로 함께 읽는다.
- 컬렉션을 fetch join 하면서 페이징하면 Hibernate 가 메모리에서 페이징한다. 컬렉션은 `default_batch_fetch_size` 로 묶어 읽는다.
- 목록·검색·통계처럼 일부 컬럼만 필요한 조회는 엔티티 대신 DTO(record) projection 으로 읽는다.
- 단건 상세는 엔티티를 읽어 트랜잭션 안에서 응답 DTO 로 바꾼다.
- 개발 환경에서는 실행 SQL 을 로그로 보며 N+1 을 확인한다. (`logging.level.org.hibernate.SQL: debug`)

```yaml
spring:
  jpa:
    open-in-view: false
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

## Repository

- Aggregate 루트마다 하나 만든다. `JpaRepository<Xxx, ID>` 를 바로 상속하고 서비스가 주입받는다. 포트 인터페이스와 구현으로 나누지 않는다.
  - `JpaRepository` 의 삭제·일괄 저장 메서드도 노출되지만, 도메인 규칙상 지우지 않는 Aggregate 에서는 쓰지 않는다. 리뷰에서 확인한다.
  - `@Immutable` 뷰처럼 쓰기가 없는 대상은 쓰기 메서드가 없는 `Repository<Xxx, ID>` 를 상속한다. (`VehicleStatusViewRepository`)
- 값 객체를 컬럼 값으로 풀어야 하는 쿼리는 `default` 메서드로 감싸고, 서비스는 `default` 메서드를 쓴다. (`UpdateResultRepository.record`)
- 목록 조회의 projection 은 조회 결과 타입을 `service` 에 두고 `@Query` 의 JPQL 생성자 표현식으로 만든다. → [architecture.md](architecture.md) 의 의존 방향
- 메서드 이름 쿼리가 길어지면 `@Query` 로 쓴다.
- 없을 때의 처리는 서비스에서 기능 예외로 바꾼다. (`findById(id).orElseThrow(() -> new BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND))`)
  예외 클래스를 경우마다 만들지 않고 `ErrorCode` 로 구분한다. → [api.md](api.md) 의 오류 응답

## 트랜잭션

- `@Transactional` 은 service 의 public 메서드에 둔다. 컨트롤러와 repository 에 두지 않는다.
- 조회만 하는 메서드는 `@Transactional(readOnly = true)` 로 둔다.
- 수정은 엔티티 메서드로 상태를 바꾸고 변경 감지에 맡긴다. 수정 뒤 `save()` 를 다시 부르지 않는다.
- 트랜잭션 안에서 외부 API 호출·메시지 발행을 하지 않는다. DB 커넥션을 오래 잡고, 롤백돼도 외부 호출은 되돌릴 수 없다.
  커밋 뒤에 할 일은 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 로 처리한다.
- 같은 클래스 안에서 `this.method()` 로 부르면 `@Transactional` 이 적용되지 않는다. (프록시를 거치지 않는다)
- checked 예외는 기본으로 롤백하지 않는다. 기능 예외는 unchecked 로 만든다.

## open-in-view

- `spring.jpa.open-in-view` 를 `false` 로 둔다. 기본값(`true`)은 응답을 다 쓸 때까지 DB 커넥션을 잡고, 컨트롤러에서 지연 로딩이 일어나 쿼리 위치를 알기 어렵게 만든다.

## 스키마

- 스키마 변경은 Flyway(또는 Liquibase) 마이그레이션 파일로 한다.
- `spring.jpa.hibernate.ddl-auto` 는 운영에서 `validate` 나 `none` 이다. `create`, `update` 를 쓰지 않는다.
- 이미 적용된 마이그레이션 파일은 고치지 않는다. 바꿀 내용은 새 파일로 추가한다.

### 배포 중 호환 (orbit)

OTA Main·Sub 클러스터는 같은 RDS 를 쓰고, 서버는 시작할 때 Flyway 로 스키마를 바꾼 뒤 `ddl-auto: validate` 로 엔티티와 맞는지 확인한다.
롤링 배포 중, Main·Sub 가 차례로 새 버전으로 바뀌는 동안, Git revert 로 이전 버전으로 되돌린 뒤에는 **직전 버전 코드가 새 스키마 위에서 돈다.**
그래서 마이그레이션은 아래 규칙을 지킨다.

1. 새 스키마에서 직전 버전 코드가 그대로 돌아야 한다.
   - 테이블·nullable 컬럼·인덱스·뷰 추가는 바로 한다.
   - 컬럼·테이블 삭제, 이름 변경, 타입 축소, NOT NULL·CHECK·UNIQUE 추가처럼 직전 코드가 읽거나 쓰다 실패하는 변경은 한 번에 하지 않는다 (2번 규칙).
   - 직전 코드의 엔티티가 매핑한 컬럼이 사라지면 `validate` 에서 서버가 뜨지 않는다. 남은 Pod 와 롤백한 Pod 가 모두 실패한다.
