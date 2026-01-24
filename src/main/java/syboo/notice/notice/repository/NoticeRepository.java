package syboo.notice.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import syboo.notice.notice.domain.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeQueryRepository {

    @Modifying(clearAutomatically = true)
    @Query("update Notice n set n.viewCount = n.viewCount + 1 where n.id = :id")
    void updateViewCount(@Param("id") Long id);
}
