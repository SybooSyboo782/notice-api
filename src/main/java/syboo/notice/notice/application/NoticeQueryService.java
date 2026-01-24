package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

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
}