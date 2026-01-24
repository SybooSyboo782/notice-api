package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.notice.api.request.NoticeSearchCondition;
import syboo.notice.notice.api.response.NoticeDetailResponse;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;

    /**
     * 공지사항 목록을 페이징하여 조회합니다.
     *
     * @param pageable 페이지 번호, 사이즈, 정렬 조건을 포함하는 객체
     * @return 페이징 처리된 공지사항 목록 응답 DTO (NoticeListResponse)
     */
    public Page<NoticeListResponse> getNoticeList(Pageable pageable) {
        log.info("공지사항 목록 조회를 시작합니다. 설정된 페이징 정보: {}", pageable);

        Page<Notice> noticePage = noticeRepository.findAll(pageable);

        log.debug("DB 조회 완료. 전체 데이터 수: {}, 현재 페이지 요소 수: {}",
                noticePage.getTotalElements(), noticePage.getNumberOfElements());

        return noticePage.map(this::toResponse);
    }

    /**
     * 공지사항 검색 조회
     */
    public Page<NoticeListResponse> searchNotices(NoticeSearchCondition condition, Pageable pageable) {
        log.info("공지사항 검색을 시작합니다. 조건: {}, 페이징: {}", condition, pageable);
        return noticeRepository.search(condition, pageable);
    }

    /**
     * Notice 엔티티를 NoticeListResponse DTO로 변환합니다.
     * <p>
     * DTO의 독립성을 유지하기 위해 DTO 내부가 아닌 서비스 레이어에서 매핑을 수행합니다.
     * </p>
     *
     * @param notice 변환할 공지사항 엔티티
     * @return 변환된 공지사항 목록 응답 DTO
     */
    private NoticeListResponse toResponse(Notice notice) {
        return new NoticeListResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getAuthor(),
                notice.getCreatedDate(),
                notice.getViewCount(),
                notice.isHasAttachment()
        );
    }

    /**
     * 공지사항 상세 정보를 조회합니다.
     *
     * @param id 조회할 공지사항 ID
     * @return 공지사항 상세 응답 DTO
     * @throws IllegalArgumentException 존재하지 않는 ID일 경우 발생
     */
    @Transactional
    public NoticeDetailResponse getNoticeDetail(Long id) {
        log.info("공지사항 상세 조회 요청 - ID: {}", id);

        // 조회수 증가
        // ⚠️ 현재는 단일 DB 업데이트 방식
        // 대규모 트래픽 환경에서는 Redis/벌크 업데이트 등 CQRS 분리 가능성을 고려
        noticeRepository.updateViewCount(id);

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("공지사항을 찾을 수 없습니다. ID: {}", id);
                    return new IllegalArgumentException("해당 공지사항이 존재하지 않습니다. ID: " + id);
                });

        return toDetailResponse(notice);
    }

    private NoticeDetailResponse toDetailResponse(Notice notice) {
        List<NoticeDetailResponse.AttachmentResponse> attachments = notice.getAttachments().stream()
                .map(attachment -> new NoticeDetailResponse.AttachmentResponse(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getStoredPath(),
                        attachment.getFileSize()
                )).toList();

        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthor(),
                notice.getCreatedDate(),
                notice.getViewCount(),
                attachments
        );
    }
}