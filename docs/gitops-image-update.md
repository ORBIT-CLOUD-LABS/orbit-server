---
status: Team Review
created: 2026-10-15
updated: 2026-10-15
decision_date: 2026-10-15
---

# Phase1 발행 이미지의 GitOps 자동 반영 (#70)

서버 PR이 phase1에 머지되면 기존 Docker Publish가 이미지를 발행한다. 발행 성공 후 update-gitops job이 전용 App으로 GitOps main의 서버 Deployment image만 갱신한다. 서버 PR 외에 배포별 GitOps 승인 대기를 추가하지 않는다. push:phase1은 직접 push도 실행하므로 PR 보호 규칙은 별도로 유지한다.

## 관리 파일과 적용 계약

| 파일 | 역할 |
| --- | --- |
| `.github/workflows/docker-publish.yml` | 기존 발행 step의 digest output을 갱신 job에 전달하고 App 인증·registry 대응을 검사한다. |
| `.github/scripts/update_gitops_image.py` | 소스 최신성·이미지 필드·diff를 검증하고 GitOps에 일반 push한다. |
| `.github/tests/test_update_gitops_image.py` | 격리된 실제 Git 저장소에서 정상·재실행·역순·경합·실패를 검사한다. |
| `.github/workflows/gitops-update-check.yml` | 관련 phase1 PR에서 updater 검사와 같은 저장소 PR의 App 접근 검증을 수행한다. 이미지를 발행하거나 GitOps를 변경하지 않는다. |
| `.github/requirements-gitops.txt` | updater 전용 Python YAML 의존성을 고정한다. Java 런타임 의존성이 아니다. |

GitOps #7의 이슈에 명시된 `workloads/orbit-server/deployment.yaml`을 계약으로 사용한다. apps/v1 Deployment 이름은 orbit-server, namespace는 orbit(미지정이면 Application의 orbit)을 사용한다. container 이름을 추측하지 않고 image repository가 `ghcr.io/orbit-cloud-labs/orbit-server`인 컨테이너 한 개를 찾는다.

초기 image는 출처를 확인할 전체 서버 SHA 태그여야 한다. 갱신 후에는 `ghcr.io/orbit-cloud-labs/orbit-server:<전체 SHA>@sha256:<발행 digest>`를 사용한다. SHA 태그가 가리키는 registry digest와 발행 output이 같아야 한다. digest를 함께 고정해 나중에 태그가 바뀌더라도 실행 내용이 바뀌지 않게 한다.

#7 경로가 없거나 Deployment·이미지 항목이 다중 일치하면 수정 전에 실패한다. Kustomize 파일이 있으면 덮어쓰는 이미지 원본이 달라질 수 있으므로 자동으로 추측하지 않고 실패한다. #7이 Kustomize를 선택하면 권한을 넓히지 않고 updater의 수정 계약·fixture를 함께 변경한다. 현재 #7 선언이 main에 없는 상태에서 이 job을 활성화하면 이미지 발행은 성공해도 GitOps 갱신은 실패할 수 있다. #7의 최종 선언을 확인한 후 운영 연결을 검증한다.

## 인증·보호 규칙

- 기존 orbit-cloud-gitops-bot을 사용한다. 서버 변수 `GITOPS_APP_CLIENT_ID`, Secret `GITOPS_APP_PRIVATE_KEY`가 필요하다.
- App 설치 토큰은 orbit-gitops만, contents:write로 발급하고 job 종료 시 기본 폐기를 유지한다. App Action은 확인한 v3 commit으로 고정한다.
- GitOps checkout만 해당 토큰을 사용한다. 서버 checkout은 발행 source SHA를 읽고 인증을 영구 저장하지 않는다.
- GitOps protect-main의 App 직접 push 예외를 사용한다. 일반 사람의 PR 승인 1건 규칙과 main-integrity의 예외 없는 삭제·force push 금지를 유지한다.
- PR의 app-access job은 PR 코드를 checkout/실행하지 않는다. 토큰 발급·App slug·contents:write를 요청한 토큰 발급·설치 저장소 한 개의 범위·main 읽기를 확인한다. 실제 main push·bypass 동작 검증과 구분한다. fork PR에서는 실행하지 않는다.

## 중복·경합·실패

