package syboo.notice.notice.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CreateNoticeCommand {

    private String title;
    private String content;
    private String author;
    private LocalDateTime noticeStartAt;
    private LocalDateTime noticeEndAt;

    private List<AttachmentCommand> attachments;

    @Getter
    @AllArgsConstructor
    public static class AttachmentCommand {
        private String fileName;
        private String storedPath;
        private long fileSize;
    }
}
