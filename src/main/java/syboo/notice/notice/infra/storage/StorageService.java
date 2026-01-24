package syboo.notice.notice.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * 파일을 저장하고 저장된 상대 경로를 반환한다.
     */
    void store(MultipartFile file, String storedFileName);

    /**
     * 저장된 파일을 삭제한다.
     */
    void delete(String storedFileName);
}