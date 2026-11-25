# 이미지 발행과 릴리스

팀 작업은 `develop`에 모으고, 배포할 때만 `develop` → `main` PR을 머지한다. `develop` 머지에서는 검사만 돈다.

이미지는 `main` 머지 때 한 번만 빌드한다. 릴리스는 그 이미지를 다시 빌드하지 않고 같은 digest에 버전 태그를 붙여 orbit-gitops에 배포 PR로 올린다.
그래서 PR에서 검사한 코드로 만든 이미지가 그대로 운영에 나간다.

## 흐름

워크플로 `.github/workflows/release.yml` (`main` push 때 실행)

| 잡 | 단계 | 결과 |
| --- | --- | --- |
| `image` | 이미지 빌드·push | `ghcr.io/orbit-cloud-labs/orbit-server:sha-<커밋 7자리>` |
| `release` | 머지된 PR의 릴리스 라벨 확인 | 라벨이 없으면 여기서 끝 (배포하지 않음) |
| | 다음 버전 계산 | 직전 `v*` 태그 + 라벨. 첫 릴리스는 라벨과 관계없이 `1.0.0` |
| | 같은 이미지에 버전 태그 | `orbit-server:1.2.4` (digest는 `sha-` 이미지와 같음) |
| | Git 태그 | `v1.2.4` (GitHub App) |
| | orbit-gitops 배포 PR | `release/orbit-server-1.2.4` 브랜치에서 `ota/base/kustomization.yaml` 이미지 항목을 `1.2.4` + digest로 바꾸고 `main` 대상 PR을 연다 (GitHub App) |

이 단계까지는 운영에 나가지 않는다. orbit-gitops는 `main` 브랜치 하나만 쓰고 Argo CD가 그 `main`을 읽는다.
팀원이 배포 PR을 검사(`manifest-checks`) 통과 뒤 승인·머지하면 Argo CD가 자동 Sync해 OTA Main·Sub가 같은 digest로 바뀐다.
그래서 운영 반영 전에 orbit-server `develop` → `main` 머지와 orbit-gitops 배포 PR 머지를 한 번씩 거친다.
배포 PR과 그 커밋 메시지에는 버전, 소스 커밋 SHA, 이미지 digest, Actions 실행 링크가 남는다.

- 새 배포 PR이 열리면 더 낮은 버전의 열린 배포 PR은 봇이 닫는다. 오래된 PR을 나중에 머지해 버전이 내려가지 않게 한다.
- 배포 PR은 Squash로 머지해도 된다. orbit-gitops에는 갈라질 다른 브랜치가 없다.

## 태그

| 태그 | 붙는 때 | 바뀌는가 |
| --- | --- | --- |
| `sha-<7자리>` | 모든 `main` 머지 | 바뀌지 않음. 버전 태그가 없으면 30일 뒤 지움 (`cleanup-images.yml`) |
| `1.2.4` (이미지), `v1.2.4` (Git) | 릴리스 라벨이 붙은 PR 머지 | 바뀌지 않음. 이미 있으면 워크플로가 멈춤 |
| `prod` | orbit-gitops에서 운영 버전이 바뀔 때 (orbit-gitops 워크플로) | 표시용. 배포에 쓰지 않음 |

`latest`, `dev` 같은 이동 태그는 만들지 않는다. orbit-gitops에는 `<버전>@sha256:<digest>`만 들어간다.

## 규칙

