package syboo.notice.notice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeQueryService noticeQueryService;

    private List<Notice> savedNotices;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 15개를 미리 생성해둡니다.
        savedNotices = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> createNotice(
                        (long) i,
                        "공지사항 제목 " + i,
                        i % 2 == 0 // 짝수번은 첨부파일 있음, 홀수번은 없음
                ))
                .toList();
    }

    @Test
    @DisplayName("공지사항 목록 조회 시 페이징 정보에 맞게 DTO로 변환되어 반환된다")
    void getNoticeList_PagingSuccess() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());

        // Mockito를 사용하여 15개 중 첫 10개만 포함된 Page 객체 반환 설정
        List<Notice> pagedNotices = savedNotices.subList(0, 10);
        Page<Notice> noticePage = new PageImpl<>(pagedNotices, pageable, savedNotices.size());

        given(noticeRepository.findAll(any(Pageable.class))).willReturn(noticePage);

        // when
        Page<NoticeListResponse> result = noticeQueryService.getNoticeList(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(15); // 전체 개수 확인
        assertThat(result.getContent()).hasSize(10);         // 페이징 사이즈 확인

        // 첫 번째 데이터 상세 검증 (역정규화 필드 반영 확인)
        NoticeListResponse firstResponse = result.getContent().get(0);
        assertThat(firstResponse.title()).isEqualTo("공지사항 제목 1");
        assertThat(firstResponse.hasAttachment()).isFalse(); // 1은 홀수이므로 false

        // 두 번째 데이터 상세 검증 (첨부파일 있는 케이스)
        NoticeListResponse secondResponse = result.getContent().get(1);
        assertThat(secondResponse.hasAttachment()).isTrue(); // 2는 짝수이므로 true
    }

    /**
     * 테스트 데이터 생성을 위한 공통 메서드
     */
    private Notice createNotice(Long id, String title, boolean hasAttachment) {
        Notice notice = Notice.builder()
                .title(title)
                .content("공지 내용입니다.")
                .author("작성자")
                .noticeStartAt(LocalDateTime.now())
                .noticeEndAt(LocalDateTime.now().plusDays(7))
                .build();

        // JPA 가 관리하는 필드들을 Reflection 으로 강제 주입
        ReflectionTestUtils.setField(notice, "id", id);
        ReflectionTestUtils.setField(notice, "hasAttachment", hasAttachment);
        ReflectionTestUtils.setField(notice, "createdDate", LocalDateTime.now().minusHours(id)); // 정렬 테스트를 위해 차등 부여

        return notice;
    }
}