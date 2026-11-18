# orbit 규칙 (프리셋 확장)

코드 규칙은 agentsmd 프리셋 원본인 [java/](java/)와 [spring-java/](spring-java/) 문서를 기본으로 따른다.
이 문서에는 orbit 이 원본과 다르게 정했거나 원본에 없는 규칙만 적는다. 원본과 겹치면 이 문서가 우선한다.

프리셋 문서는 고치지 않는다. 다음 버전으로 올릴 때 덮어쓰기만 하면 되도록, orbit 의 차이는 여기에 더한다.
도메인 설계(DDD, 바운디드 컨텍스트)는 [architecture.md](architecture.md) 에 둔다.

## 기존 코드

- 기존 코드는 [java/checklist.md](java/checklist.md) 보다 엄격한 규칙(객체지향 생활 체조)으로 작성됐다.
  그래도 checklist 의 위반은 아니므로 일괄로 고치지 않고, 변경할 때 필요하면 맞춘다.
- 원본 규칙을 새로 채택했는데 기존 코드가 그와 다르면, 일괄로 고치지 않고 그 코드를 변경할 때 맞춘다.

## 이름

- boolean 은 원본처럼 `is/has/can` 접두를 쓰되, 인자를 받는 판정은 예/아니오로 읽히는 3인칭 동사도 허용한다. (`contains(item)`)

## API 응답

원본 [spring-java/api.md](spring-java/api.md) 와 달리 orbit 은 자체 공통 응답 형식을 쓴다. 응답 형식은 API 계약이라 바꾸면 클라이언트가 깨진다.

- 성공·실패 모두 `ApiResponse`(`isSuccess`, `code`, `message`, `data`, `error`)로 응답한다. `ProblemDetail` 을 쓰지 않는다.
- 성공 코드는 `SuccessCode`, 오류 코드는 `ErrorCode` 를 구현한 enum(공통 `CommonErrorCode`, 컨텍스트별 `XxxErrorCode`)으로 정한다. 클라이언트는 `code` 로 분기한다.
- 생성은 `201` 과 `ApiResponse` 본문(생성한 ID 등)으로 응답한다. `Location` 헤더는 두지 않는다.

## 테스트

원본 [spring-java/testing.md](spring-java/testing.md) 와 다른 점만 적는다.

- 컨트롤러 테스트는 `MockMvcTester` 대신 `MockMvc` 를 쓴다. 기존 테스트와 같은 방식으로 맞춘다.
- Repository·쿼리 테스트는 `@DataJpaTest` 대신 `@SpringBootTest` 와 `TestcontainersConfiguration` 을 쓴다. 운영과 같은 DB 를 띄우는 원칙은 같다.

## 빌드

- 의존성 버전은 `gradle/libs.versions.toml` 이 아니라 `build.gradle` 에 적는다. 버전 카탈로그 도입은 별도 이슈로 다룬다.

## 원본에서 새 코드부터 채택한 규칙

기존 코드에는 아직 없지만 원본대로 따르는 규칙이다. 기존 코드는 위 "기존 코드" 절대로 변경할 때 맞춘다.

- null 이 될 수 있는 반환값·파라미터에는 JSpecify `@Nullable` 을 붙인다. → [spring-java/architecture.md](spring-java/architecture.md)
- 개수가 늘어날 수 있는 새 목록 API 는 페이징한다. `default_batch_fetch_size` 는 컬렉션을 묶어 읽어야 할 때 함께 설정한다.
  → [spring-java/api.md](spring-java/api.md), [spring-java/data-jpa.md](spring-java/data-jpa.md)

## CI

공통 검증 명령은 [java/lint-and-ci.md](java/lint-and-ci.md) 를 따른다. 아래는 orbit 저장소의 GitHub Actions 설정이다.

### Phase1 PR CI 결과 코멘트 (#66)

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
