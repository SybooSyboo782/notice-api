package syboo.notice.notice.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class UpdateNoticeCommand {

    private final Long noticeId;
    private final String title;
    private final String content;
    private final LocalDateTime noticeStartAt;
    private final LocalDateTime noticeEndAt;
    private final List<AttachmentCommand> attachments;

    @Getter
    @RequiredArgsConstructor
    public static class AttachmentCommand {
        private final String fileName;
        private final String storedPath;
        private final long fileSize;
    }
}