package syboo.notice.notice.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공지사항 목록 조회 응답 (요약 정보)")
public record NoticeListResponse(
        @Schema(description = "공지사항 ID", example = "1")
        Long id,

        @Schema(description = "제목", example = "2026년 설 연휴 고객센터 휴무 안내")
        String title,

        @Schema(description = "작성자", example = "운영팀")
        String author,

        @Schema(description = "등록 일시")
        LocalDateTime createdDate,

        @Schema(description = "조회수", example = "350")
        long viewCount,

        @Schema(description = "첨부파일 존재 여부", example = "true")
        boolean hasAttachment
) {
}