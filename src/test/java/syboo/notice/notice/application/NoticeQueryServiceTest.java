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
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import syboo.notice.common.exception.NoticeNotFoundException;
import syboo.notice.notice.api.request.NoticeSearchCondition;
import syboo.notice.notice.api.response.NoticeDetailResponse;
import syboo.notice.notice.api.response.NoticeListResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.repository.NoticeRepository;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeQueryService noticeQueryService;

    private List<Notice> savedNotices;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 25, 20, 0);

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

    @Test
    @DisplayName("성공: 존재하는 ID로 상세 조회 시 상세 정보와 첨부파일 리스트를 반환한다")
    void getNoticeDetail_Success() {
        // given
        Long noticeId = 2L;
        Notice targetNotice = savedNotices.get(1);

        ReflectionTestUtils.setField(targetNotice, "viewCount", 10L);

        // 첨부파일 강제 주입
        NoticeAttachment attachment = NoticeAttachment.builder()
                .originFileName("file1.txt")
                .storedFileName("uuid-file1.txt")
                .fileSize(123L)
                .contentType("text/plain")
                .checksum("SHA256_ABC123")
                .build();

        ReflectionTestUtils.setField(attachment, "id", 100L);

        targetNotice.addAttachment(attachment);
        ReflectionTestUtils.setField(targetNotice, "viewCount", 10L);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(targetNotice));

        // when
        NoticeDetailResponse result = noticeQueryService.getNoticeDetail(noticeId);

        // then
        assertThat(result.id()).isEqualTo(noticeId);
        assertThat(result.title()).isEqualTo("공지사항 제목 2");

        // 단위 테스트에서는 실제 DB 값이 안 변하므로 호출 여부가 가장 중요함
        verify(noticeRepository, times(1)).updateViewCount(noticeId);

        // 첨부파일 검증
        assertThat(result.attachments()).hasSize(1);
        assertThat(result.attachments().get(0).id()).isEqualTo(100L); // ID 검증 추가 가능
        assertThat(result.attachments().get(0).originFileName()).isEqualTo("file1.txt");
        assertThat(result.attachments().get(0).fileSize()).isEqualTo(123L);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 ID로 상세 조회 시 NoticeNotFoundException 발생한다")
    void getNoticeDetail_Fail_NotFound() {
        // given
        // setUp에서 생성된 ID는 1~15이므로, 절대 존재할 수 없는 ID 999를 사용
        Long nonExistentId = 999L;

        given(noticeRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        // 1. 해당 로직 실행 시 특정 예외가 발생하는지 검증
        // 2. 예외 메시지에 사용자님이 설정한 문구가 포함되어 있는지 확인
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        noticeQueryService.getNoticeDetail(nonExistentId)
                )
                .isInstanceOf(NoticeNotFoundException.class);
    }

    @Test
    @DisplayName("검색 조건으로 조회 시 레포지토리를 호출하고 결과를 반환한다.")
    void searchNotices_Success() {
        // given
        NoticeSearchCondition condition = new NoticeSearchCondition("제목", "", null, null);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());

        // Mock 데이터 생성
        NoticeListResponse response = new NoticeListResponse(
                1L, "테스트 제목", "작성자", fixedNow, 0L, false
        );
        Page<NoticeListResponse> mockPage = new PageImpl<>(List.of(response), pageable, 1);

        // 레포지토리 동작 정의 (Stubbing)
        given(noticeRepository.search(any(NoticeSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

        // when
        Page<NoticeListResponse> result = noticeQueryService.searchNotices(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("테스트 제목");

        // 실제로 레포지토리의 search 메서드가 호출되었는지 확인
        verify(noticeRepository).search(condition, pageable);
    }

    /**
     * 테스트 데이터 생성을 위한 공통 메서드
     */
    private Notice createNotice(Long id, String title, boolean hasAttachment) {
        Notice notice = Notice.builder()
                .title(title)
                .content("공지 내용입니다.")
                .author("작성자")
                .noticeStartAt(fixedNow)
                .noticeEndAt(fixedNow.plusDays(7))
                .build();

        // JPA 가 관리하는 필드들을 Reflection 으로 강제 주입
        ReflectionTestUtils.setField(notice, "id", id);
        ReflectionTestUtils.setField(notice, "hasAttachment", hasAttachment);
        ReflectionTestUtils.setField(notice, "createdDate", fixedNow.minusHours(id)); // 정렬 테스트를 위해 차등 부여

        return notice;
    }
}