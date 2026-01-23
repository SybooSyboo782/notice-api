package syboo.notice.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import syboo.notice.notice.domain.NoticeAttachment;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {
}