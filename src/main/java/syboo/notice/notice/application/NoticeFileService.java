package syboo.notice.notice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import syboo.notice.common.exception.FileInvalidException;
import syboo.notice.common.exception.FileSecurityException;
import syboo.notice.notice.api.response.FileDownloadResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.infra.storage.StorageService;
import syboo.notice.notice.infra.util.ChecksumGenerator;
import syboo.notice.notice.infra.util.FileValidator;
import syboo.notice.notice.repository.NoticeAttachmentRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeFileService {

    private final Clock clock;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final NoticeAttachmentRepository noticeAttachmentRepository;

    /**
     * 신규 파일들을 저장하고 NoticeAttachment 엔티티 리스트를 반환한다.
     * <p>
     * <b>주의:</b> 해당 메서드는 물리적 파일 저장과 DB 저장을 병행
     *  * 파일 저장 도중 예외가 발생하면 DB 트랜잭션은 롤백되지만, 이미 저장된 물리 파일은 남음
     *  * 이 정합성 문제는 별도의 배치 작업이나 파일 삭제 이벤트를 통해 해결 필요
     *  </p>
     */
    public void storeFiles(List<MultipartFile> files, Notice notice) {
        if (files == null || files.isEmpty()) {
            return;
        }

        log.info("첨부파일 물리 저장 시작: count={}", files.size());

        for (MultipartFile file : files) {
            // 보안 검증 (Tika를 이용한 MIME 타입 및 화이트리스트 체크)
            String validatedMimeType = fileValidator.validateAndReturnMimeType(file);

            // 체크섬 생성 (SHA-256 기반 무결성 해시 추출)
            String checksum = ChecksumGenerator.generate(file);

            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            log.debug("파일 저장 시도: originName='{}', storedName='{}'",
                    file.getOriginalFilename(), storedFileName);

            storageService.store(file, storedFileName);

            notice.addAttachment(NoticeAttachment.builder()
                    .originFileName(file.getOriginalFilename())
                    .storedFileName(storedFileName)
                    .fileSize(file.getSize())
                    .contentType(validatedMimeType)
                    .checksum(checksum)
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

        List<NoticeAttachment> targetList = List.copyOf(attachments);

        targetList.forEach(att -> {
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
        List<NoticeAttachment> attachments = List.copyOf(notice.getAttachments());

        if (attachments.isEmpty()) {
            return;
        }

        log.info("공지사항[{}] 관련 모든 첨부파일 삭제 시작: count={}",
                notice.getId(), attachments.size());

        attachments.forEach(att -> {
            try {
                // 물리 서버의 파일 삭제
                storageService.delete(att.getStoredFileName());

                // 엔티티 간 양방향 연관관계 제거
                notice.removeAttachment(att);
                log.debug("물리 파일 삭제 완료: {}", att.getStoredFileName());
            } catch (Exception e) {
                log.error("파일 삭제 중 오류 발생 (파일명: {}): {}", att.getStoredFileName(), e.getMessage());
            }
        });
    }

    /**
     * 첨부파일 식별자를 통해 파일을 로드하고, SHA-256 체크섬 대조를 통해 무결성을 검증합니다.
     * <p>
     * @param attachmentId 첨부파일 식별자 (ID)
     * @return 원본 파일명과 검증된 리소스를 포함한 {@link FileDownloadResponse}
     * @throws FileInvalidException  존재하지 않는 파일 ID일 경우 발생
     * @throws FileSecurityException 서버 내 파일 위변조가 감지될 경우 발생
     */
    public FileDownloadResponse downloadFile(Long attachmentId) {
        log.info("첨부파일 다운로드 프로세스 시작: ID={}", attachmentId);

        NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> {
                    log.warn("파일 다운로드 실패 - 존재하지 않는 ID: {}", attachmentId);
                    return new FileInvalidException("존재하지 않는 파일입니다.");
                });

        // 해당 파일이 속한 공지사항의 게시 기간 확인
        Notice notice = attachment.getNotice();
        if (notice == null || !notice.isViewable(clock)) {
            log.warn("권한 없는 파일 다운로드 시도: FileID={}, NoticeID={}",
                    attachmentId, (notice != null ? notice.getId() : "null"));
            throw new FileSecurityException("현재 다운로드 가능한 공지 기간이 아닙니다.");
        }

        Resource resource = storageService.loadAsResource(attachment.getStoredFileName());

        // 무결성 검증 (보안 요구사항)
        log.debug("파일 무결성 검증 시작: {}", attachment.getOriginFileName());
        String currentChecksum = ChecksumGenerator.generate(resource);

        if (!attachment.getChecksum().equals(currentChecksum)) {
            log.error("보안 경고: 파일 무결성 검증 실패! (ID: {}, 파일명: {})",
                    attachmentId, attachment.getOriginFileName());
            log.error("무결성 검증 실패! DB: {}, 실제: {}", attachment.getChecksum(), currentChecksum);
            throw new FileSecurityException("파일이 서버에서 변조되었을 가능성이 있습니다.");
        }

        log.info("첨부파일 검증 완료 및 다운로드 준비: {}", attachment.getOriginFileName());

        return new FileDownloadResponse(attachment.getOriginFileName(), resource);
    }
}
