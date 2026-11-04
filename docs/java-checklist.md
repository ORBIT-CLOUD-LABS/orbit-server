# 자바 클린 코드 체크리스트

Java 코드 작성·리뷰 시 확인한다. [clean-code.md](clean-code.md) 와 겹치면 이 문서가 우선한다.

| # | 체크 항목 |
| --- | --- |
| 1 | 자바 코드 컨벤션을 지켰는가 |
| 2 | 메서드 들여쓰기가 1단계인가 |
| 3 | `else` 를 쓰지 않았는가 |
| 4 | 원시값과 문자열을 포장했는가 |
| 5 | 컬렉션에 일급 컬렉션을 적용했는가 |
| 6 | 인스턴스 변수가 2개 이하인가 (서비스의 주입 필드는 5개 이하) |
| 7 | 도메인 객체에 getter/setter 가 없는가 |
| 8 | 메서드 인자가 3개 이하인가 |
| 9 | 한 줄에 점(`.`)이 하나인가 |
| 10 | 메서드가 한 가지 일만 하는가 |
| 11 | 클래스가 작은가 |

## 1. 자바 코드 컨벤션

- [palantir-java-format](https://github.com/palantir/palantir-java-format) 스타일을 따른다.
- IntelliJ 에서 palantir-java-format 플러그인으로 포맷팅한다.
- 최종 판정은 `spotlessCheck`. → [lint-and-ci.md](lint-and-ci.md)

## 2. 들여쓰기 1단계

- 메서드 안의 들여쓰기는 1단계만 허용한다.
- 중첩이 생기면 안쪽 블록을 메서드로 추출한다.

## 3. else 금지

- `else` 를 쓰지 않는다. 조기 반환으로 바꾼다.

```java
// 나쁨
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

## 4. 원시값·문자열 포장

- 의미 있는 원시값과 문자열은 클래스로 포장한다.
- 검증 로직은 포장 클래스 생성자에 둔다.

```java
public class Money {
	private final int amount;

	public Money(int amount) {
		// 음수 금액은 도메인상 존재할 수 없으므로 생성 시점에 막는다.
		if (amount < 0) {
			throw new IllegalArgumentException("금액은 0 이상이어야 한다.");
		}
		this.amount = amount;
	}
}
```

## 5. 일급 컬렉션

- 컬렉션은 그 컬렉션만 필드로 가진 클래스로 감싼다.
- 컬렉션 관련 로직(검증, 합계, 필터)은 일급 컬렉션 안에 둔다.

```java
public class Lottos {
	private final List<Lotto> lottos;

	public Lottos(List<Lotto> lottos) {
		this.lottos = List.copyOf(lottos);
	}

	public int countWinning(WinningNumbers winning) { ... }
}
```

## 6. 인스턴스 변수 2개 이하

- 3개 이상의 인스턴스 변수를 가진 클래스를 만들지 않는다.
- 어렵다면 최소한 줄이려고 시도한다. 관련 있는 필드끼리 객체로 묶는다.
- 예외: Aggregate 루트와 엔티티는 ID·상태·연관 ID 때문에 2개를 넘을 수 있다.
  대신 관련 필드는 값 객체로 묶어 개수를 줄인다. → [architecture.md](architecture.md)
- 예외: 서비스·컴포넌트가 생성자로 주입받는 협력 객체 필드(Spring Bean)는 2개 규칙 대신 **5개 이하**를 기준으로 한다.
  유스케이스 서비스는 여러 협력 객체를 조율하는 것이 역할이라, 2개로 맞추면 이름 붙이기 어려운 중간 객체만 늘어나기 때문이다.
  - `Clock`, 설정 `Properties` 도 개수에 포함한다.
  - 5개를 넘으면 먼저 책임 분리를 검토한다. 유스케이스를 나누거나, 함께 쓰이고 이름이 자연스럽게 붙는 협력 객체끼리 묶는다.
  - 분리하지 않기로 했다면 PR 에 이유를 적는다.

## 7. getter/setter 금지

- 핵심 로직을 담은 도메인 객체에는 getter/setter 를 쓰지 않는다.
- 값을 꺼내 판단하지 않고 객체에 메시지를 보낸다.
- DTO 는 허용한다.

## 8. 메서드 인자 3개 이하

- 4개 이상의 인자는 허용하지 않는다.
- 3개도 줄일 수 있는지 먼저 검토한다.

## 9. 한 줄에 점 하나

- 디미터 법칙("친구하고만 대화하라")을 지킨다.
- `location.current.representation.substring(0, 1)` 처럼 점이 여러 개면 리팩터링 대상을 찾는다.

## 10. 메서드는 한 가지 일만

- 메서드 이름으로 하는 일이 다 설명되지 않으면 나눈다.

## 11. 작은 클래스

- 클래스를 작게 유지한다.
- 클래스가 커지면 책임을 나눌 수 있는지 먼저 본다.
