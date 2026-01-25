package syboo.notice.notice.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import syboo.notice.IntegrationTestSupport;
import syboo.notice.config.TestClockConfig;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeQueryControllerTest extends IntegrationTestSupport {
    @Autowired
    private NoticeRepository noticeRepository;


    @BeforeEach
    void setUp() {
        // 1. Given: 테스트 데이터 11개 대량 생성
        for (int i = 1; i <= 11; i++) {
            noticeRepository.save(Notice.builder()
                    .title("테스트 제목 " + i)
                    .content("테스트 내용 " + i)
                    .author("tester")
                    .noticeStartAt(TestClockConfig.FIXED_NOW)
                    .noticeEndAt(TestClockConfig.FIXED_NOW.plusDays(7))
                    .build());
        }
    }

    @Test
    @DisplayName("해피 케이스: 공지사항 목록을 페이징하여 조회한다")
    void getNotices_Pagination_Success() throws Exception {
        // Given
        // When & Then: 0번 페이지, 10개씩 조회 시도
        mockMvc.perform(get("/api/notices")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc") // 최신순 정렬
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // 응답 데이터가 10개인지 확인
                .andExpect(jsonPath("$.content.length()").value(10))

                // 전체 데이터 개수가 11개인지 확인 (Spring Data Page 객체 기준)
                .andExpect(jsonPath("$.page.totalElements").value(11))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.content[0].title").value("테스트 제목 11"));
    }

    @Test
    @DisplayName("해피 케이스: 제목 검색 조건(기본 타입)으로 공지사항을 필터링하여 조회한다")
    void searchNotices_ByTitle_Success() throws Exception {
        // Given
        // When & Then
        mockMvc.perform(get("/api/notices/search")
                        .param("query", "11")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("테스트 제목 11"));
    }

    @Test
    @DisplayName("해피 케이스: 제목 + 내용 검색 조건으로 공지사항을 필터링하여 조회한다")
    void searchNotices_ByContent_Success() throws Exception {
        // Given
        // When & Then
        mockMvc.perform(get("/api/notices/search")
                        .param("query", "테스트 내용 11")
                        .param("searchType", "TITLE_CONTENT") // 통합 검색 타입
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("테스트 제목 11"));
    }

    @Test
    @DisplayName("해피 케이스: 상세 조회 시 제목, 내용, 등록일시, 조회수, 작성자, 첨부파일이 모두 포함되고 조회수가 증가한다")
    void getNotice_IncreaseViewCount() throws Exception {
        // Given: setUp에서 생성된 데이터 중 하나를 선택
        Notice targetNotice = noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).getLast();
        Long targetId = targetNotice.getId();

        // When: 두 번 조회
        mockMvc.perform(get("/api/notices/" + targetId));
        mockMvc.perform(get("/api/notices/" + targetId));

        // Then: 조회수가 3가 되어야 함
        mockMvc.perform(get("/api/notices/" + targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.createdDate").exists())
                .andExpect(jsonPath("$.author").exists())
                .andExpect(jsonPath("$.attachments").isArray())
                .andExpect(jsonPath("$.viewCount").value(3));
    }

    @Test
    @DisplayName("해피 케이스: 등록일자 검색 기간을 조절하여 데이터 필터링을 검증한다")
    void searchByCreatedDateRange_Relative_Success() throws Exception {
        // 1. Given: setUp에서 이미 오늘(TestClockConfig.FIXED_NOW) 날짜로 11개의 데이터가 생성됨

        // 2. Scenario A: 검색 범위를 '오늘 전체'로 설정 (11개 모두 나와야 함)
        LocalDateTime startOfToday = TestClockConfig.FIXED_NOW.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        mockMvc.perform(get("/api/notices/search")
                        .param("startDate", startOfToday.toString())
                        .param("endDate", endOfToday.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(11))
                .andDo(print());

        // 3. Scenario B: 검색 범위를 '어제'로 설정 (데이터가 하나도 나오지 않아야 함)
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime endOfYesterday = endOfToday.minusDays(1);

        mockMvc.perform(get("/api/notices/search")
                        .param("startDate", startOfYesterday.toString())
                        .param("endDate", endOfYesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0)) // 0개 검증
                .andExpect(jsonPath("$.content").isEmpty());

        // 4. Scenario C: 검색 범위를 '내일'로 설정 (데이터가 하나도 나오지 않아야 함)
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        LocalDateTime endOfTomorrow = endOfToday.plusDays(1);

        mockMvc.perform(get("/api/notices/search")
                        .param("startDate", startOfTomorrow.toString())
                        .param("endDate", endOfTomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0)); // 0개 검증
    }

    @Test
    @DisplayName("해피 케이스: 존재하지 않는 페이지 번호(범위 초과) 요청 시 빈 목록을 반환한다")
    void getNotices_PageOutOfRange_ReturnsEmpty() throws Exception {
        // When & Then: 100번째 페이지 요청
        mockMvc.perform(get("/api/notices")
                        .param("page", "100")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").hasJsonPath()); // 전체 카운트는 나와야 함
    }

    @Test
    @DisplayName("해피 케이스: 검색어 앞뒤에 공백이 포함되어도 이를 제거(trim)하고 정상적으로 조회한다")
    void searchNotices_WithWhitespace_Success() throws Exception {
        // 1. Given: " 11 " 처럼 앞뒤에 공백이 섞인 쿼리를 준비합니다.
        String queryWithWhitespace = "  11  ";

        // 2. When & Then
        mockMvc.perform(get("/api/notices/search")
                        .param("query", queryWithWhitespace)
                        .param("searchType", "TITLE") // 제목 검색 기준
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // 공백이 제거되어 "테스트 제목 11"이 검색되어야 함 (기존 setUp 데이터 기준)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("테스트 제목 11"));
    }

    @Test
    @DisplayName("예외 케이스: 존재하지 않는 ID로 상세 조회 시 404 에러를 반환한다")
    void getNotice_NotFound_Exception() throws Exception {
        // Given: 존재할 수 없는 큰 ID 값
        long nonExistentId = 999_999L;

        // When & Then
        mockMvc.perform(get("/api/notices/" + nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound());
        // 만약 Custom ExceptionHandler가 있다면 응답 바디의 에러코드도 검증 가능
    }

    @Test
    @DisplayName("예외 케이스: ID 값이 1 미만(0)일 경우 유효성 검증에 실패하여 400 에러를 반환한다")
    void getNotice_InvalidId_Exception() throws Exception {
        // When & Then: @Min(1) 제약 조건 위반
        mockMvc.perform(get("/api/notices/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예외 케이스: 정렬 기준에 존재하지 않는 필드명을 넣을 경우 안전하게 기본 정렬로 대체되거나 에러를 반환한다")
    void getNotices_InvalidSortProperty_Exception() throws Exception {
        // When & Then: 존재하지 않는 'unknownField'로 정렬 요청
        // 현재 Repository 로직상 default 처리가 되어 있다면 200 OK가 나오겠지만,
        // 엄격하게 처리한다면 에러를 기대할 수 있습니다.
        mockMvc.perform(get("/api/notices")
                        .param("sort", "unknownField,desc"))
                .andExpect(status().isOk()); // 현재 Repository switch-default 로직에 따라 정상 동작 확인
    }

    @Test
    @DisplayName("예외 케이스: 시작일이 종료일보다 늦을 경우 검색 결과가 0건을 반환해야 한다")
    void search_DateInversion_Exception() throws Exception {
        // When & Then: 종료일이 시작일보다 앞선 경우
        mockMvc.perform(get("/api/notices/search")
                        .param("startDate", "2026-01-26T00:00:00")
                        .param("endDate", "2026-01-20T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0)); // 논리적으로 0건 반환 확인
    }
}