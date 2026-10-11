package com.orbit.server.domain.artifact.application;

import com.orbit.server.domain.artifact.model.ArtifactFile;
import org.springframework.core.io.InputStreamSource;

/**
 * 업데이트 원본 파일을 보관하는 Origin File Server.
 *
 * <p>Origin 은 외부 시스템이고 테스트에서 대체해야 하므로 인터페이스로 둔다. 구현은 infrastructure 에 둔다.
 */
public interface OriginStorage {

    /**
     * 파일을 Origin 의 {@link ArtifactFile#path()} 위치에 올린다. 등록한 파일은 수정하지 않으므로 이미 있는 파일을 덮어쓰지 않는다.
     *
     * @param file 올릴 위치와 크기·해시
     * @param content 파일 내용. 해시 계산과 업로드에서 각각 읽을 수 있도록 매번 새 스트림을 여는 출처를 받는다
     * @throws OriginFileExistsException 같은 경로에 이미 파일이 있는 경우
     * @throws OriginUploadException 업로드에 실패한 경우
     */
    void upload(ArtifactFile file, InputStreamSource content);
}
