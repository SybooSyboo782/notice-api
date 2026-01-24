package syboo.notice.notice.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

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

    private final List<MultipartFile> attachments;
}
