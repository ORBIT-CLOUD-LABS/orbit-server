# 자바 클린 코드 체크리스트

Java 코드 작성·리뷰 시 확인한다. [clean-code.md](clean-code.md) 와 겹치면 이 문서가 우선한다.

실무에서 흔히 쓰는 기준을 따른다. 숫자로 적힌 기준은 리뷰에서 먼저 볼 신호이지 기계적으로 막는 한도가 아니다.
기준을 넘기로 했다면 이유를 PR 에 적는다.

| # | 체크 항목 |
| --- | --- |
| 1 | 자바 코드 컨벤션을 지켰는가 |
| 2 | 중첩이 깊지 않은가 (조기 반환) |
| 3 | 의미와 검증이 있는 값을 값 객체로 만들었는가 |
| 4 | 규칙이 붙는 컬렉션을 일급 컬렉션으로 감쌌는가 |
| 5 | 주입받는 협력 객체가 많지 않은가 (5개 이하) |
| 6 | setter 없이 의미 있는 메서드로 상태를 바꾸는가 (getter 는 읽기에만) |
| 7 | 메서드 인자가 많지 않은가 (3개 이하 권장) |
| 8 | 객체 내부를 연달아 탐색하지 않는가 (디미터 법칙) |
| 9 | 메서드가 한 가지 일만 하는가 |
| 10 | 클래스가 작은가 |
| 11 | Java 21 문법으로 더 간단히 쓸 수 있지 않은가 |

## 1. 자바 코드 컨벤션

