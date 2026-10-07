# AGENTS.md

orbit-server 에서 Java 코드를 작성·수정·리뷰하는 AI 에이전트용 지침.
세부 규칙은 `docs/` 에 있고, 이 문서는 작업 순서와 어떤 문서를 언제 볼지를 정한다.

## 요구사항

요구사항의 원본(source of truth)은 orbit 레포에 있다. 이 레포에 복사하지 않는다.
기능 구현 전에 읽는 방법과 규칙은 [docs/requirements.md](docs/requirements.md).

## 작업 전 읽을 문서

| 상황 | 문서 |
| --- | --- |
| 기능 구현 (요구사항 확인) | [docs/requirements.md](docs/requirements.md) |
| 패키지·클래스 위치, 도메인 설계 (DDD) | [docs/architecture.md](docs/architecture.md) |
| Java 코드 작성·리뷰 (항상) | [docs/java-checklist.md](docs/java-checklist.md) |
| 설계·함수·예외·SOLID 판단 | [docs/clean-code.md](docs/clean-code.md) |
| 원칙 적용 예시가 필요할 때 | [docs/clean-code-examples.md](docs/clean-code-examples.md) |
| 포맷·네이밍·주석 | [docs/code-style.md](docs/code-style.md) |
| Issue·Branch·커밋·PR·리뷰 (Org 공통 CONTRIBUTING) | [docs/git-conventions.md](docs/git-conventions.md) |

문서끼리 충돌하면 `java-checklist.md` > `clean-code.md` 순으로 따른다.
(예: 들여쓰기는 clean-code 의 2단계가 아니라 checklist 의 1단계를 따른다.)

## 핵심 규칙 요약

문서를 다 읽지 못했더라도 아래는 반드시 지킨다.

- 패키지는 최상위 `domain` / `global` 로 나눈다. `domain` 아래는 바운디드 컨텍스트별로 `model` / `application` / `infrastructure` / `presentation` 을 둔다.
- 의존 방향은 `presentation → application → model ← infrastructure`, `domain → global`. 다른 컨텍스트는 `application` 인터페이스나 이벤트로만 접근한다.
- `global` 에는 설정·전역 예외·보안 같은 기술 기반만 둔다. 비즈니스 규칙은 두지 않는다.
- `XxxService` 는 `application` 의 애플리케이션 서비스만 쓴다. `model` 의 도메인 서비스는 역할로 이름 짓는다. (`DispatchPolicy`)

- 메서드 들여쓰기 1단계, `else` 금지, 조기 반환.
- 메서드 인자 3개 이하, 한 가지 일만.
- 원시값·문자열은 의미 있는 타입으로 포장하고 검증은 생성자에서.
- 컬렉션은 일급 컬렉션으로 감싼다.
- 도메인 객체에 getter/setter 금지. 값을 꺼내 판단하지 말고 객체에 시킨다. (DTO 는 예외)
- 인스턴스 변수 2개 이하를 목표로 한다.
- 한 줄에 점 하나. (디미터 법칙)
- 빈 catch 금지. 커스텀 예외로 감싸고 cause 를 유지한다.
- null 반환 대신 `Optional` 또는 빈 컬렉션.
- 매직 넘버·문자열은 상수나 enum 으로.
- public 클래스·메서드에는 Javadoc. 주석은 무엇이 아니라 **왜**를 쓴다.
- 인터페이스는 경계에만 둔다: 다른 모듈이 호출하는 진입점, 외부 시스템 연동, 구현이 둘 이상인 정책.
  모듈 내부의 단일 구현 클래스에는 만들지 않는다. (`XxxServiceImpl` 금지)
- 과설계 금지: 패턴은 문제가 보일 때 적용하고, 중복은 세 번 반복될 때 추출한다.

## 작업 방식

- 기존 코드의 구조·네이밍·주석 밀도를 먼저 확인하고 그에 맞춘다.
- 요청 범위 밖의 리팩터링이나 파일 정리는 하지 않는다. 필요해 보이면 제안만 한다.
- 기능 구현은 테스트와 함께 작성한다. 버그 수정은 재현 테스트를 먼저 쓴다.
- 작업을 마치기 전에 아래 검증을 실행하고 결과를 그대로 보고한다. 실패를 숨기지 않는다.

## 빌드·검증

```bash
./gradlew spotlessApply   # 포맷 적용 (palantir-java-format)
./gradlew spotlessCheck   # 포맷 검사
./gradlew test            # 테스트
./gradlew build           # 전체 빌드
```

- 포맷은 손으로 맞추지 않고 palantir-java-format 에 맡긴다. 들여쓰기는 공백 4칸. 강제 방식은 [docs/lint-and-ci.md](docs/lint-and-ci.md).
