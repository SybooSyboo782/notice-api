package syboo.notice.notice.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {
    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 25, 20, 0);

    @Test
    void create_notice_success() {
        // given
        LocalDateTime startAt = fixedNow;
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
        LocalDateTime startAt = fixedNow;
        LocalDateTime endAt = startAt.minusDays(1);

        Notice.NoticeBuilder noticeBuilder = Notice.builder()
                .title("title")
                .content("content")
                .author("admin")
                .noticeStartAt(startAt)
                .noticeEndAt(endAt);

        // when & then
        assertThatThrownBy(noticeBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공지 종료일");
    }

    @Test
    void update_notice_success() {
        // given
        Notice notice = createNotice();

        // when
        notice.update(
                "new title",
                "new content",
                fixedNow,
                fixedNow.plusDays(1)
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
                .noticeStartAt(fixedNow)
                .noticeEndAt(fixedNow.plusDays(1))
                .build();
    }

    private NoticeAttachment createAttachment() {
        return NoticeAttachment.builder()
                .originFileName("file.txt")           // fileName -> originFileName 변경
                .storedFileName(UUID.randomUUID() + "_file.txt") // 서버 저장용 고유 이름 추가
                .fileSize(100L)
                .contentType("text/plain")            // 파일 타입 추가
                .checksum("SHA256_EXAMPLE_HASH_123")  // 무결성 검증용 해시 추가
                .build();
    }

}