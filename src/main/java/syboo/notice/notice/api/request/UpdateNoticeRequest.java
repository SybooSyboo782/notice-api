package syboo.notice.notice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateNoticeRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 500)
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotNull(message = "공지 시작일은 필수입니다.")
        LocalDateTime noticeStartAt,

        @NotNull(message = "공지 종료일은 필수입니다.")
        LocalDateTime noticeEndAt,

        List<MultipartFile> newAttachments,

        // 2. 유지할 기존 파일들의 ID 리스트 (이 리스트에 없는 기존 파일은 삭제 처리)
        List<Long> remainAttachmentIds
) {
}