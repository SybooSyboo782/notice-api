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
                        combineSearch(cond.query(), cond.searchType()),
                        dateBetween(cond.startDate(), cond.endDate())
                );

        for (Sort.Order o : pageable.getSort()) {
            Order direction = o.isAscending() ? Order.ASC : Order.DESC;

            OrderSpecifier<?> orderSpecifier = switch (o.getProperty()) {
                case "id" -> new OrderSpecifier<>(direction, notice.id);
                case "title" -> new OrderSpecifier<>(direction, notice.title);
                case "viewCount" -> new OrderSpecifier<>(direction, notice.viewCount);
                case "createdDate" -> new OrderSpecifier<>(direction, notice.createdDate);
                default -> {
                    log.warn("허용되지 않은 정렬 필드 요청 차단됨: {}", o.getProperty());
                    yield null; // 허용되지 않은 필드는 무시
                }
            };

            if (orderSpecifier != null) {
                query.orderBy(orderSpecifier);
            }
        }

        // 정렬 조건이 없거나 잘못된 경우 기본 정렬 추가 (최신순)
        query.orderBy(notice.createdDate.desc(), notice.id.desc());

        List<NoticeListResponse> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리 (첫 페이지이면서 페이지 사이즈보다 적은 데이터일 경우 쿼리 생략됨)
        JPAQuery<Long> countQuery = queryFactory
                .select(notice.count())
                .from(notice)
                .where(
                        combineSearch(cond.query(), cond.searchType()),
                        dateBetween(cond.startDate(), cond.endDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 제목과 내용 검색 로직 통합
     * - 제목과 내용 파라미터가 모두 전달되면 (제목 OR 내용) 조건으로 검색 (요구사항: 제목+내용)
     * - 제목만 전달되면 제목에서만 검색
     */
    private BooleanExpression combineSearch(String query, String searchType) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        // "제목 + 내용" 검색
        if ("TITLE_CONTENT".equals(searchType)) {
            return notice.title.contains(query).or(notice.content.contains(query));
        }

        // 기본값: "제목" 검색
        return notice.title.contains(query);
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
