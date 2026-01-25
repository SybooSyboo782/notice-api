package syboo.notice.common.exception;

/**
 * 파일 저장소(로컬 디스크, S3 등)와의 상호작용 중 발생하는 물리적 오류를 나타냅니다.
 */
public class FileStorageException extends FileException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}