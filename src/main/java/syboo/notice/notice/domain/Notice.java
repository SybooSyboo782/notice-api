package syboo.notice.notice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import syboo.notice.common.domain.baseentity.BaseEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notices", indexes = {
        @Index(name = "idx_notice_created_at", columnList = "createdAt"),
        @Index(name = "idx_notice_title", columnList = "title")
})
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private String author;

    @Column(nullable = false)
    private LocalDateTime noticeStartAt;

    @Column(nullable = false)
    private LocalDateTime noticeEndAt;

    @Column(nullable = false)
    private long viewCount = 0L;

    @Column(nullable = false)
    private boolean hasAttachment = false;

    @Version
    private Long version;

    @OneToMany(mappedBy = "notice",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private final List<NoticeAttachment> attachments = new ArrayList<>();

    @Builder
    private Notice(
            String title,
            String content,
            String author,
            LocalDateTime noticeStartAt,
            LocalDateTime noticeEndAt
    ) {
        validateNoticePeriod(noticeStartAt, noticeEndAt);
        this.title = title;
        this.content = content;
        this.author = author;
        this.noticeStartAt = noticeStartAt;
        this.noticeEndAt = noticeEndAt;
        this.hasAttachment = false;
    }

    public void update(
            String title,
            String content,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validateNoticePeriod(startAt, endAt);
        this.title = title;
        this.content = content;
        this.noticeStartAt = startAt;
        this.noticeEndAt = endAt;
    }

    private void validateNoticePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("공지 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void addAttachment(NoticeAttachment attachment) {
        attachments.add(attachment);
        attachment.assignNotice(this);
        updateAttachmentStatus();
    }

    public void removeAttachment(NoticeAttachment attachment) {
        attachments.remove(attachment);
        attachment.assignNotice(null);
        updateAttachmentStatus();
    }

    public void removeAllAttachments() {
        attachments.clear();
        updateAttachmentStatus();
    }

    /**
     * 현재 시점을 기준으로 해당 공지사항이 조회 가능한 기간인지 확인한다.
     * <p>
     * 시작일시 <= 현재시간 <= 종료일시
     * </p>
     * @param clock 시간 측정을 위한 Clock 객체
     * @return 조회 가능 여부
     */
    public boolean isViewable(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return (noticeStartAt == null || !now.isBefore(noticeStartAt))
                && (noticeEndAt == null || !now.isAfter(noticeEndAt));
    }

    /**
     * 첨부파일 리스트의 상태를 확인하여 hasAttachment 필드를 동기화한다.
     */
    private void updateAttachmentStatus() {
        this.hasAttachment = !this.attachments.isEmpty();
    }
}
