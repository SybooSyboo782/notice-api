package syboo.notice.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import syboo.notice.notice.domain.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
