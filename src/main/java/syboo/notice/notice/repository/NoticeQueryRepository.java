package syboo.notice.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import syboo.notice.notice.api.request.NoticeSearchCondition;
import syboo.notice.notice.api.response.NoticeListResponse;

public interface NoticeQueryRepository {
    Page<NoticeListResponse> search(NoticeSearchCondition condition, Pageable pageable);
}