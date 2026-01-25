package syboo.notice.common.exception;

public class NoticeNotFoundException extends RuntimeException {
    public NoticeNotFoundException(Long id) {
        super("공지사항이 존재하지 않습니다. id=" + id);
    }
}
