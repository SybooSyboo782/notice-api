package syboo.notice.notice.api.response;

import org.springframework.core.io.Resource;

/**
 * 첨부파일 다운로드 요청에 대한 응답 데이터를 담는 객체
 */
public record FileDownloadResponse(
        String originFileName,
        Resource resource
) {

}