공통 concurrency group은 갱신 job을 직렬화하지만 소스 커밋 순서까지 보장하지 않는다. job은 source SHA가 현재 phase1 HEAD와 같은지 시작과 push 직전에 재확인한다. 이미 GitOps가 더 최신 source를 선택하면 skip하고, 소스 ancestry를 증명할 수 없으면 실패한다. 서버 Git 이력은 fetch-depth:0으로 확보한다.

현재 image가 이미 같으면 새 커밋을 만들지 않는다. 같은 source SHA인데 기존 고정 digest와 발행 digest가 다르면 재발행 충돌로 실패시킨다. 기존 빌드 workflow는 SHA 태그를 다시 발행할 수 있으므로 동일 소스 재빌드의 digest 불변성을 가정하지 않는다. 이 경우 현재 배포는 고정 digest를 유지하며 발행 정책·빌드 재현성을 먼저 확인한다.

GitOps는 최신 main을 읽어 image scalar 한 개만 바꾸고 원본 주석·다른 YAML 설정을 보존한다. source SHA·image digest·발행 run/attempt URL을 커밋 메시지에 남긴다. 일반 push가 동시 main 변경으로 거부되면 최신 main에서 다시 계산하며 최대 3회 시도한다. 인증·정책 거부로 main이 그대로면 즉시 실패한다. 원격 force push는 사용하지 않는다.

두 저장소를 원자적으로 잠그지는 않는다. 검사 직후 새 서버 커밋이 생겨 잠시 중간 버전이 반영될 수 있다. 이미 반영된 최신 버전을 늦은 이전 실행이 덮어쓰지 않는지와 최종 최신 성공 실행으로 수렴하는지를 확인한다. 최신 발행 실패 시 이전 정상 이미지를 유지하며 오래된 실행으로 자동 fallback하지 않는다.

발행 job 실패·취소 시 needs의 기본 성공 조건에 따라 update-gitops는 실행하지 않는다. 발행 성공·갱신 실패는 별도 결과이며 이미지 발행 성공만으로 배포 성공을 표시하지 않는다.

## 검증과 증거

로컬 updater 검사:

```bash
python3 -m venv /tmp/orbit-server-gitops-check
/tmp/orbit-server-gitops-check/bin/pip install -r .github/requirements-gitops.txt
/tmp/orbit-server-gitops-check/bin/python -m unittest discover -s .github/tests -v
```

실제 완료 판정은 다음 증거를 연결한다.

1. 서버 PR URL·승인 1건·phase1 merge SHA.
2. Docker Publish run·attempt·SHA 태그·digest 및 registry 대응.
3. GitOps main commit·변경 파일·서버 image. 두 번째 사람 승인 없이 반영됐는지 확인한다.
4. #7 서버 Application의 적용 revision·Pod spec.image·imageID·Ready·health. 전체 E2E 증거는 서버 #61에 연결한다.
5. 발행 실패 시 update job skipped와 GitOps 불변. 로컬 모의 검증과 실제 workflow 실패를 구분한다.

multi-platform digest는 index와 실제 플랫폼 manifest가 다를 수 있으므로 registry에서 실행 아키텍처의 imageID 대응을 확인한다. CDN·플랫폼 image·HPA·Secret은 updater의 변경 범위가 아니다.

2026-10-15 로컬의 실제 Git fixture 17개 검사를 통과했다. #7 실제 manifest·토큰 발급·원격 push·PR merge 후 발행→GitOps→Pod 검증은 이 기록에 포함하지 않는다. 원격 PR 검사 결과와 운영 연결 결과를 추가로 기록한다.

## 복구와 실험 중지

문제가 있으면 먼저 Docker Publish workflow를 GitHub Actions에서 비활성화해 자동 갱신을 중지한다. 이미 실행 중인 run도 확인·중지한다. 이후 GitOps의 이전 검증 image로 되돌리는 새 커밋을 리뷰·반영하고 Argo CD·Pod·health를 확인한다. Git revert는 DB schema·Secret·NFS 파일을 복구하지 않는다.

부하 실험 중에는 서버 phase1·GitOps 이미지/설정 변경을 멈추고 측정 revision을 고정한다. 갱신 재개 시 오래된 run을 무조건 재실행하지 말고 현재 phase1 source·GitOps 상태를 다시 확인한다.
