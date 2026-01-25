package syboo.notice.common.exception;

/**
 * 파일 누락, 잘못된 형식 등 입력 값의 유효성이 떨어질 때 발생하는 예외입니다.
 */
public class FileInvalidException extends FileException {
    public FileInvalidException(String message) { super(message); }
}