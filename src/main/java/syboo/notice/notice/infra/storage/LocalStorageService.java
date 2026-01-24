package syboo.notice.notice.infra.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.common.exception.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalStorageService implements StorageService{
    private final Path rootLocation;

    // 생성자 주입을 통해 yml 설정값을 가져옵니다.
    public LocalStorageService(@Value("${storage.location}") String location) {
        this.rootLocation = Paths.get(location);
    }

    /**
     * 서비스 기동 시점에 디렉토리를 미리 생성한다.
     * 생성자 주입 직후 실행되어 저장소 준비 상태를 확인한다.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
            log.info("파일 저장소 준비 완료: {}", this.rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("파일 저장소 디렉토리 생성 실패: {}", this.rootLocation, e);
            throw new FileStorageException("저장소 초기화에 실패했습니다.", e);
        }
    }

    @Override
    public void store(MultipartFile file, String storedFileName) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("빈 파일은 저장할 수 없습니다.");
            }

            Path rootAbsPath = this.rootLocation.toAbsolutePath().normalize();

            // 저장할 절대 경로 생성
            Path destinationFile = rootAbsPath.resolve(Paths.get(storedFileName))
                    .normalize().toAbsolutePath();

            // 보안 검증: 설정된 경로 밖으로 나가는지 확인 (Path Traversal 방지)
            if (!destinationFile.startsWith(rootAbsPath)) {
                throw new SecurityException("파일 저장 경로가 허용된 범위를 벗어났습니다.");
            }

            // 파일 저장 (기존 파일이 있으면 덮어쓰기)
            Files.copy(
                    file.getInputStream(),
                    destinationFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            log.debug("파일 물리 저장 완료: {}", destinationFile);

        } catch (IOException e) {
            log.error("파일 저장 실패: {}", storedFileName, e);
            throw new FileStorageException("물리 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        try {
            if (storedFileName == null || storedFileName.isBlank()) {
                return;
            }

            Path rootAbsPath = this.rootLocation.toAbsolutePath().normalize();

            Path fileToDelete = rootAbsPath.resolve(Paths.get(storedFileName))
                    .normalize().toAbsolutePath();

            if (!fileToDelete.startsWith(rootAbsPath)) {
                throw new SecurityException("파일 삭제 경로가 허용된 범위를 벗어났습니다.");
            }

            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
                log.info("파일 물리 삭제 완료: {}", storedFileName);
            }
        } catch (IOException e) {
            log.warn("파일 삭제 실패 (파일이 없거나 사용 중일 수 있음): {}", storedFileName);
        }
    }
}
