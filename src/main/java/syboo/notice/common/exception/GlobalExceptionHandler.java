package syboo.notice.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice // 전역 예외 처리기
public class GlobalExceptionHandler {

    public static final String ERROR = "error";
    public static final String MESSAGE = "message";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String SERVER_ERROR_MESSAGE = "서버 이용 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의하세요.";

    /**
     * [400] @Valid 검증 실패 시 상세 메시지 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("입력값 검증 실패: {}", e.getBindingResult().getAllErrors().getFirst().getDefaultMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                ERROR, "VALIDATION_FAILED",
                "details", errors
        ));
    }

    /**
     * [400] 비즈니스 로직 제약 조건 위반 (예: 공지 기간 역전)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("비즈니스 로직 위반 발생: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        ERROR, "BAD_REQUEST",
                        MESSAGE, e.getMessage()
                ));
    }

    /**
     *  [403] 보안 정책 위반 파일을 올렸을 경우
     */
    @ExceptionHandler(FileSecurityException.class)
    public ResponseEntity<Map<String, String>> handleFileSecurityException(FileSecurityException e) {
        log.warn("보안 정책 위반 파일 감지: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        ERROR, "FILE_SECURITY_VIOLATION",
                        MESSAGE, e.getMessage()
                ));
    }

    /**
     * [404] 파일 자체가 존재하지 않거나 잘못된 경로일 경우
     */
    @ExceptionHandler(FileInvalidException.class)
    public ResponseEntity<Map<String, String>> handleFileInvalidException(FileInvalidException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        ERROR, "FILE_NOT_FOUND",
                        MESSAGE, e.getMessage()
                ));
    }

    /**
     * [409] 동시성 수정 충돌 발생 시 (대용량 트래픽 고려)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    protected ResponseEntity<Map<String, String>> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e) {
        log.error("동시 수정 충돌 발생: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        ERROR, "CONCURRENCY_CONFLICT",
                        MESSAGE, "다른 사용자에 의해 수정된 데이터입니다. 새로고침 후 다시 시도해주세요."
                ));
    }

    /**
     * [413] 파일 용량 제한 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of(
                        ERROR, "FILE_TOO_LARGE",
                        MESSAGE, "업로드 가능한 최대 파일 용량을 초과했습니다."
                ));
    }

    /**
     * [500] 파일 저장 프로세스 중 물리적 오류 발생 (디스크 풀, 권한 부족 등)
     * 이 예외는 서버 관리자의 확인이 필요한 중대한 사안입니다.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, String>> handleFileStorageException(FileStorageException e) {
        // 1. 관리자를 위한 로그
        log.error("파일 시스템 물리적 오류 발생: ", e);

        // 2. 사용자를 위한 응답
        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        ERROR, INTERNAL_SERVER_ERROR,
                        MESSAGE, SERVER_ERROR_MESSAGE
                ));
    }

    /**
     * [500] 파일 처리 과정에서 발생한 일반적인 서버 내부 오류
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<Map<String, String>> handleFileException(FileException e) {
        log.warn("정의되지 않은 파일 예외 발생: {}", e.getMessage());

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        ERROR, INTERNAL_SERVER_ERROR,
                        MESSAGE, SERVER_ERROR_MESSAGE
                ));
    }

    /**
     * [500] 그 외 예상치 못한 모든 서버 내부 오류
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("서버 내부 오류 발생: ", e); // 500 에러는 반드시 스택트레이스를 로그로 남겨야 함
        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                ERROR, INTERNAL_SERVER_ERROR,
                MESSAGE, SERVER_ERROR_MESSAGE
        ));
    }
}