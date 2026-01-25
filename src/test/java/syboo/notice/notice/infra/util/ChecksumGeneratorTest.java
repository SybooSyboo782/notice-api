package syboo.notice.notice.infra.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class ChecksumGeneratorTest {

    @Test
    @DisplayName("동일한 내용의 파일은 항상 같은 체크섬을 생성한다")
    void generateChecksumSuccess() {
        // given
        byte[] content = "file content for checksum".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("file", "test1.txt", "text/plain", content);
        MockMultipartFile file2 = new MockMultipartFile("file", "test2.txt", "text/plain", content);

        // when
        String hash1 = ChecksumGenerator.generate(file1);
        String hash2 = ChecksumGenerator.generate(file2);

        // then
        assertThat(hash1)
                .isEqualTo(hash2)
                // SHA-256 16진수 문자열은 64자
                .hasSize(64);
    }
}