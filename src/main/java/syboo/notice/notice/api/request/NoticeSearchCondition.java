package syboo.notice.notice.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공지사항 검색 조건")
public record NoticeSearchCondition(
        @Schema(description = "검색어 (제목 또는 내용에 포함된 단어, 앞뒤 공백은 자동으로 제거됨)", example = "업데이트")
        String query,

        @Schema(description = "검색 타입 (TITLE: 제목 검색, TITLE_CONTENT: 제목+내용 통합 검색)", example = "TITLE_CONTENT")
        String searchType,

        @Schema(description = "조회 시작일 (등록일 기준)", example = "2026-01-01T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "조회 종료일 (등록일 기준)", example = "2026-01-26T23:59:59")
        LocalDateTime endDate
) {
    // 생성자에서 trim() 처리
    public NoticeSearchCondition {
        query = (query != null) ? query.trim() : null;
    }
}