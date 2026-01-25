package syboo.notice.notice.infra.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.common.exception.FileException;

import java.security.MessageDigest;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChecksumGenerator {

    /**
     * 파일 콘텐츠를 기반으로 SHA-256 체크섬을 생성합니다.
     * <p>
     * <b>성능 참고:</b> 현재 방식은 {@code file.getBytes()}를 사용하여 파일 전체를 메모리에 올림
     *      * 향후 대용량 파일(예: 100MB 이상) 대응이 필요할 경우, 메모리 점유율을 낮추기 위해
     *      * {@code DigestInputStream}을 이용한 스트림 방식의 해싱으로 개선 필요
     * </p>
     * * @param file 해시를 생성할 MultipartFile 객체
     * @return 16진수로 변환된 SHA-256 해시 문자열
     * @throws FileBaseException 해시 생성 알고리즘이 없거나 파일 접근에 실패할 경우
     */
    public static String generate(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());

            // 바이트를 16진수 문자열로 변환
            return byteToHex(hash);
        } catch (Exception e) {
            throw new FileException("체크섬 생성 실패", e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환합니다.
     */
    private static String byteToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
