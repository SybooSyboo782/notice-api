package syboo.notice.notice.api.request;

import java.time.LocalDateTime;

public record NoticeSearchCondition(
        String title,
        String content,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}