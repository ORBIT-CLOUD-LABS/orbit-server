# 린트 · CI

## 린트 (Spotless + palantir-java-format)

orbit 은 `build.gradle`(Groovy DSL)에 아래처럼 설정되어 있다. 버전을 올릴 때는 이 문서도 함께 고친다.

palantir-java-format 은 Google Java Style 을 바탕으로 들여쓰기 4칸, 줄 길이 120 을 쓴다.

```groovy
plugins {
	id 'java'
	id 'com.diffplug.spotless' version '8.10.3'
}

java {
	toolchain {
		// 로컬에 설치된 JDK 와 상관없이 같은 버전으로 컴파일·테스트한다.
		languageVersion = JavaLanguageVersion.of(21)
	}
}

spotless {
	java {
		palantirJavaFormat('2.102.0')
		removeUnusedImports()
	}
}
```

| 명령 | 용도 |
| --- | --- |
| `./gradlew spotlessApply` | 포맷 적용 |
| `./gradlew spotlessCheck` | 포맷 검사 (작업 마무리, CI) |

작업을 마치기 전에 `./gradlew spotlessApply` 로 포맷을 맞추고 아래 검증 명령을 실행한다.

- Spotless 플러그인을 적용하면 `spotlessCheck` 이 `check` 에 연결되어, 포맷이 틀리면 `./gradlew build` 도 실패한다.
- JDK 버전은 위 toolchain 으로 고정한다. 버전을 바꾸면 toolchain 과 CI 의 JDK 를 함께 고친다.

## IntelliJ

IDE 포맷터는 편의용이다. 최종 판정은 `spotlessCheck` 이다.

1. Settings > Plugins > Marketplace 에서 `palantir-java-format` 을 설치하고 IDE 를 재시작한다.
2. Settings > Other Settings > palantir-java-format Settings 에서 Enable 을 체크한다. (프로젝트마다 한 번씩 켠다)
3. Settings > Build, Execution, Deployment > Build Tools > Gradle 의 Gradle JVM 을 toolchain 과 같은 버전으로 맞춘다.

## CI

orbit CI(`.github/workflows/ci.yml`)는 아래 명령을 실행한다. `spotlessCheck` 이 `check` 에 연결되어 있어 `build` 에서 함께 돈다.

```bash
./gradlew build
```

- CI 의 JDK 는 toolchain 과 같은 21 을 쓴다.
- `gradlew` 가 없는 저장소(지침만 있는 저장소)에서는 건너뛴다.
- 버전을 올릴 때는 이 문서의 스니펫과 CI 설정을 같이 고친다.

### Phase1 PR CI 결과 코멘트 (#66)

orbit 저장소의 GitHub Actions 설정이다.

- `CI` 는 `develop`, `phase1` 대상 PR 과 push 에서 실행한다.
- `CI result comment` 는 PR CI 완료 이벤트만 받아, 현재 head SHA 가 일치하는 열린 `phase1` PR 에 결과를 게시한다.
- 성공·실패 등의 결과, 검사한 PR head SHA, 해당 실행 시도 링크를 표시한다. CI 의 checkout 은 기본 설정대로 PR merge ref 를 사용한다.
- `orbit:phase1-ci-result` 마커가 있는 `github-actions[bot]` 코멘트 하나를 갱신한다. 오래된 SHA·실행·시도의 결과와 중복 이벤트는 무시한다.
- 게시 작업은 `actions: read`, `pull-requests: write` 만 사용한다. PR 코드·아티팩트·캐시를 실행하거나 읽지 않는다.
- `workflow_run` 은 워크플로 파일이 기본 브랜치에 있어야 동작한다. 현재 기본 브랜치인 `phase1` 에 반영된 뒤 실제 코멘트 게시를 검증해야 한다.

기본 브랜치 반영 후 실제 GitHub 검증:

1. `phase1` 대상 테스트 PR 에서 CI 를 성공시켜 상태·head SHA·Actions 실행 시도 링크를 확인한다.
2. 테스트 PR 에 실패하는 변경을 push 해 같은 코멘트가 실패 결과와 새 SHA 로 바뀌는지 확인한다.
3. 해당 CI 를 재실행해 코멘트가 추가되지 않고 실행 시도 링크가 갱신되는지 확인한다.
4. 성공하는 새 커밋을 push 해 같은 코멘트가 갱신되는지 확인한다. 이전 커밋의 CI 를 재실행해도 최신 결과가 유지되어야 한다.
5. `develop` 대상 PR 과 push CI 에서는 코멘트를 게시하지 않는지 확인한다.

