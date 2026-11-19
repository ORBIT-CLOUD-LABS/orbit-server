# AGENTS.md

orbit-server 에서 Java 코드를 작성·수정·리뷰하는 AI 에이전트용 지침.
세부 규칙은 `docs/` 에 있고, 이 문서는 작업 순서와 어떤 문서를 언제 볼지를 정한다.

## 요구사항

요구사항의 원본(source of truth)은 orbit 레포에 있다. 이 레포에 복사하지 않는다.
기능 구현 전에 읽는 방법과 규칙은 [docs/requirements.md](docs/requirements.md).
프로젝트의 목표와 범위도 요구사항 원본을 따른다. 그래서 별도의 방향 문서(agentsmd `project` 프리셋의 `direction.md`)는 두지 않는다.

## 문서 지도

`docs/java/`, `docs/spring-java/` 는 [agentsmd-kit v0.8.0](https://github.com/xoruddl/agentsmd-kit/tree/v0.8.0) 의 `java`·`spring-java` 프리셋 문서를 orbit 에 맞게 고쳐 쓴 것이다.
orbit 이 프리셋과 다르게 정한 규칙은 따로 두지 않고 해당 문서에 바로 적는다.
프리셋 버전을 올릴 때는 `agentsmd init --force` 로 덮어쓰지 않는다. 이전 원본과 지금 문서의 차이(orbit 수정분)를 새 원본에 다시 적용한다.

프리셋 문서가 설명하는 `LayerArchitectureTest`·`PackageSizeTest`·`CodingRulesTest`·`SpringCodingRulesTest` 는 템플릿 그대로 `src/test/java/`(기본 패키지)에 있다.
템플릿에 없는 orbit 규칙(다른 컨텍스트 내부 참조 금지, `global/external` 포트 규칙)은 `com.orbit.server.ContextBoundaryTest` 가 검사한다.

| 문서 | 읽는 시점 |
| --- | --- |
| [docs/requirements.md](docs/requirements.md) | 기능 구현 전 (요구사항 확인) |
| [docs/architecture.md](docs/architecture.md) | 도메인 설계 (DDD). Shared Kernel, Aggregate, 도메인 서비스·이벤트, 컨텍스트 간 통신 |
| [docs/git-conventions.md](docs/git-conventions.md) | Issue·Branch·커밋·PR·리뷰 (Org 공통 CONTRIBUTING) |
| [docs/java/checklist.md](docs/java/checklist.md) | Java 코드 작성·리뷰 |
| [docs/java/clean-code.md](docs/java/clean-code.md) | 코드 작성, 설계 |
| [docs/java/clean-code-examples.md](docs/java/clean-code-examples.md) | 코드 작성 시 참고 예시 |
| [docs/java/code-style.md](docs/java/code-style.md) | 코드 작성, 주석 |
| [docs/java/testing.md](docs/java/testing.md) | 테스트를 쓸 때 |
| [docs/java/logging-and-security.md](docs/java/logging-and-security.md) | 로그를 남길 때, 비밀값·외부 입력·의존성을 다룰 때 |
| [docs/java/lint-and-ci.md](docs/java/lint-and-ci.md) | 린트·포맷·CI 설정 |
| [docs/java/package-structure.md](docs/java/package-structure.md) | 클래스를 추가할 때, 패키지가 커져 PackageSizeTest 가 실패할 때 |
| [docs/spring-java/architecture.md](docs/spring-java/architecture.md) | 클래스·패키지를 만들 때. 패키지 구조, 의존 방향, 주입, DTO 경계, 설정 |
| [docs/spring-java/data-jpa.md](docs/spring-java/data-jpa.md) | 엔티티·Repository·트랜잭션을 다룰 때. 지연 로딩, N+1, 스키마 마이그레이션 |
| [docs/spring-java/api.md](docs/spring-java/api.md) | 컨트롤러·API 를 만들 때. URL, 상태 코드, 요청 검증, 오류 응답, 페이징 |
| [docs/spring-java/testing.md](docs/spring-java/testing.md) | Spring 이 필요한 테스트를 쓸 때. 슬라이스 테스트, @MockitoBean, Testcontainers |

문서끼리 충돌하면 `docs/architecture.md` > `docs/spring-java/` > `docs/java/` 순으로 따른다.
`docs/java/` 안에서는 `checklist.md` > `clean-code.md` 순이다. (예: 파라미터 객체 검토 예외는 checklist 의 기준을 따른다.)

## 핵심 규칙

문서를 다 읽지 못했더라도 아래는 반드시 지킨다.

- 패키지는 최상위 `domain` / `global` 로 나눈다. `domain` 아래는 바운디드 컨텍스트별로 `controller` / `dto` (`request`, `response`) / `service` / `repository` / `domain` / `exception` 을 둔다.
  컨텍스트 안의 `domain` 에는 엔티티, 값 객체, enum 을 두고, 도메인 이벤트는 `domain/event` 에 둔다. (`com.orbit.server.domain.<컨텍스트>.domain`)
  레이어 안에서 역할이 다르면 클래스가 적어도 역할 하위 패키지로 나눈다. 새 클래스의 위치는 [docs/spring-java/architecture.md](docs/spring-java/architecture.md) 의 "클래스를 추가할 때" 를 따른다.
- 의존 방향은 `controller → service → repository → domain`, 최상위 `domain → global`. 다른 컨텍스트는 `service` 나 이벤트로만 접근한다.
- `global` 에는 설정·전역 예외·보안 같은 기술 기반만 둔다. 비즈니스 규칙은 두지 않는다.
  외부 시스템 연동 구현은 `global/external/{시스템}` 에 두고, `service` 의 포트(인터페이스)를 구현한다.
- `XxxService` 는 유스케이스를 조율하는 서비스만 쓴다. 도메인 서비스는 역할로 이름 짓는다. (`DispatchPolicy`)
- Repository 는 `JpaRepository` 를 바로 상속하고 서비스가 직접 주입받는다. 포트와 구현으로 나누지 않는다.
- 단건 상세 조회는 엔티티를 읽어 변환하고, 목록·대시보드는 Repository 의 `@Query` projection 으로 읽는다.
- API 는 성공·실패 모두 공통 응답 `ApiResponse` 로 응답한다. `ProblemDetail` 을 쓰지 않는다.
- 빈 catch 금지. 커스텀 예외로 감싸고 cause 를 유지한다.
- 인터페이스는 경계에만 둔다: 다른 모듈이 호출하는 진입점, 외부 시스템 연동, 구현이 둘 이상인 정책.
  모듈 내부의 단일 구현 클래스에는 만들지 않는다. (`XxxServiceImpl` 금지)
- 과설계 금지: 패턴은 문제가 보일 때 적용하고, 중복은 세 번 반복될 때 추출한다.
- 주석에는 코드만 봐서는 알 수 없는 의도와 이유를 쓴다. 코드를 되풀이하지 않는다.
- 포맷은 손으로 맞추지 않는다. 작업을 마치기 전 `./gradlew spotlessApply`.
- 기능은 테스트와 함께 쓴다. 버그 수정은 재현 테스트부터 쓴다.
- 로그와 예외 메시지에 비밀값·개인정보를 남기지 않는다.
- 새 의존성은 사용자에게 묻고 추가한다.
- 필드 주입 대신 생성자 주입을 쓴다.
- 엔티티를 API 요청·응답으로 쓰지 않는다. DTO(record)로 바꾼다.
- `@Transactional` 은 service 에 두고, 조회는 `readOnly = true`. 트랜잭션 안에서 외부 API 를 부르지 않는다.
- ArchUnit 기준선(archunit_store), PackageSizeTest 의 큰 패키지 상한, 순환 허용 목록을 늘리지 않는다. 넘으면 패키지를 나누고, 역방향 의존은 도메인 이벤트로 끊는다.

중첩, 인자 개수, 값 객체, 일급 컬렉션, getter, 협력 객체 수, 디미터 법칙 같은 코드 단위 기준은 [docs/java/checklist.md](docs/java/checklist.md),
null·매직 넘버·예외는 [docs/java/clean-code.md](docs/java/clean-code.md), Javadoc·주석은 [docs/java/code-style.md](docs/java/code-style.md) 에 있다.

## 작업 방식

- 기존 코드의 구조·네이밍·주석 밀도를 먼저 확인하고 그에 맞춘다.
- 요청 범위 밖의 리팩터링이나 파일 정리는 하지 않는다. 필요해 보이면 제안만 한다.
- 작업을 마치기 전에 아래 검증을 실행하고 결과를 그대로 보고한다. 실패를 숨기지 않는다.

## 검증

작업을 마치기 전에 `agentsmd verify` 를 실행하고 결과를 그대로 보고한다. 단계 목록은 `.agentsmd/verify.json`.

```bash
./gradlew spotlessApply              # 포맷 적용 (palantir-java-format). verify 전에 먼저 실행한다
npx @agentsmd/cli@0.8.0 verify       # .agentsmd/verify.json 의 단계(./gradlew spotlessCheck build)를 실행한다
```

- 포맷은 손으로 맞추지 않고 palantir-java-format 에 맡긴다. 들여쓰기는 공백 4칸. 강제 방식은 [docs/java/lint-and-ci.md](docs/java/lint-and-ci.md).
