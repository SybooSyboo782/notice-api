package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.repository.NoticeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public Long createNotice(CreateNoticeCommand command) {
        log.info("공지사항 생성 시작: title='{}', author='{}'", command.getTitle(), command.getAuthor());

        Notice notice = Notice.builder()
                .title(command.getTitle())
                .content(command.getContent())
                .author(command.getAuthor())
                .noticeStartAt(command.getNoticeStartAt())
                .noticeEndAt(command.getNoticeEndAt())
                .build();

        processAttachments(command, notice);

        Notice savedNotice = noticeRepository.save(notice);

        log.info("공지사항 저장 완료: id={}", savedNotice.getId());
        return savedNotice.getId();
    }

    private static void processAttachments(CreateNoticeCommand command, Notice notice) {
        if (command.getAttachments() == null || command.getAttachments().isEmpty()) {
            return;
        }

        for (CreateNoticeCommand.AttachmentCommand att : command.getAttachments()) {
            notice.addAttachment(
                    NoticeAttachment.builder()
                            .fileName(att.getFileName())
                            .storedPath(att.getStoredPath())
                            .fileSize(att.getFileSize())
                            .build()
            );
        }

        log.debug("첨부파일 {}개 추가 완료", command.getAttachments().size());
    }
}
