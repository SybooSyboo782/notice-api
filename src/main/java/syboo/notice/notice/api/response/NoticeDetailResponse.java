package syboo.notice.notice.api.response;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdDate,
        long viewCount,
        List<AttachmentResponse> attachments
) {
    public record AttachmentResponse(
            Long id,
            String fileName,
            String storedPath,
            long fileSize
    ) {}
}