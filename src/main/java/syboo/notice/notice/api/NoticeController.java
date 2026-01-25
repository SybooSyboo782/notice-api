package syboo.notice.notice.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import syboo.notice.notice.api.request.CreateNoticeRequest;
import syboo.notice.notice.api.request.UpdateNoticeRequest;
import syboo.notice.notice.api.response.FileDownloadResponse;
import syboo.notice.notice.application.NoticeFileService;
import syboo.notice.notice.application.NoticeService;
import syboo.notice.notice.application.command.CreateNoticeCommand;
import syboo.notice.notice.application.command.UpdateNoticeCommand;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Validated
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeFileService noticeFileService;

    /**
     * 신규 공지사항을 등록한다.
     * @param request 공지사항 등록 요청 데이터 (JSON)
     * @return 생성된 공지사항의 식별자(ID)와 201 Created 상태코드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createNotice(@ModelAttribute @Valid CreateNoticeRequest request) {
        log.info("공지사항 등록 요청: title='{}', author='{}', fileCount={}",
                request.title(), request.author(),
                request.attachments() != null ? request.attachments().size() : 0);

        Long noticeId = noticeService.createNotice(toCommand(request));

        // 현재 요청 URI를 기준으로 새로운 리소스의 위치를 생성
        URI location = createLocationUri(noticeId);

        log.info("공지사항 등록 완료: id={}", noticeId);

        return ResponseEntity.created(location).body(noticeId);
    }

    /**
     * 기존 공지사항을 수정한다.
     *
     * @param id 수정할 대상의 식별자
     * @param request 수정 요청 데이터
     * @return 204 No Content
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateNotice(
            @PathVariable @Min(1) Long id,
            @ModelAttribute @Valid UpdateNoticeRequest request) {
        log.info("공지사항 수정 요청: id={}, title='{}', newFiles={}, remainFiles={}",
                id,
                request.title(),
                request.newAttachments() != null ? request.newAttachments().size() : 0,
                request.remainAttachmentIds() != null ? request.remainAttachmentIds().size() : 0);

        noticeService.updateNotice(id, toUpdateCommand(id, request));

        log.info("공지사항 수정 완료: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공지사항을 삭제한다.
     *
     * @param id 삭제할 대상의 식별자
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable @Min(1) Long id) {
        log.info("공지사항 삭제 요청: id={}", id);

        noticeService.deleteNotice(id);

        log.info("공지사항 삭제 완료: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /* -------------------------------------------------------------------------- */
    /* 내부 변환 로직 (Mapping & URI) */
    /* -------------------------------------------------------------------------- */

    /**
     * 리소스 접근을 위한 Location URI를 생성한다.
     */
    private static URI createLocationUri(Long noticeId) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(noticeId)
                .toUri();
    }

    /**
     * 등록 요청 DTO를 서비스용 Command 객체로 매핑한다.
     */
    private CreateNoticeCommand toCommand(CreateNoticeRequest request) {
        List<MultipartFile> attachments = Optional.ofNullable(request.attachments())
                .orElseGet(List::of);

        log.debug("첨부파일 변환 완료: {}개", attachments.size());

        return new CreateNoticeCommand(
                request.title(),
                request.content(),
                request.author(),
                request.noticeStartAt(),
                request.noticeEndAt(),
                attachments
        );
    }

    /**
     * 수정 요청 DTO를 서비스용 Command 객체로 매핑한다.
     */
    private UpdateNoticeCommand toUpdateCommand(Long id, UpdateNoticeRequest request) {
        // 1. 새로 업로드된 파일들 추출
        List<MultipartFile> newAttachments = Optional.ofNullable(request.newAttachments())
                .orElseGet(List::of);

        // 2. 유지할 기존 파일 ID 리스트 추출
        List<Long> remainAttachmentIds = Optional.ofNullable(request.remainAttachmentIds())
                .orElseGet(List::of);

        log.debug("공지사항[{}] 수정 변환 - 신규 파일: {}개, 유지 ID: {}개",
                id, newAttachments.size(), remainAttachmentIds.size());

        return new UpdateNoticeCommand(
                id,
                request.title(),
                request.content(),
                request.noticeStartAt(),
                request.noticeEndAt(),
                newAttachments,
                remainAttachmentIds
        );
    }

    /**
     * 첨부파일을 다운로드한다.
     * 한글 파일명 깨짐 방지를 위해 UTF-8 인코딩을 적용함.
     *
     * @param fileId 첨부파일 식별자
     * @return 파일 바이너리 리소스를 포함한 {@link ResponseEntity}
     */
    @GetMapping("/attachments/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        log.info("첨부파일 다운로드 API 호출: fileId={}", fileId);

        // Record의 필드 접근은 메서드 호출 방식을 사용 (get 접미사 없음)
        FileDownloadResponse response = noticeFileService.downloadFile(fileId);

        // 한글 파일명 깨짐 방지 인코딩 (StandardCharsets.UTF_8 사용)
        String encodedFileName = UriUtils.encode(response.originFileName(), StandardCharsets.UTF_8);

        // RFC 5987 규격에 따른 Content-Disposition 설정
        String contentDisposition =
                "attachment; filename=\"" + encodedFileName + "\"; " +
                        "filename*=UTF-8''" + encodedFileName;

        log.debug("파일 다운로드 응답 생성 완료: originName='{}'", response.originFileName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // 이진 데이터 스트림 명시
                .body(response.resource());
    }
}
