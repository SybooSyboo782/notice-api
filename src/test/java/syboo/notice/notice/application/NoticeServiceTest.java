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
import java.util.Optional;

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

    @Test
    @DisplayName("공지사항 수정 성공 - 첨부파일 포함")
    void updateNotice_success() {
        // given
        Long noticeId = 1L;

        Notice notice = Notice.builder()
                .title("기존 제목")
                .content("기존 내용")
                .author("admin")
                .noticeStartAt(LocalDateTime.now())
                .noticeEndAt(LocalDateTime.now().plusDays(1))
                .build();

        ReflectionTestUtils.setField(notice, "id", noticeId);

        given(noticeRepository.findById(noticeId))
                .willReturn(Optional.of(notice));

        UpdateNoticeCommand command = new UpdateNoticeCommand(
                noticeId,
                "수정된 제목",
                "수정된 내용",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                List.of(
                        new UpdateNoticeCommand.AttachmentCommand(
                                "file.txt",
                                "/files/file.txt",
                                1024L
                        )
                )
        );

        // when
        noticeService.updateNotice(noticeId, command);

        // then
        assertThat(notice.getTitle()).isEqualTo("수정된 제목");
        assertThat(notice.getAttachments()).hasSize(1);

        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 공지사항을 수정하면 예외가 발생한다")
    void updateNotice_fail_whenNoticeNotFound() {
        // given
        Long noticeId = 999L;

        given(noticeRepository.findById(noticeId))
                .willReturn(Optional.empty());

        UpdateNoticeCommand command = new UpdateNoticeCommand(
                noticeId,
                "제목",
                "내용",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of()
        );

        // when & then
        assertThatThrownBy(() -> noticeService.updateNotice(noticeId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공지사항이 존재하지 않습니다.");

        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("공지사항을 정상적으로 삭제한다")
    void deleteNotice_success() {
        // given
        Long noticeId = 1L;

        Notice notice = Notice.builder()
                .title("공지")
                .content("내용")
                .author("admin")
                .noticeStartAt(LocalDateTime.now())
                .noticeEndAt(LocalDateTime.now().plusDays(1))
                .build();

        ReflectionTestUtils.setField(notice, "id", noticeId);

        given(noticeRepository.findById(noticeId))
                .willReturn(Optional.of(notice));

        // when
        noticeService.deleteNotice(noticeId);

        // then
        verify(noticeRepository).delete(notice);
    }

    @Test
    @DisplayName("존재하지 않는 공지사항을 삭제하면 예외가 발생한다")
    void deleteNotice_fail_whenNoticeNotFound() {
        // given
        Long noticeId = 999L;

        given(noticeRepository.findById(noticeId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteNotice(noticeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공지사항이 존재하지 않습니다.");

        verify(noticeRepository, never()).delete(any());
    }

}