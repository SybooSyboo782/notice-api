package syboo.notice.notice.api.response;

import java.time.LocalDateTime;

public record NoticeListResponse(
        Long id,
        String title,
        String author,
        LocalDateTime createdDate,
        long viewCount,
        boolean hasAttachment
) {

}