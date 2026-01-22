package syboo.notice.notice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import syboo.notice.common.domain.baseentity.BaseEntity;

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

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private String author;

    @Column(nullable = false)
    private LocalDateTime noticeStartAt;

    @Column(nullable = false)
    private LocalDateTime noticeEndAt;

    @Column(nullable = false)
    private long viewCount = 0L;

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
    }

    public void removeAttachment(NoticeAttachment attachment) {
        attachments.remove(attachment);
        attachment.assignNotice(null);
    }

    public void removeAllAttachments() {
        attachments.clear();
    }
}
