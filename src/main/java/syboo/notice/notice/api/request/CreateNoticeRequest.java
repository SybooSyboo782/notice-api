package syboo.notice.notice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateNoticeRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 500, message = "제목은 최대 500자까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotBlank(message = "작성자는 필수입니다.")
        String author,

        @NotNull(message = "공지 시작일은 필수입니다.")
        LocalDateTime noticeStartAt,

        @NotNull(message = "공지 종료일은 필수입니다.")
        LocalDateTime noticeEndAt,

        List<AttachmentRequest> attachments
) {
    public record AttachmentRequest(
            @NotBlank(message = "파일명은 필수입니다.")
            String fileName,

            @NotBlank(message = "저장 경로는 필수입니다.")
            String storedPath,

            long fileSize
    ) {}
}

