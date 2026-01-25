package syboo.notice.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import syboo.notice.common.response.ErrorResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice // 전역 예외 처리기
public class GlobalExceptionHandler {

    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String SERVER_ERROR_MESSAGE = "서버 이용 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의하세요.";

    /**
     * [400] @Valid 검증 실패 시 상세 메시지 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("입력값 검증 실패: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("VALIDATION_FAILED", errors));
    }

    /**
     * [400] 비즈니스 로직 제약 조건 위반 시 발생한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("비즈니스 로직 위반 발생: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("INVALID_INPUT_VALUE", e.getMessage()));
    }

    /**
     * [400] 핸들러 메서드 유효성 검사 실패 시 발생한다.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.warn("유효성 검사 실패: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_INPUT", "입력 파라미터가 유효하지 않습니다."));
    }

    /**
     * [403] 보안 정책 위반 파일을 감지했을 때 발생한다.
     */
    @ExceptionHandler(FileSecurityException.class)
    public ResponseEntity<ErrorResponse> handleFileSecurityException(FileSecurityException e) {
        log.warn("보안 정책 위반 파일 감지: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FILE_SECURITY_VIOLATION", e.getMessage()));
    }

    /**
     * [404] 리소스(공지사항, 파일 등)를 찾을 수 없을 때 발생한다.
     */
    @ExceptionHandler({NoticeNotFoundException.class, FileInvalidException.class})
    protected ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException e) {
        log.warn("리소스 조회 실패: {}", e.getMessage());
        String code = (e instanceof NoticeNotFoundException) ? "NOT_FOUND" : "FILE_NOT_FOUND";
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    /**
     * [409] 동시성 수정 충돌 발생 시 발생한다.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    protected ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e) {
        log.error("동시 수정 충돌 발생: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONCURRENCY_CONFLICT", "다른 사용자에 의해 수정된 데이터입니다. 새로고침 후 다시 시도해주세요."));
    }

    /**
     * [413] 파일 용량 제한을 초과했을 때 발생한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ErrorResponse.of("FILE_TOO_LARGE", "업로드 가능한 최대 파일 용량을 초과했습니다."));
    }

    /**
     * [500] 파일 저장 프로세스 중 물리적 오류 발생 (디스크 풀, 권한 부족 등)
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException e) {
        // 1. 관리자를 위한 상세 로그 (물리적 장애 상황)
        log.error("파일 시스템 물리적 오류 발생 (시스템 점검 필요): ", e);

        // 2. 사용자를 위한 응답 (보안상 상세 원인은 숨김)
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE));
    }

    /**
     * [500] 파일 처리 과정에서 발생한 일반적인 서버 내부 오류
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<ErrorResponse> handleFileException(FileException e) {
        // 1. 비즈니스 로직 예외 로그
        log.warn("정의되지 않은 파일 예외 발생: {}", e.getMessage());

        // 2. 사용자를 위한 응답
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE));
    }

    /**
     * [500] 그 외 예상치 못한 모든 서버 내부 오류
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 1. 미처리 예외에 대한 추적용 로그
        log.error("예상치 못한 서버 내부 오류 발생: ", e);

        // 2. 사용자를 위한 응답
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE));
    }
}