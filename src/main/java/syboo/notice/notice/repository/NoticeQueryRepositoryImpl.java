package syboo.notice.notice.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import syboo.notice.notice.api.request.NoticeSearchCondition;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.domain.Notice;

import java.time.LocalDateTime;
import java.util.List;

import static syboo.notice.notice.domain.QNotice.notice;

@Slf4j
@RequiredArgsConstructor
public class NoticeQueryRepositoryImpl implements NoticeQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 제목, 내용, 등록일자
    @Override
    public Page<NoticeListResponse> search(NoticeSearchCondition condition, Pageable pageable) {
        log.debug("Notice search started with condition: {}", condition);
        NoticeSearchCondition cond = (condition != null) ? condition : new NoticeSearchCondition(null, null, null, null);

        // 1. 컨텐츠 조회 (DTO 직접 조회로 메모리 절약)
        JPAQuery<NoticeListResponse> query = queryFactory
                .select(Projections.constructor(NoticeListResponse.class,
                        notice.id,
                        notice.title,
                        notice.author,
                        notice.createdDate,
                        notice.viewCount,
                        notice.hasAttachment
                ))
                .from(notice)
                .where(
                        combineSearch(cond.title(), cond.content()),
                        dateBetween(cond.startDate(), cond.endDate())
                );

        for (Sort.Order o : pageable.getSort()) {
            PathBuilder<Notice> pathBuilder = new PathBuilder<>(notice.getType(), notice.getMetadata());
            query.orderBy(new OrderSpecifier(
                    o.isAscending() ? Order.ASC : Order.DESC,
                    pathBuilder.get(o.getProperty())
            ));
        }

        List<NoticeListResponse> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리 (첫 페이지이면서 페이지 사이즈보다 적은 데이터일 경우 쿼리 생략됨)
        JPAQuery<Long> countQuery = queryFactory
                .select(notice.count())
                .from(notice)
                .where(
                        combineSearch(cond.title(), cond.content()),
                        dateBetween(cond.startDate(), cond.endDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 제목과 내용 검색 로직 통합
     * - 제목과 내용 파라미터가 모두 전달되면 (제목 OR 내용) 조건으로 검색 (요구사항: 제목+내용)
     * - 제목만 전달되면 제목에서만 검색
     */
    private BooleanExpression combineSearch(String title, String content) {
        boolean hasTitle = StringUtils.hasText(title);
        boolean hasContent = StringUtils.hasText(content);

        if (hasTitle && hasContent) {
            // "제목 + 내용" 검색 (OR 조건)
            return notice.title.contains(title).or(notice.content.contains(content));
        }
        if (hasTitle) {
            return notice.title.contains(title);
        }
        if (hasContent) {
            return notice.content.contains(content);
        }
        return null;
    }

    private BooleanExpression dateBetween(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return notice.createdDate.between(start, end);
        }
        if (start != null) {
            return notice.createdDate.goe(start);
        }
        if (end != null) {
            return notice.createdDate.loe(end);
        }
        return null;

    }
}
