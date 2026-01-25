package syboo.notice.notice.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지사항 상세 조회 응답")
public record NoticeDetailResponse(
        @Schema(description = "공지사항 ID", example = "1")
        Long id,

        @Schema(description = "제목", example = "2026년 상반기 신입 사원 채용 공고")
        String title,

        @Schema(description = "내용", example = "자세한 사항은 첨부파일을 확인해 주세요.")
        String content,

        @Schema(description = "작성자", example = "인사팀")
        String author,

        @Schema(description = "등록 일시")
        LocalDateTime createdDate,

        @Schema(description = "조회수", example = "128")
        long viewCount,

        @Schema(description = "첨부파일 목록")
        List<AttachmentResponse> attachments
) {
    @Schema(description = "공지사항 첨부파일 정보")
    public record AttachmentResponse(
            @Schema(description = "파일 ID", example = "10")
            Long id,

            @Schema(description = "원본 파일명", example = "recruit_guide.pdf")
            String originFileName,

            @Schema(description = "파일 크기 (Byte 단위)", example = "1024576")
            long fileSize,

            @Schema(description = "파일 확장자/타입", example = "application/pdf")
            String contentType
    ) {}
}