실제 토큰 권한·이벤트 전달·코멘트 게시는 위 절차로 확인한다.

## 코드 규칙 테스트

`src/test/java/CodingRulesTest.java` 는 바이트코드로 판단할 수 있는 코드 작성 규칙을 ArchUnit 으로 검사한다. `./gradlew build` 에 테스트로 포함된다.
ArchUnit 의존성은 [package-structure.md](package-structure.md) 의 스니펫으로 추가한다. 컴파일된 main 클래스만 읽는다.

| 규칙 | 근거 문서 |
| --- | --- |
| `System.out`·`System.err`·`printStackTrace` 를 쓰지 않는다 | [logging-and-security.md](logging-and-security.md) |
| `java.util.logging` 타입의 필드를 쓰지 않는다 | [logging-and-security.md](logging-and-security.md) |
| `Exception`·`RuntimeException`·`Throwable` 을 그대로 던지지 않는다 | [clean-code.md](clean-code.md) |
| `java.time` 의 인자 없는 `now()` 를 부르지 않는다 (`now(Clock)` 은 허용) | [testing.md](testing.md) |
| `Optional` 을 필드 타입과 파라미터 타입으로 쓰지 않는다 (반환 타입은 허용) | [clean-code.md](clean-code.md) |
| `ObjectInputStream` 에 의존하지 않는다 | [logging-and-security.md](logging-and-security.md) |

판단이 필요한 규칙(이름, 한 가지 일만 하는가, 로그 문구 등)은 검사하지 않고 문서와 리뷰에 맡긴다.

### 기준선

모든 규칙은 `FreezingArchRule` 로 감싸 있다. 이미 있는 위반은 기준선(`src/test/resources/archunit_store`)에 기록해 두고 **새 위반만** 실패시킨다.
orbit 의 `ArchitectureTest` 는 다르다. 컨텍스트 간 내부 참조 금지 규칙만 기준선에 두고, 순환을 포함한 나머지 규칙은 위반하면 바로 실패한다.
위반을 고치면 기준선에서 줄어든다. 기준선은 커밋해서 팀이 같은 기준으로 검사한다.

처음 도입할 때 기준선을 만드는 절차:

1. `src/test/resources/archunit.properties` 의 `freeze.store.default.allowStoreCreation` 을 `true` 로 바꾼다.
2. `./gradlew test` 를 실행한다. 기준선 파일이 생기고 테스트가 통과한다.
3. 값을 다시 `false` 로 되돌리고 `archunit_store` 와 함께 커밋한다.

- `allowStoreCreation` 은 평소에 `false` 로 둔다. 기준선 파일이 없을 때 새로 만들면 모든 위반이 조용히 통과하기 때문이다. `false` 이면 기준선이 없을 때 테스트가 실패한다.
- 기준선(`archunit_store`)을 손으로 고치거나 지우지 않는다. 테스트가 실패해서 기준선을 지우거나 줄이고 싶어질 때는 위반 코드를 고친다.
- `allowStoreCreation=false` 는 기준선 파일이 통째로 없을 때만 막는다. 기준선이 이미 있을 때 **규칙을 새로 추가하거나 규칙 설명(`.because(...)` 문구 포함)을 바꾸면**
  ArchUnit 이 새 규칙으로 보고 그 시점의 위반을 모두 `archunit_store` 에 기록한다. 그래서 PR 에서는 `archunit_store` 의 diff 를 반드시 함께 리뷰한다.
  규칙을 추가하거나 설명을 바꾼 PR 에서 기준선이 늘었다면 의도한 것인지 확인한다.
- `freeze.store.default.allowStoreUpdate` 는 기본값(`true`)을 그대로 둔다. `false` 로 바꾸지 않는다. 위반을 고쳐도 기준선에서 줄지 않고 남아,
  나중에 같은 위반을 다시 만들어도 기준선에 있는 것으로 보여 조용히 통과한다.
- 규칙 설명은 실행마다 같아야 한다. 순서가 없는 `Set` 을 설명에 넣으면 실행할 때마다 새 규칙이 되어 위반이 조용히 기록된다.
