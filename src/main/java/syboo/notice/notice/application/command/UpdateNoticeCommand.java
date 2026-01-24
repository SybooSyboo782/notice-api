package syboo.notice.notice.application.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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

    // 1. 신규로 추가 업로드된 파일들
    private final List<MultipartFile> newAttachments;

    // 2. 삭제되지 않고 유지되어야 하는 기존 첨부파일의 ID 목록
    private final List<Long> remainAttachmentIds;
}