- 릴리스 라벨은 `develop` → `main` PR에 붙인다. 그 PR에 모인 변경 전체를 보고 기준([java/lint-and-ci.md](java/lint-and-ci.md)의 "릴리스 라벨")에 맞는 것 하나를 고른다.
- `develop` → `main` PR은 **Create a merge commit**으로 머지한다. Squash나 Rebase로 머지하면 `develop`과 `main`의 이력이 갈라져 다음 PR에서 충돌이 나고, 워크플로가 머지 커밋으로 PR을 찾지 못한다.
- 잘못 나간 릴리스는 태그를 지우거나 옮기지 않고 다음 버전으로 고친다. 급하면 orbit-gitops `main`에서 배포 PR의 커밋을 revert하는 PR을 머지해 이전 버전으로 되돌린 뒤 고친다.
- 버전은 커밋 순서를 따른다. 직전 릴리스 태그가 이번 커밋의 조상이 아니면 (더 새 커밋이 이미 릴리스됨) 워크플로가 멈춘다.
- 릴리스는 한 번에 하나만 진행한다 (`concurrency`). 대기 중인 릴리스가 있을 때 또 머지되면 GitHub가 먼저 대기하던 실행을 취소한다.
  취소된 PR의 변경은 다음 릴리스 이미지에 함께 들어가지만, 그 PR의 라벨은 쓰이지 않는다. 그 버전이 꼭 필요하면 다음 PR 라벨을 그에 맞춰 붙인다.
- DB 마이그레이션은 [spring-java/data-jpa.md](spring-java/data-jpa.md)의 "배포 중 호환" 규칙을 지킨다. 롤백한 이전 버전이 새 스키마에서 돌아야 한다.

## 다시 실행

실패한 릴리스는 Actions에서 그 실행을 다시 돌린다. 이미 끝난 단계는 건너뛴다.

- 커밋에 `v*` 태그가 이미 있으면 그 버전으로 남은 단계만 한다.
- 이미지 버전 태그가 같은 digest에 이미 있으면 건너뛰고, 다른 digest에 있으면 멈춘다.
- orbit-gitops `main`에 이미 같은 버전·digest가 있거나 같은 버전의 배포 PR이 열려 있으면 새 PR을 만들지 않는다.
- `main`이나 열린 배포 PR에 더 높은 버전이 있으면 PR을 만들지 않는다.
- 앞선 실행이 브랜치만 push하고 PR을 못 만들었으면 브랜치를 같은 내용으로 덮어쓰고 PR을 연다.
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

## GitHub·GHCR 설정 (관리자, 한 번)

워크플로가 돌기 전에 아래를 설정한다. 비밀값은 저장소 설정에만 두고 문서·이슈에 적지 않는다.

1. GitHub App `orbit-cloud-gitops-bot`
   - 설치 저장소에 `orbit-server`, `orbit-gitops` 포함
   - Repository permissions → Contents: Read and write (orbit-server 태그 생성, orbit-gitops 배포 브랜치 push), Pull requests: Read and write (orbit-gitops 배포 PR 열기·닫기)
   - orbit-server 저장소 변수 `GITOPS_APP_CLIENT_ID`, 시크릿 `GITOPS_APP_PRIVATE_KEY`가 있는지 확인
2. orbit-server 태그 ruleset (Settings → Rules → Rulesets → New tag ruleset)
   - 대상: `v*`
   - Restrict creations, Restrict updates, Restrict deletions 켜기
   - Bypass list: `orbit-cloud-gitops-bot` (생성은 App만)
3. orbit-gitops ruleset에는 App 우회를 넣지 않는다. App은 `release/orbit-server-*` 브랜치에만 push하고, `main`에는 PR로만 들어간다
4. GHCR 패키지 `orbit-server` (Org → Packages → orbit-server → Package settings)
   - Change visibility → Public (클러스터에 pull Secret을 두지 않음)
   - Manage Actions access: `orbit-server` 저장소 Admin (발행·태그·정리 워크플로가 버전을 지우려면 Admin 필요)

## 설정 뒤 확인

1. 라벨 없는 PR 머지 → `sha-<7자리>` 이미지만 생기고 Git 태그·orbit-gitops는 그대로인지
2. `release:patch` PR 머지 (첫 릴리스) → Git 태그 `v1.0.0`, 이미지 태그 `1.0.0`(digest가 `sha-` 이미지와 같음), orbit-gitops 배포 PR이 열리고 `manifest-checks`가 도는지
3. 그 실행을 다시 돌림 → 새 태그·PR 없이 끝나는지
4. 배포 PR을 머지 → Argo CD에서 OTA Main·Sub가 같은 digest로 바뀌는지
5. 정리 워크플로를 `dry-run`으로 수동 실행 → 버전 태그가 붙은 이미지는 대상에 없는지
