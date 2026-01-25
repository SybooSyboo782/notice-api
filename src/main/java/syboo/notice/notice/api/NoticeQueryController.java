package syboo.notice.notice.api;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import syboo.notice.notice.api.request.NoticeSearchCondition;
import syboo.notice.notice.api.response.NoticeDetailResponse;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.application.NoticeQueryService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeQueryController {

    private final NoticeQueryService noticeQueryService;

    /**
     * 공지사항 목록을 페이징하여 조회합니다.
     * <p>
     * 기본적으로 최신 등록순(createdDate DESC)으로 정렬되며, 한 페이지당 10개의 데이터를 반환합니다.
     * </p>
     *
     * @param pageable 페이징 및 정렬 정보 (기본값: 10개, createdDate 내림차순)
     * @return 페이징 처리된 공지사항 목록 응답
     */
    @GetMapping
    public ResponseEntity<Page<NoticeListResponse>> getNotices(
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)Pageable pageable) {

        log.info("공지사항 목록 조회 API 호출 - Page: {}, Size: {}, Sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<NoticeListResponse> responses = noticeQueryService.getNoticeList(pageable);

        // ResponseEntity를 사용하여 HTTP 상태 코드(200 OK)를 명시적으로 반환
        return ResponseEntity.ok(responses);
    }

    /**
     * 공지사항 검색 목록 조회 API
     * GET /api/notices/search?title=공지&startDate=2026-01-01T00:00:00...
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeListResponse>> search(
            NoticeSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NoticeListResponse> responses = noticeQueryService.searchNotices(condition, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * 공지사항 상세 정보를 조회합니다.
     *
     * @param id 공지사항 ID
     * @return 공지사항 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoticeDetailResponse> getNotice(@PathVariable @Min(1) Long id) {
        log.info("공지사항 상세 조회 API 호출 - ID: {}", id);
        return ResponseEntity.ok(noticeQueryService.getNoticeDetail(id));
    }
}