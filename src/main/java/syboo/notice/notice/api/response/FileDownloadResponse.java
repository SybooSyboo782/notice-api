package syboo.notice.notice.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.Resource;

/**
 * 첨부파일 다운로드 요청에 대한 응답 데이터를 담는 객체
 */
@Schema(description = "첨부파일 다운로드 응답 모델")
public record FileDownloadResponse(

        @Schema(description = "원본 파일명", example = "project_plan.pdf")
        String originFileName,

        @Schema(description = "파일 리소스 (바이너리 데이터)", hidden = true)
        Resource resource
) {

}