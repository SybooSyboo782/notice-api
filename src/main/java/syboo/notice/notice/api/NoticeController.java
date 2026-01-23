package syboo.notice.notice.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import syboo.notice.notice.api.request.CreateNoticeRequest;
import syboo.notice.notice.application.NoticeService;
import syboo.notice.notice.application.command.CreateNoticeCommand;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 신규 공지사항을 등록한다.
     * * @param request 공지사항 등록 요청 데이터 (JSON)
     * @return 생성된 공지사항의 식별자(ID)와 201 Created 상태코드
     */
    @PostMapping
    public ResponseEntity<Long> createNotice(@RequestBody @Valid CreateNoticeRequest request) {
        log.info("공지사항 등록 요청: title='{}', author='{}'", request.title(), request.author());

        Long noticeId = noticeService.createNotice(toCommand(request));

        // 현재 요청 URI를 기준으로 새로운 리소스의 위치를 생성 (더 안전한 방식)
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(noticeId)
                .toUri();

        log.info("공지사항 등록 완료: id={}", noticeId);

        return ResponseEntity.created(location).body(noticeId);
    }

    /**
     * Request DTO를 Command 객체로 매핑한다.
     * 계층 간 의존성을 분리하기 위한 작업이다.
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
}
