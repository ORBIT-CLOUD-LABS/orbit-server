# Java 테스트

도구: JUnit 5, AssertJ, Mockito. DB 처럼 실제 외부 시스템이 필요하면 Testcontainers.

## 무엇을 어떻게 테스트하나

| 대상 | 방법 |
| --- | --- |
| 도메인 객체, 값 객체 | mock 없이 입력과 결과를 비교하는 단위 테스트 |
| 유스케이스 서비스 | 협력 객체(Repository, 외부 연동)를 Mockito 로 대체한 단위 테스트 |
| DB 쿼리 | Testcontainers 로 운영과 같은 DB 를 띄워 확인. H2 같은 다른 DB 로 대신하지 않는다 (방언·제약 차이) |
| API 경계 | 프레임워크의 슬라이스 테스트 (Spring 이면 `@WebMvcTest`) 로 요청·응답 형식 확인 |

## 원칙

- 공개된 동작을 검사한다. private 메서드나 호출 순서처럼 구현을 바꾸면 깨지는 것을 검사하지 않는다.
- mock 은 경계(Repository, 외부 연동)에만 쓴다. 도메인 객체와 값 객체는 mock 하지 않고 실제 객체를 쓴다.
- 본문은 given / when / then 으로 나눈다.
- 테스트 이름은 `@DisplayName` 에 한국어로, 사용하는 쪽 입장의 동작을 쓴다.
- 단언은 AssertJ(`assertThat`, `assertThatThrownBy`)로 쓴다.
- 경계값과 여러 입력은 `@ParameterizedTest` 로 묶는다.
- 현재 시각은 `Clock` 을 주입받아 쓰고 테스트에서 `Clock.fixed` 로 고정한다. 코드에서 `LocalDateTime.now()` 를 직접 부르지 않는다.
- 비동기 결과를 `Thread.sleep` 으로 기다리지 않는다. 필요하면 Awaitility 로 조건을 기다린다.
- 테스트끼리 상태를 공유하거나 실행 순서에 기대지 않는다.
- 버그 수정은 그 버그를 재현하는 테스트부터 쓴다.

## 예시

```java
class MoneyTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, -1000})
    @DisplayName("음수 금액은 만들 수 없다")
    void rejectsNegativeAmount(int amount) {
        assertThatThrownBy(() -> new Money(amount)).isInstanceOf(IllegalArgumentException.class);
    }
}

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("결제 후 24시간이 지난 주문은 취소할 수 없다")
    void rejectsCancelAfterLimit() {
        // given
        Order order = OrderFixture.paidAt(Instant.parse("2025-12-31T00:00:00Z"));
        given(orderRepository.getById(1L)).willReturn(order);
        OrderCancelService service = new OrderCancelService(orderRepository, NOW);

        // when, then
        assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(OrderNotCancelableException.class);
    }
}
```
