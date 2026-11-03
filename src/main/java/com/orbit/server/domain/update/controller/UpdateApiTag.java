package com.orbit.server.domain.update.controller;

/** 체크인과 매니페스트 API 를 API 문서에서 한 그룹으로 묶는 Tag. 두 컨트롤러가 같은 값을 써야 그룹이 갈라지지 않는다. */
final class UpdateApiTag {

    static final String NAME = "차량 · 업데이트";
    static final String DESCRIPTION = "체크인으로 업데이트 대상을 확인하고 매니페스트로 다운로드 정보를 받는다";

    private UpdateApiTag() {}
}
