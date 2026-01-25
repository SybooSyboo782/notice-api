package syboo.notice.notice.infra.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import syboo.notice.common.exception.FileSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    private final FileValidator fileValidator = new FileValidator();

    @Test
    @DisplayName("허용된 MIME 타입(이미지)은 정상적으로 분석되어 반환된다")
    void validateSuccess() {
        // given: 실제 JPEG 헤더를 가진 모의 파일
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        // when
        String mimeType = fileValidator.validateAndReturnMimeType(file);

        // then
        assertThat(mimeType).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("파일 확장자는 이미지지만, 실제 내용이 텍스트인 위변조 파일은 예외가 발생한다")
    void validateSecurityFail() {
        // given: 확장자는 jpg지만 내용은 평범한 문자열인 파일
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file", "malicious.jpg", "image/jpeg", "This is not an image content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidator.validateAndReturnMimeType(spoofedFile))
                .isInstanceOf(FileSecurityException.class)
                .hasMessageContaining("위변조가 의심됨");
    }
}