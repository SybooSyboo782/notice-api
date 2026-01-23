package syboo.notice.notice.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import syboo.notice.notice.api.request.CreateNoticeRequest;
import syboo.notice.notice.api.request.UpdateNoticeRequest;
import syboo.notice.notice.application.NoticeService;
import syboo.notice.notice.application.command.CreateNoticeCommand;
import syboo.notice.notice.application.command.UpdateNoticeCommand;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Validated
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 신규 공지사항을 등록한다.
     * @param request 공지사항 등록 요청 데이터 (JSON)
     * @return 생성된 공지사항의 식별자(ID)와 201 Created 상태코드
     */
    @PostMapping
    public ResponseEntity<Long> createNotice(@RequestBody @Valid CreateNoticeRequest request) {
        log.info("공지사항 등록 요청: title='{}', author='{}', hasFiles={}",
                request.title(), request.author(), !request.attachments().isEmpty());

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
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid UpdateNoticeRequest request) {
        log.info("공지사항 수정 요청: id={}, title='{}'", id, request.title());

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
        List<CreateNoticeCommand.AttachmentCommand> attachments =
                Optional.ofNullable(request.attachments())
                        .orElseGet(List::of) // null 안전성 확보
                        .stream()
                        .map(att -> new CreateNoticeCommand.AttachmentCommand(
                                att.fileName(),
                                att.storedPath(),
                                att.fileSize()
                        ))
                        .toList();

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
        List<UpdateNoticeCommand.AttachmentCommand> attachments =
                Optional.ofNullable(request.attachments())
                        .orElseGet(List::of)
                        .stream()
                        .map(att -> new UpdateNoticeCommand.AttachmentCommand(
                                att.fileName(),
                                att.storedPath(),
                                att.fileSize()
                        ))
                        .toList();

        log.debug("공지사항[{}] 수정 첨부파일 변환 완료: {}개", id, attachments.size());

        return new UpdateNoticeCommand(
                id,
                request.title(),
                request.content(),
                request.noticeStartAt(),
                request.noticeEndAt(),
                attachments
        );
    }
}
