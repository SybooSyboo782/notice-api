package syboo.notice.notice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import syboo.notice.common.domain.baseentity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_attachments", indexes = {
        @Index(name = "idx_attachment_stored_name", columnList = "storedFileName"),
        @Index(name = "idx_attachment_notice_id", columnList = "notice_id")
})
public class NoticeAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originFileName;

    @Column(nullable = false, unique = true)
    private String storedFileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String contentType;

    // 파일 정합성 검증용 SHA-256 해시값
    @Column(nullable = false)
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Builder
    private NoticeAttachment(
            String originFileName,
            String storedFileName,
            long fileSize,
            String contentType,
            String checksum
    ) {
        this.originFileName = originFileName;
        this.storedFileName = storedFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.checksum = checksum;
    }

    /**
     * 연관관계 편의 메서드
     * Notice 엔티티에서 호출하여 양방향 관계를 설정합니다.
     */
    void assignNotice(Notice notice) {
        this.notice = notice;
    }
}