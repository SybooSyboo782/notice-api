package syboo.notice.notice.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지사항 수정 요청 모델 (Multipart/form-data)")
public record UpdateNoticeRequest(
        @Schema(description = "수정할 제목", example = "[(수정)] 2026년 상반기 채용 일정 변경")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 500)
        String title,

        @Schema(description = "수정할 내용", example = "기존 일정에서 일주일 연기되었습니다.")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "공지 게시 시작 일시", example = "2026-01-26T00:00:00")
        @NotNull(message = "공지 시작일은 필수입니다.")
        LocalDateTime noticeStartAt,

        @Schema(description = "공지 게시 종료 일시", example = "2026-12-31T23:59:59")
        @NotNull(message = "공지 종료일은 필수입니다.")
        LocalDateTime noticeEndAt,

        @Schema(description = "새롭게 추가할 첨부파일 리스트", type = "string", format = "binary")
        List<MultipartFile> newAttachments,

        @Schema(description = "유지할 기존 첨부파일 ID 리스트 (이 리스트에 없는 기존 파일은 삭제 처리됨)", example = "[1, 2, 5]")
        List<Long> remainAttachmentIds
) {
}