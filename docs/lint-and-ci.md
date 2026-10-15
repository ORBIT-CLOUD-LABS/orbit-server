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

## Phase1 PR CI 결과 코멘트 (#66)

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
