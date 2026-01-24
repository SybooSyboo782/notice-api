package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.infra.storage.StorageService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeFileService {

    private final StorageService storageService;

    /**
     * 신규 파일들을 저장하고 NoticeAttachment 엔티티 리스트를 반환한다.
     */
    public void storeFiles(List<MultipartFile> files, Notice notice) {
        if (files == null || files.isEmpty()) {
            return;
        }

        log.info("첨부파일 물리 저장 시작: count={}", files.size());

        for (MultipartFile file : files) {
            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            log.debug("파일 저장 시도: originName='{}', storedName='{}'",
                    file.getOriginalFilename(), storedFileName);

            storageService.store(file, storedFileName);

            notice.addAttachment(NoticeAttachment.builder()
                    .originFileName(file.getOriginalFilename())
                    .storedFileName(storedFileName)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .checksum("SHA256_HASH_PLACEHOLDER") // TODO: 유틸리티 구현 시 교체
                    .build());

            log.info("파일 저장 및 엔티티 매핑 완료: {}", storedFileName);
        }
    }

    /**
     * 특정 첨부파일들을 물리적으로 삭제하고 연관관계를 끊는다.
     */
    public void removeFiles(List<NoticeAttachment> attachments, Notice notice) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        log.info("첨부파일 부분 삭제 시작: count={}", attachments.size());

        attachments.forEach(att -> {
            log.debug("파일 삭제 시도: storedName='{}'", att.getStoredFileName());

            storageService.delete(att.getStoredFileName());
            notice.removeAttachment(att);

            log.info("파일 삭제 및 연관관계 제거 완료: {}", att.getStoredFileName());
        });
    }

    /**
     * 공지사항 삭제 시 모든 관련 파일을 물리 삭제한다.
     */
    public void deleteAllFiles(Notice notice) {
        List<NoticeAttachment> attachments = notice.getAttachments();
        if (attachments.isEmpty()) {
            return;
        }

        log.info("공지사항[{}] 관련 모든 첨부파일 삭제 시작: count={}",
                notice.getId(), attachments.size());

        attachments.forEach(att -> {
            storageService.delete(att.getStoredFileName());
            log.debug("물리 파일 삭제 완료: {}", att.getStoredFileName());
        });
    }
}
