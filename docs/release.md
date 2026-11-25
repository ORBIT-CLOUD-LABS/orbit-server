# 이미지 발행과 릴리스

이미지는 `develop` 머지 때 한 번만 빌드한다. 릴리스는 그 이미지를 다시 빌드하지 않고 같은 digest에 버전 태그를 붙여 orbit-gitops에 반영한다.
그래서 PR에서 검사한 코드로 만든 이미지가 그대로 운영에 나간다.

## 흐름

워크플로 `.github/workflows/release.yml` (`develop` push 때 실행)

| 잡 | 단계 | 결과 |
| --- | --- | --- |
| `image` | 이미지 빌드·push | `ghcr.io/orbit-cloud-labs/orbit-server:sha-<커밋 7자리>` |
| `release` | 머지된 PR의 릴리스 라벨 확인 | 라벨이 없으면 여기서 끝 (배포하지 않음) |
| | 다음 버전 계산 | 직전 `v*` 태그 + 라벨. 첫 릴리스는 라벨과 관계없이 `1.0.0` |
| | 같은 이미지에 버전 태그 | `orbit-server:1.2.4` (digest는 `sha-` 이미지와 같음) |
| | Git 태그 | `v1.2.4` (GitHub App) |
| | orbit-gitops 반영 | `develop`의 `ota/base/kustomization.yaml` 이미지 항목을 `1.2.4` + digest로 커밋 (GitHub App) |

Argo CD가 orbit-gitops 변경을 자동 Sync하므로 OTA Main·Sub가 같은 digest로 바뀐다.
orbit-gitops 커밋 메시지에는 버전, 소스 커밋 SHA, 이미지 digest, Actions 실행 링크가 남는다.

## 태그

| 태그 | 붙는 때 | 바뀌는가 |
| --- | --- | --- |
| `sha-<7자리>` | 모든 `develop` 머지 | 바뀌지 않음. 버전 태그가 없으면 30일 뒤 지움 (`cleanup-images.yml`) |
| `1.2.4` (이미지), `v1.2.4` (Git) | 릴리스 라벨이 붙은 PR 머지 | 바뀌지 않음. 이미 있으면 워크플로가 멈춤 |
| `prod` | orbit-gitops에서 운영 버전이 바뀔 때 (orbit-gitops 워크플로) | 표시용. 배포에 쓰지 않음 |

`latest`, `dev` 같은 이동 태그는 만들지 않는다. orbit-gitops에는 `<버전>@sha256:<digest>`만 들어간다.

## 규칙

- 릴리스 라벨 기준은 [java/lint-and-ci.md](java/lint-and-ci.md)의 "릴리스 라벨"을 따른다. PR 하나에 하나만 붙인다.
- 잘못 나간 릴리스는 태그를 지우거나 옮기지 않고 다음 버전으로 고친다. 급하면 orbit-gitops에서 해당 커밋을 revert해 이전 버전으로 되돌린 뒤 고친다.
- 버전은 커밋 순서를 따른다. 직전 릴리스 태그가 이번 커밋의 조상이 아니면 (더 새 커밋이 이미 릴리스됨) 워크플로가 멈춘다.
- 릴리스는 한 번에 하나만 진행한다 (`concurrency`). 대기 중인 릴리스가 있을 때 또 머지되면 GitHub가 먼저 대기하던 실행을 취소한다.
  취소된 PR의 변경은 다음 릴리스 이미지에 함께 들어가지만, 그 PR의 라벨은 쓰이지 않는다. 그 버전이 꼭 필요하면 다음 PR 라벨을 그에 맞춰 붙인다.
- DB 마이그레이션은 [spring-java/data-jpa.md](spring-java/data-jpa.md)의 "배포 중 호환" 규칙을 지킨다. 롤백한 이전 버전이 새 스키마에서 돌아야 한다.

## 다시 실행

실패한 릴리스는 Actions에서 그 실행을 다시 돌린다. 이미 끝난 단계는 건너뛴다.

- 커밋에 `v*` 태그가 이미 있으면 그 버전으로 남은 단계만 한다.
- 이미지 버전 태그가 같은 digest에 이미 있으면 건너뛰고, 다른 digest에 있으면 멈춘다.
- orbit-gitops가 이미 같은 버전·digest면 커밋하지 않는다. 더 높은 버전이 이미 있으면 덮어쓰지 않는다.
- orbit-gitops에 `ota/base/kustomization.yaml`이 아직 없으면 경고만 남기고 끝난다. 파일이 생긴 뒤 다시 실행한다.

이미지 빌드나 버전 태그 단계가 실패하면 Git 태그와 orbit-gitops는 바뀌지 않는다.

## orbit-gitops 이미지 항목 형식

`ota/base/kustomization.yaml`에 orbit-server 항목이 정확히 하나 있어야 한다. 갱신 스크립트는 `newTag`와 `digest` 값만 바꾼다.

```yaml
images:
  - name: ghcr.io/orbit-cloud-labs/orbit-server   # 또는 name: <짧은 이름> + newName: ghcr.io/orbit-cloud-labs/orbit-server
    newTag: "1.2.4"
    digest: sha256:<64자리>
```

스크립트와 테스트는 `.github/scripts/`, `.github/tests/`에 있다. 바꾼 PR에서는 `Release script tests`가 돈다.

```bash
python3 -m pip install -r .github/requirements-release.txt
python3 -m unittest discover -s .github/tests
```
