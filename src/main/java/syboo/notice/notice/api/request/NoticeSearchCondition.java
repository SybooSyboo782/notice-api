package syboo.notice.notice.api.request;

import java.time.LocalDateTime;

public record NoticeSearchCondition(
        String query,
        String searchType,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    // 생성자에서 trim() 처리
    public NoticeSearchCondition {
        query = (query != null) ? query.trim() : null;
    }
}