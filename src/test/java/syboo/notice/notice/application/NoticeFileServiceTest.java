package syboo.notice.notice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import syboo.notice.common.exception.FileSecurityException;
import syboo.notice.notice.api.response.FileDownloadResponse;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.domain.NoticeAttachment;
import syboo.notice.notice.infra.storage.StorageService;
import syboo.notice.notice.infra.util.ChecksumGenerator;
import syboo.notice.notice.infra.util.FileValidator;
import syboo.notice.notice.repository.NoticeAttachmentRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeFileServiceTest {

    @InjectMocks
    private NoticeFileService noticeFileService;

    @Mock
    private StorageService storageService;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private NoticeAttachmentRepository attachmentRepository;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 25, 20, 0);
    private final Clock clock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noticeFileService, "clock", clock);
    }

    @Test
    @DisplayName("파일 저장 시 물리 저장소에 파일이 복사되고 엔티티에 추가된다")
    void storeFiles_success() {
        // given
        Notice notice = createNotice(LocalDateTime.now().minusDays(1), fixedNow.plusDays(1));
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        given(fileValidator.validateAndReturnMimeType(any())).willReturn("text/plain");

        // when
        noticeFileService.storeFiles(List.of(file), notice);

        // then
        verify(storageService, times(1)).store(any(), anyString());
        assertThat(notice.getAttachments()).hasSize(1);
        assertThat(notice.isHasAttachment()).isTrue();

        // 추가 검증: 엔티티에 값이 잘 들어갔는지 확인
        NoticeAttachment attachment = notice.getAttachments().get(0);
        assertThat(attachment.getOriginFileName()).isEqualTo("test.txt");
        assertThat(attachment.getContentType()).isEqualTo("text/plain");
    }

    @Nested
    @DisplayName("파일 다운로드 테스트")
    class DownloadFile {

        @Test
        @DisplayName("정상적인 공지 기간 내 파일 다운로드 요청 시 성공한다.")
        void downloadSuccess() {
            // Given
            Long fileId = 1L;
            String content = "Hello World";
            String hash = ChecksumGenerator.generate(
                    new ByteArrayResource(content.getBytes())
            );

            Notice notice = createNotice(fixedNow.minusDays(1), fixedNow.plusDays(1));
            NoticeAttachment attachment = createAttachment(notice, "test.txt", "stored.txt", hash);

            Resource resource = new ByteArrayResource(content.getBytes());

            given(attachmentRepository.findById(fileId)).willReturn(Optional.of(attachment));
            given(storageService.loadAsResource("stored.txt")).willReturn(resource);

            // When
            FileDownloadResponse response = noticeFileService.downloadFile(fileId);

            // Then
            assertThat(response.originFileName()).isEqualTo("test.txt");
            assertThat(response.resource()).isNotNull();
        }

        @Test
        @DisplayName("공지 기간이 종료된 후 다운로드 요청 시 FileSecurityException이 발생한다.")
        void downloadFailExpiredPeriod() {
            // Given
            // Given
            Long fileId = 1L;
            LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 25, 12, 0); // 2026년 1월 25일

            // 2026년 1월 20일에 종료된 공지사항
            Notice expiredNotice = createNotice(fixedNow.minusDays(10), fixedNow.minusDays(5));
            NoticeAttachment attachment = createAttachment(expiredNotice, "expired.txt", "stored.txt", "hash");

            // Clock 고정 (현재 시간을 25일로 고정)
            Clock fixedClock = Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

            // 서비스 내부의 clock이 fixedClock을 바라보도록 설정 (ReflectionTestUtils 또는 @Spy 활용 가능)
            ReflectionTestUtils.setField(noticeFileService, "clock", fixedClock);

            given(attachmentRepository.findById(fileId)).willReturn(Optional.of(attachment));

            // When & Then
            assertThatThrownBy(() -> noticeFileService.downloadFile(fileId))
                    .isInstanceOf(FileSecurityException.class);
        }

        @Test
        @DisplayName("파일 체크섬이 일치하지 않으면(위변조) FileSecurityException이 발생한다.")
        void downloadFailChecksumMismatch() {
            // Given
            Long fileId = 1L;
            String wrongHash = "mismatch_hash";
            Notice notice = createNotice(fixedNow.minusDays(1), fixedNow.plusDays(1));
            NoticeAttachment attachment = createAttachment(notice, "fake.txt", "stored.txt", wrongHash);

            Resource realResource = new ByteArrayResource("Different Content".getBytes());

            given(attachmentRepository.findById(fileId)).willReturn(Optional.of(attachment));
            given(storageService.loadAsResource("stored.txt")).willReturn(realResource);

            // When & Then
            assertThatThrownBy(() -> noticeFileService.downloadFile(fileId))
                    .isInstanceOf(FileSecurityException.class)
                    .hasMessageContaining("변조되었을 가능성");
        }
    }

    // Helper Methods
    private Notice createNotice(LocalDateTime start, LocalDateTime end) {
        return Notice.builder()
                .title("테스트 공지")
                .content("내용")
                .author("작성자")
                .noticeStartAt(start)
                .noticeEndAt(end)
                .build();
    }

    private NoticeAttachment createAttachment(Notice notice, String origin, String stored, String hash) {
        NoticeAttachment att = NoticeAttachment.builder()
                .originFileName(origin)
                .storedFileName(stored)
                .checksum(hash)
                .build();
        notice.addAttachment(att);
        return att;
    }
}