- [palantir-java-format](https://github.com/palantir/palantir-java-format) 스타일을 따른다. Google Java Style 기반이고 들여쓰기 4칸, 줄 길이 120 이다.
- IntelliJ 는 palantir-java-format 플러그인으로 포맷팅한다. 설정 방법은 [lint-and-ci.md](lint-and-ci.md).
- 포맷은 손으로 맞추지 않는다. 최종 판정은 `spotlessCheck`.

## 2. 중첩은 얕게, 조기 반환

- 메서드 안의 들여쓰기는 2단계 이하를 기준으로 한다. 더 깊어지면 안쪽 블록을 메서드로 추출한다.
- 예외·검증 조건은 조기 반환(guard clause)으로 먼저 처리한다.
- `else` 는 허용한다. 다만 앞 분기가 `return`·`throw` 로 끝나면 `else` 없이 쓴다.

```java
// 나쁨: 앞 분기가 반환으로 끝나는데 else 로 한 단계 더 들어간다
if (score >= 60) {
    return PASS;
} else {
    return FAIL;
}

// 좋음
if (score >= 60) {
    return PASS;
}
return FAIL;
```

## 3. 값 객체

- 검증이나 도메인 의미가 있는 값은 값 객체로 만든다. (금액, 이메일, 식별자, 기간 등)
- 검증 로직은 생성자에 둔다. 유효하지 않은 값 객체는 존재할 수 없다.
- 불변으로 만든다. 다른 제약이 없으면 `record` 를 쓴다.
- 검증도 의미도 없는 단순 값(DTO 필드, 로그 메시지 등)까지 감싸지는 않는다.

```java
public record Money(int amount) {

    public Money {
        // 음수 금액은 도메인상 존재할 수 없으므로 생성 시점에 막는다.
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 한다.");
        }
    }
}
```

## 4. 일급 컬렉션

- 컬렉션에 검증·합계·필터 같은 규칙이 붙으면 그 컬렉션만 필드로 가진 클래스로 감싸고 규칙을 그 안에 둔다.
- 규칙 없이 담아 전달만 하는 컬렉션은 `List`·`Set` 그대로 쓴다.
- 받은 컬렉션은 `List.copyOf` 로 복사해 밖에서 바꿀 수 없게 한다.

```java
public class Lottos {
    private final List<Lotto> lottos;

    public Lottos(List<Lotto> lottos) {
        this.lottos = List.copyOf(lottos);
    }

    public int countWinning(WinningNumbers winning) { ... }
}
```

## 5. 협력 객체 수

- 인스턴스 변수 개수는 제한하지 않는다. 관련 있는 필드는 값 객체로 묶는다.
- 생성자로 주입받는 협력 객체는 **5개 이하**를 기준으로 한다. `Clock`, 설정 객체도 개수에 포함한다.
- 5개를 넘으면 먼저 책임 분리를 검토한다. 유스케이스를 나누거나, 함께 쓰이고 이름이 자연스럽게 붙는 협력 객체끼리 묶는다.
- 분리하지 않기로 했다면 PR 에 이유를 적는다.

## 6. getter 는 읽기에만, setter 금지

- 도메인 객체에 setter 를 두지 않는다. 상태는 의도를 드러내는 메서드로 바꾼다. (`setStatus(CANCELED)` 가 아니라 `cancel()`)
- getter 는 응답 변환 같은 읽기에 쓴다. 값을 꺼내 판단하지 않고 객체에 시킨다. (Tell, Don't Ask)
- 컬렉션 getter 는 밖에서 바꿀 수 없는 뷰나 복사본을 돌려준다.
- DTO 와 record 는 값을 담아 나르는 것이 역할이므로 제한하지 않는다.

```java
// 나쁨: 값을 꺼내 판단한다
if (order.getStatus() == OrderStatus.PAID && order.getPaidAt().isAfter(limit)) { ... }

// 좋음: 객체에 시킨다
if (order.isCancelableAt(now)) { ... }

// 허용: 응답 변환을 위한 읽기
OrderResponse.from(order);
```

## 7. 메서드 인자

- 3개 이하를 권장한다.
- 4개 이상이면 함께 다니는 인자를 파라미터 객체(record)로 묶을 수 있는지 먼저 검토한다.
- 생성자·정적 팩토리처럼 값을 모으는 것이 역할인 메서드는 예외로 본다.

## 8. 디미터 법칙

- 객체 내부를 연달아 탐색하지 않는다. `order.getCustomer().getAddress().getCity()` 는 필요한 것을 객체에 묻도록 바꾼다.
- 같은 객체를 다루는 체인(stream, builder, `Optional`, 테스트 assertion)은 허용한다.
- record·DTO 의 값을 읽는 것은 탐색이 아니므로 허용한다.

## 9. 메서드는 한 가지 일만

- 메서드 이름으로 하는 일이 다 설명되지 않으면 나눈다.

## 10. 작은 클래스

- 클래스를 작게 유지한다.
- 클래스가 커지면 책임을 나눌 수 있는지 먼저 본다.

## 11. Java 21 문법

- 값만 담는 클래스는 `record` 로 만든다. 생성자·`equals`·`hashCode`·`toString` 을 손으로 쓰지 않는다.
- 분기는 `switch` 식(`->`)으로 쓴다. enum·sealed 타입을 분기하면 `default` 를 두지 않는다. 새 경우가 생기면 컴파일러가 빠진 곳을 알려 준다.
- 경우가 정해진 타입 계층은 `sealed interface` 와 `record` 로 만들고 패턴 매칭으로 분기한다.
- `instanceof` 뒤에 캐스팅하지 않고 패턴 매칭(`if (payment instanceof CardPayment card)`)을 쓴다.
- 여러 줄 문자열(SQL, JSON)은 텍스트 블록(`"""`)으로 쓴다.
- 불변 컬렉션은 `List.of`, `List.copyOf`, `stream().toList()` 로 만든다.
- `var` 는 오른쪽에서 타입이 바로 보일 때만 쓴다. (`var orders = new ArrayList<Order>()`) 메서드 반환값처럼 타입이 안 보이면 타입을 적는다.

```java
sealed interface Payment permits CardPayment, BankTransfer {}

record CardPayment(String cardNumber, int amount) implements Payment {}

record BankTransfer(String accountNumber, int amount) implements Payment {}

int fee(Payment payment) {
    // sealed 타입이라 default 가 없어도 모든 경우를 다룬다. 새 결제 수단을 추가하면 여기서 컴파일 오류가 난다.
    return switch (payment) {
        case CardPayment card -> card.amount() * 3 / 100;
        case BankTransfer transfer -> 500;
    };
}
```
