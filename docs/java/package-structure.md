# 패키지 구조

패키지 하나에 클래스가 쌓이면 어디에 무엇이 있는지 찾기 어려워지고, 역할이 다른 코드가 한 곳에 섞인다.
`src/test/java/PackageSizeTest.java` 가 패키지당 최상위 클래스 수를 검사해서, 상한(기본 15개)을 넘으면 테스트를 실패시키고 이 문서로 안내한다.

## 의존성 추가

`PackageSizeTest` 는 ArchUnit 으로 컴파일된 클래스를 읽는다. 이미 ArchUnit 이 있는 프로젝트는 이 단계를 건너뛴다.
버전은 이 문서를 쓴 시점의 최신 안정판(1.5.1)이다. 추가하기 전에 새 버전이 있는지 확인한다.

Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
}
```

Groovy DSL (`build.gradle`):

```groovy
dependencies {
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.5.1'
}
```

JUnit 5 를 쓰도록 `tasks.test { useJUnitPlatform() }` (Kotlin) 또는 `test { useJUnitPlatform() }` (Groovy)도 켜져 있어야 한다.

## 검사 방식과 한계

- 루트 패키지를 몰라도 되도록 테스트를 기본 패키지에 두고, 테스트 클래스와 jar 를 뺀 프로젝트의 main 클래스만 읽는다.
- 바이트코드 기준이라 **컴파일된 main 클래스만** 센다. 소스에만 있고 컴파일되지 않은 파일은 세지 않는다.
- 최상위 클래스만 센다. 중첩·익명·합성 클래스와 `package-info` 는 파일 수에 가깝게 세려고 뺀다. 인터페이스·enum·record 는 센다.
- 하위 패키지는 따로 센다. `a.b` 가 15개 이하여도 `a.b.c` 와 합산하지 않는다.

## 패키지가 커졌을 때 나누는 순서

크기 초과 메시지의 역할 분포(`config 3, service 7, record 13, 기타 6`)를 보고 위에서부터 판단한다.

1. **역할이 섞여 있으면 역할 단위 하위 패키지로 나눈다.** 예: `domain/event/`, `application/listener/`, `application/config/`.
   분포가 한 역할로 몰려 있지 않고 두 가지 이상이 크게 섞여 있으면 이 단계다.
2. **한 역할 안에서도 크면 기능 단위 하위 패키지로 나눈다.** 예: `service/` 안의 서비스가 모두 서비스인데 15개를 넘으면 `service/alert/`, `service/report/` 처럼 기능으로 묶는다.
3. **컨텍스트 경계 자체가 의심되면 사람에게 묻는다.** 서로 다른 기능이 한 패키지에 들어와 있어 나누기보다 모듈·기능 패키지를 새로 만들어야 하는 경우다. 혼자 정하지 않는다.

역할 분류는 위에서부터 먼저 맞는 하나다. 어노테이션은 패키지를 뺀 단순 이름이 정확히 같은지로 비교한다.

| 순서 | 역할 | 기준 |
| --- | --- | --- |
| 1 | config | `@Configuration`, `@ConfigurationProperties` |
| 2 | controller | `@RestController`, `@Controller` |
| 3 | service | `@Service`, `@Component` |
| 4 | repository | `@Repository`, 또는 `org.springframework.data.repository.Repository` 를 상속 |
| 5 | entity | `@Entity`, `@Embeddable` |
| 6 | record | `record` |
| 7 | 기타 | 위에 해당하지 않는 것 |

## 재구성 작업 방식

크기 신호가 오면 AI 가 스스로 판단해서 재구성한다. 다음 순서를 지킨다.

1. 진행 중인 기능 변경과 섞지 않는다. **파일 이동만 하는 별도 커밋**으로 재구성한다.
2. 같은 커밋에서 패키지 구조를 설명하는 문서를 함께 고친다.
3. `agentsmd verify` 가 통과한 뒤에 원래 작업을 이어 간다.

## 이미 큰 패키지가 있을 때: KNOWN_LARGE_PACKAGES

테스트를 도입하는 시점에 이미 상한을 넘은 패키지가 있으면, 한꺼번에 고치지 않고 `PackageSizeTest` 의 `KNOWN_LARGE_PACKAGES` 에 패키지 전체 이름과 그 시점의 클래스 수를 적는다.

```java
private static final Map<String, Integer> KNOWN_LARGE_PACKAGES = Map.of(
        "com.x.monitoring.service", 28,
        "com.x.update.service", 17);
```

- **도입 시점에만 채운다.** 나중에 패키지가 커졌다고 항목을 더하지 않는다.
- **적힌 수를 올리지 않는다.** 적힌 수를 넘으면 테스트가 실패한다. 그때는 수를 고치지 말고 패키지를 나눈다. 상한을 올리는 변경은 PR diff 에 드러나므로 리뷰에서 걸러진다.
- **줄면 지운다.** 패키지가 기본 상한(15) 이하로 줄거나 사라지면 테스트가 `KNOWN_LARGE_PACKAGES 에서 지우세요` 로 실패한다. 항목을 지우면 통과한다. 적힌 수보다 줄어든 것만으로는 실패하지 않는다.
- 기본 상한(`MAX_CLASSES_PER_PACKAGE`)은 프로젝트에 맞게 바꿀 수 있다. 바꿨다면 이 문서의 15 와 `KNOWN_LARGE_PACKAGES` 항목을 함께 맞춘다.
