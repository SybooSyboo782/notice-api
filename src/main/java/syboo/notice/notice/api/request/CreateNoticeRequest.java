package syboo.notice.notice.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지사항 생성 요청 모델 (Multipart/form-data)")
public record CreateNoticeRequest(
        @Schema(description = "공지사항 제목", example = "신규 기능 업데이트 안내")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 500, message = "제목은 최대 500자까지 입력 가능합니다.")
        String title,

        @Schema(description = "공지사항 상세 내용", example = "이번 업데이트를 통해 성능이 20% 향상되었습니다.")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "작성자 성함/ID", example = "관리자")
        @NotBlank(message = "작성자는 필수입니다.")
        String author,

        @Schema(description = "공지 게시 시작 일시 (ISO-8601)", example = "2026-01-26T00:00:00")
        @NotNull(message = "공지 시작일은 필수입니다.")
        LocalDateTime noticeStartAt,

        @Schema(description = "공지 게시 종료 일시 (ISO-8601)", example = "2026-12-31T23:59:59")
        @NotNull(message = "공지 종료일은 필수입니다.")
        LocalDateTime noticeEndAt,

        @Schema(description = "첨부파일 리스트 (다중 파일 업로드 가능)", type = "string", format = "binary")
        List<MultipartFile> attachments
) {
}