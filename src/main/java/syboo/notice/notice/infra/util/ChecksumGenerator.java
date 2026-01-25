package syboo.notice.notice.infra.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.common.exception.FileException;
import syboo.notice.common.exception.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChecksumGenerator {

    /**
     * 업로드 시점: MultipartFile 콘텐츠를 스트림 방식으로 읽어 SHA-256 체크섬을 생성합니다.
     * <p>
     * <b>성능 최적화:</b> 내부적으로 8KB 버퍼를 사용하는 스트림 해싱을 수행하므로,
     * 대용량 파일 업로드 시에도 서버의 힙(Heap) 메모리 점유율을 낮게 유지합니다.
     * </p>
     * @param file 해시를 생성할 MultipartFile 객체
     * @return 16진수로 변환된 SHA-256 해시 문자열
     * @throws FileException 해시 생성 알고리즘이 없거나 리소스 읽기에 실패할 경우 발생
     */
    public static String generate(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return calculateHash(is);
        } catch (Exception e) {
            throw new FileStorageException("파일 콘텐츠를 읽는 중 서버 오류가 발생했습니다.", e);
        }
    }



    /**
     * 다운로드 시점: Resource로부터 체크섬 생성
     * <p>
     * <b>성능 최적화:</b> 8KB 버퍼를 사용하는 스트림 방식을 채택하여,
     * 대용량 파일 처리 시에도 낮은 메모리 점유율을 유지합니다.
     * </p>
     * @param resource 해시를 생성할 Resource 객체 (파일 시스템, URL 등)
     * @return 16진수로 변환된 SHA-256 해시 문자열
     * @throws FileException 해시 생성 알고리즘이 없거나 리소스 읽기에 실패할 경우 발생
     */
    public static String generate(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return calculateHash(is);
        } catch (Exception e) {
            throw new FileStorageException("저장된 리소스를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공통 해시 계산 로직 (중복 제거)
     */
    private static String calculateHash(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return byteToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("SHA-256 알고리즘 실행 중 오류 발생", e);
        } catch (IOException e) {
            throw new FileStorageException("파일을 읽는 중 시스템 오류가 발생했습니다.", e);
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
