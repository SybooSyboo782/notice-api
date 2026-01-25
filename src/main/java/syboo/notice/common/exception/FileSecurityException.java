package syboo.notice.common.exception;

/**
 * 파일 위변조, 허용되지 않은 MIME 타입 등 보안 정책 위반 시 발생하는 예외입니다.
 */
public class FileSecurityException extends FileException {
    public FileSecurityException(String message) { super(message); }
}