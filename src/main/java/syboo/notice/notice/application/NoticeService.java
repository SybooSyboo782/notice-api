package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.common.exception.NoticeNotFoundException;
import syboo.notice.notice.application.command.CreateNoticeCommand;
import syboo.notice.notice.application.command.UpdateNoticeCommand;
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
    private final NoticeFileService noticeFileService;

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

        // 첨부파일 물리 저장 및 엔티티 매핑
        noticeFileService.storeFiles(command.getAttachments(), notice);

        Notice savedNotice = noticeRepository.save(notice);

        log.info("공지사항 저장 완료: id={}", savedNotice.getId());
        return savedNotice.getId();
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
     * @throws NoticeNotFoundException 존재하지 않는 ID이거나 공지 기간 유효성 검증 실패 시 발생
     */
    public void updateNotice(Long noticeId, UpdateNoticeCommand command) {
        log.info("공지사항 수정 시작: id={}, title='{}'", noticeId, command.getTitle());

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> {
                    log.error("공지사항 수정 실패: 존재하지 않는 ID = {}", noticeId);
                    return new NoticeNotFoundException(noticeId);
                });

        // 기본 정보 수정 (내부에서 기간 검증 수행)
        notice.update(
                command.getTitle(),
                command.getContent(),
                command.getNoticeStartAt(),
                command.getNoticeEndAt()
        );

        // 1. 삭제 대상 필터링 및 처리 위임
        List<NoticeAttachment> toRemove = notice.getAttachments().stream()
                .filter(att -> !command.getRemainAttachmentIds().contains(att.getId()))
                .toList();

        noticeFileService.removeFiles(toRemove, notice);

        // 2. 신규 파일 저장 위임
        noticeFileService.storeFiles(command.getNewAttachments(), notice);

        log.info("공지사항 수정 완료: id={}", noticeId);
        // @Transactional에 의해 별도의 save() 호출 없이도 변경사항이 DB에 반영(Dirty Checking)됩니다.
    }


    /**
     * 공지사항을 삭제한다.
     * <p>
     * {@link CascadeType#ALL} 및 {@code orphanRemoval = true} 설정에 의해
     * 해당 공지사항과 연관된 모든 첨부파일(NoticeAttachment) 데이터도 함께 삭제된다.
     *
     * @param noticeId 삭제할 공지사항의 식별자
     * @throws NoticeNotFoundException 존재하지 않는 ID일 경우 발생
     */
    public void deleteNotice(Long noticeId) {
        log.info("공지사항 삭제 요청: id={}", noticeId);

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> {
                    log.error("공지사항 삭제 실패: 존재하지 않는 ID = {}", noticeId);
                    return new NoticeNotFoundException(noticeId);
                });

        noticeFileService.deleteAllFiles(notice);

        noticeRepository.delete(notice);

        log.info("공지사항 삭제 완료: id={}", noticeId);
    }
}
