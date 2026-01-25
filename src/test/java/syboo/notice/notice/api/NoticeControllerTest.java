package syboo.notice.notice.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import syboo.notice.IntegrationTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoticeControllerTest extends IntegrationTestSupport {
    @Test
    @DisplayName("해피 케이스: 공지사항의 생성부터 수정, 삭제까지의 전체 생명주기 검증")
    void notice_Full_Lifecycle_HappyPath() throws Exception {
        // 1. 등록 (Create) - Multipart 요청 시뮬레이션
        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "initial.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        String location = mockMvc.perform(multipart("/api/notices")
                        .file(file)
                        .param("title", "최초 제목")
                        .param("content", "최초 내용")
                        .param("author", "tester")
                        .param("noticeStartAt", "2026-01-25T00:00:00")
                        .param("noticeEndAt", "2026-12-31T23:59:59"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        // Location 헤더에서 생성된 ID 추출 (예: /api/notices/1 -> 1)
        String targetId = location.substring(location.lastIndexOf("/") + 1);

        // 2. 수정 (Update) 단계 코드 수정
        MockMultipartFile newFile = new MockMultipartFile(
                "newAttachments",
                "added.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        mockMvc.perform(multipart("/api/notices/" + targetId)
                        .file(newFile)
                        // MockMvc의 multipart는 기본이 POST이므로 PUT으로 강제 지정
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("noticeStartAt", "2026-01-25T00:00:00")
                        .param("noticeEndAt", "2026-12-31T23:59:59")
                        .param("remainAttachmentIds", targetId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)) // 타입을 명시적으로 지정
                .andDo(print())
                .andExpect(status().isNoContent());

        // 3. 상세 조회 (Read) - 수정사항 반영 확인
        mockMvc.perform(get("/api/notices/" + targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.attachments.length()").value(2))
                .andExpect(jsonPath("$.attachments[?(@.originFileName == 'initial.jpg')]").exists())
                .andExpect(jsonPath("$.attachments[?(@.originFileName == 'added.jpg')]").exists());

        // 4. 삭제 (Delete)
        mockMvc.perform(delete("/api/notices/" + targetId))
                .andExpect(status().isNoContent());

        // 5. 최종 확인 - 삭제 후 조회 시 404
        mockMvc.perform(get("/api/notices/" + targetId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("해피 케이스: 첨부파일이 없는 공지사항도 정상적으로 등록 및 조회되어야 한다")
    void createNotice_WithoutFile_Success() throws Exception {
        // Given: 파일 없이 파라미터만 전송
        mockMvc.perform(multipart("/api/notices")
                        .param("title", "파일 없는 공지")
                        .param("content", "내용")
                        .param("author", "tester")
                        .param("noticeStartAt", "2026-01-25T00:00:00")
                        .param("noticeEndAt", "2026-12-31T23:59:59")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        // Then: 조회 시 hasAttachment가 false여야 함
        mockMvc.perform(get("/api/notices"))
                .andExpect(jsonPath("$.content[?(@.title == '파일 없는 공지')].hasAttachment").value(false));
    }

    @Test
    @DisplayName("예외 케이스: 존재하지 않는 공지사항 상세 조회 시 404 에러를 반환한다")
    void getNotice_Fail_NotFound() throws Exception {
        // Given: 존재하지 않는 ID 9999L

        // When & Then: GlobalExceptionHandler가 작동하여 규격화된 에러 응답을 주는지 확인
        mockMvc.perform(get("/api/notices/9999")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("예외 케이스: 공지 종료일이 시작일보다 빠르면 400 에러를 반환한다")
    void createNotice_Fail_InvalidDateRange() throws Exception {
        mockMvc.perform(multipart("/api/notices")
                        .param("title", "역전된 날짜")
                        .param("content", "내용")
                        .param("author", "tester")
                        .param("noticeStartAt", "2026-12-31T00:00:00") // 시작이 12월
                        .param("noticeEndAt", "2026-01-01T00:00:00")   // 종료가 1월
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("예외 케이스: 잘못된 입력값(제목 누락)으로 등록 시도 시 400 에러를 반환한다")
    void createNotice_Fail_InvalidInput() throws Exception {
        // Given: 제목(title)이 없는 요청
        mockMvc.perform(multipart("/api/notices")
                        .param("content", "내용만 있고 제목은 없음")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}