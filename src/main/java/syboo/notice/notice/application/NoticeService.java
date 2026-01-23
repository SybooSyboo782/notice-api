package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.repository.NoticeRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;

    /**
     * 신규 공지사항을 등록한다.
     * @param command 공지사항 생성에 필요한 데이터 (DTO)
     * @return 생성된 공지사항의 식별자(ID)
     */
    public Long createNotice(CreateNoticeCommand command) {
        log.info("공지사항 생성 시작: title='{}', author='{}'", command.getTitle(), command.getAuthor());

        // 기본 정보 생성 (내부에서 기간 검증 수행)
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

    /**
     * 요청 데이터로부터 첨부파일 엔티티를 생성하여 공지사항에 추가한다.
     * CascadeType.ALL 설정에 의해 Notice 저장 시 함께 저장된다.
     */
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

    /**
     * 공지사항 정보를 수정한다.
     * <p>
     * JPA의 변경 감지(Dirty Checking) 기능을 활용하여, 엔티티의 상태 변경만으로
     * 트랜잭션 종료 시점에 업데이트 쿼리가 수행된다.
     * 첨부파일의 경우 기존 리스트를 비우고 새로 등록하는 전체 교체 방식을 사용한다.
     *
     * @param noticeId 수정할 공지사항의 식별자
     * @param command  수정할 데이터가 담긴 Command 객체
     * @throws IllegalArgumentException 존재하지 않는 ID이거나 공지 기간 유효성 검증 실패 시 발생
     */
    public void updateNotice(Long noticeId, UpdateNoticeCommand command) {
        log.info("공지사항 수정 시작: id={}, title='{}'", noticeId, command.getTitle());

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> {
                    log.error("공지사항 수정 실패: 존재하지 않는 ID = {}", noticeId);
                    return new IllegalArgumentException("공지사항이 존재하지 않습니다.");
                });

        // 기본 정보 수정 (내부에서 기간 검증 수행)
        notice.update(
                command.getTitle(),
                command.getContent(),
                command.getNoticeStartAt(),
                command.getNoticeEndAt()
        );

        updateAttachments(notice, command.getAttachments());

        log.info("공지사항 수정 완료: id={}", noticeId);
        // @Transactional에 의해 별도의 save() 호출 없이도 변경사항이 DB에 반영(Dirty Checking)됩니다.
    }

    /**
     * 첨부파일 리스트를 교체한다.
     * <p>
     * orphanRemoval = true 설정에 의해 기존 attachments 리스트를 clear() 하면
     * 기존 데이터는 DB에서 자동으로 DELETE 된다.
     *
     * @param notice             수정할 공지사항 엔티티
     * @param attachmentCommands 새롭게 등록할 첨부파일 정보 리스트
     */
    private void updateAttachments(Notice notice, List<UpdateNoticeCommand.AttachmentCommand> attachmentCommands) {
        // 기존 첨부파일 전체 제거 (orphanRemoval = true에 의해 DB에서도 삭제됨)
        notice.removeAllAttachments();

        if (attachmentCommands != null && !attachmentCommands.isEmpty()) {
            for (UpdateNoticeCommand.AttachmentCommand att : attachmentCommands) {
                notice.addAttachment(
                        NoticeAttachment.builder()
                                .fileName(att.getFileName())
                                .storedPath(att.getStoredPath())
                                .fileSize(att.getFileSize())
                                .build()
                );
            }
            log.debug("첨부파일 교체 완료: {}개 신규 등록", attachmentCommands.size());
        }
    }
}
