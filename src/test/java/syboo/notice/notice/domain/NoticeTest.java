package syboo.notice.notice.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {
    @Test
    void create_notice_success() {
        // given
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusDays(1);

        // when
        Notice notice = Notice.builder()
                .title("title")
                .content("content")
                .author("admin")
                .noticeStartAt(startAt)
                .noticeEndAt(endAt)
                .build();

        // then
        assertThat(notice.getTitle()).isEqualTo("title");
        assertThat(notice.getViewCount()).isZero();
    }

    @Test
    void create_notice_fail_when_end_before_start() {
        // given
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.minusDays(1);

        // when & then
        assertThatThrownBy(() ->
                Notice.builder()
                        .title("title")
                        .content("content")
                        .author("admin")
                        .noticeStartAt(startAt)
                        .noticeEndAt(endAt)
                        .build()
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_notice_success() {
        // given
        Notice notice = createNotice();

        // when
        notice.update(
                "new title",
                "new content",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        // then
        assertThat(notice.getTitle()).isEqualTo("new title");
    }

    @Test
    void increase_view_count() {
        // given
        Notice notice = createNotice();

        // when
        notice.increaseViewCount();
        notice.increaseViewCount();

        // then
        assertThat(notice.getViewCount()).isEqualTo(2);
    }

    @Test
    void add_attachment() {
        // given
        Notice notice = createNotice();
        NoticeAttachment attachment = createAttachment();

        // when
        notice.addAttachment(attachment);

        // then
        assertThat(notice.getAttachments()).hasSize(1);
    }

    @Test
    void remove_attachment() {
        // given
        Notice notice = createNotice();
        NoticeAttachment attachment = createAttachment();
        notice.addAttachment(attachment);

        // when
        notice.removeAttachment(attachment);

        // then
        assertThat(notice.getAttachments()).isEmpty();
    }

    @Test
    void remove_all_attachments() {
        // given
        Notice notice = createNotice();
        notice.addAttachment(createAttachment());
        notice.addAttachment(createAttachment());

        // when
        notice.removeAllAttachments();

        // then
        assertThat(notice.getAttachments()).isEmpty();
    }

    private Notice createNotice() {
        return Notice.builder()
                .title("title")
                .content("content")
                .author("admin")
                .noticeStartAt(LocalDateTime.now())
                .noticeEndAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private NoticeAttachment createAttachment() {
        return NoticeAttachment.builder()
                .fileName("file.txt")
                .storedPath("/files/file.txt")
                .fileSize(100L)
                .build();
    }

}