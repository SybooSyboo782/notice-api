package syboo.notice.notice.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("공지사항 등록 성공 - 첨부파일 포함")
    void createNotice() {
        // given
        CreateNoticeCommand command = new CreateNoticeCommand(
                "공지 제목",
                "공지 내용",
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of()
        );

        Notice savedNotice = Notice.builder()
                .title(command.getTitle())
                .content(command.getContent())
                .author(command.getAuthor())
                .noticeStartAt(command.getNoticeStartAt())
                .noticeEndAt(command.getNoticeEndAt())
                .build();

        ReflectionTestUtils.setField(savedNotice, "id", 1L);

        given(noticeRepository.save(any(Notice.class)))
                .willReturn(savedNotice);

        // when
        Long noticeId = noticeService.createNotice(command);

        // then
        assertThat(noticeId).isEqualTo(1L);
    }

    @Test
    @DisplayName("공지 종료일이 시작일보다 빠르면 실패한다")
    void createNotice_fail_when_notice_end_before_start() {
        // given
        CreateNoticeCommand command = new CreateNoticeCommand(
                "제목",
                "내용",
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // when & then
        assertThatThrownBy(() -> noticeService.createNotice(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공지 종료일");

        // 실패 시 DB 저장이 절대 호출되지 않았음을 확인 (대용량/성능 관점)
        verify(noticeRepository, never()).save(any());
    }
}