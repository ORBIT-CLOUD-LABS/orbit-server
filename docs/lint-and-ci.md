# 린트 강제

포맷은 [palantir-java-format](https://github.com/palantir/palantir-java-format) 으로 강제한다.
Gradle 에서는 [Spotless](https://github.com/diffplug/spotless) 플러그인으로 실행한다.
사람이 아니라 도구가 판정한다.

## 스타일 요약

- 들여쓰기: 공백 4칸 (연속 줄은 8칸)
- 한 줄: 120자
- 설정 옵션이 거의 없다. 스타일을 고르지 않고 도구를 따른다.

## 강제 지점

| 시점 | 동작 | 위반 시 |
| --- | --- | --- |
| 빌드 | `check` 태스크에 `spotlessCheck` 포함 (플러그인 기본 동작) | `./gradlew build` 실패 |

위반이 나면 `./gradlew spotlessApply` 로 고친다. 커밋 전에 실행한다.

## 설정

`build.gradle`

```groovy
plugins {
	id 'com.diffplug.spotless' version '8.10.3'
}

spotless {
	java {
		palantirJavaFormat('2.102.0')
		removeUnusedImports()
	}
}
```

플러그인을 적용하면 `spotlessApply`, `spotlessCheck` 태스크가 생기고 `spotlessCheck` 이 `check` 에 연결된다.

## IDE

IDE 포맷터는 편의용이다. 최종 판정은 `spotlessCheck` 이다.

### IntelliJ 플러그인 설치

1. Settings > Plugins > Marketplace 에서 `palantir-java-format` 을 설치하고 IDE 를 재시작한다.
2. Settings > Other Settings > palantir-java-format Settings 에서 Enable 을 체크한다.
   (프로젝트마다 한 번씩 켜야 한다.)

- 확인: Reformat Code 시 공백 4칸으로 정렬되면 정상이다.
- 저장 시 포맷: Settings > Tools > Actions on Save > Reformat code.
