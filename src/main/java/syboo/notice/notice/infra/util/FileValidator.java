package syboo.notice.notice.infra.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.common.exception.FileException;
import syboo.notice.common.exception.FileInvalidException;
import syboo.notice.common.exception.FileSecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "application/pdf");
    private static final Tika TIKA = new Tika();

    /**
     * 파일의 존재 여부와 실제 MIME 타입을 검증한 후, 분석된 타입을 반환합니다.
     *
     * @param file 검증할 MultipartFile 객체
     * @return 분석된 실제 MIME 타입 (e.g., "image/jpeg")
     * @throws FileInvalidException 파일이 비어있거나 존재하지 않을 경우
     * @throws FileSecurityException 허용되지 않는 파일 형식이거나 위변조가 의심될 경우
     * @throws FileBaseException 파일 분석 중 시스템 오류가 발생할 경우
     */
    public String validateAndReturnMimeType(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileInvalidException("파일이 비어있음");
        }

        try (InputStream is = file.getInputStream()) {
            // Apache Tika를 활용한 실제 파일 헤더(Magic Number) 분석
            String detectedMimeType = TIKA.detect(is);

            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                log.warn("허용되지 않는 파일 형식 시도: {}", detectedMimeType);
                throw new FileSecurityException("지원하지 않는 파일 형식이거나 위변조가 의심됨: " + detectedMimeType);
            }

            return detectedMimeType;
        } catch (IOException e) {
            // 물리 삭제 실패가 DB 트랜잭션 전체 롤백으로 이어지지 않도록 로그만 남김
            throw new FileException("파일 읽기 중 오류 발생", e);
        }
    }
}