package syboo.notice.notice.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import syboo.notice.notice.domain.Notice;
import syboo.notice.notice.repository.NoticeRepository;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // 실제 어플리케이션 컨텍스트를 로드함
@AutoConfigureMockMvc // MockMvc를 자동으로 설정함
@AutoConfigureJson
class NoticeConcurrencyTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 25, 20, 0);

    @Test
    @DisplayName("예외 케이스: 여러 사용자가 동시에 공지사항을 조회해도 조회수가 정확하게 반영되어야 한다")
    void getNotice_ConcurrentViewCountIncrease() throws Exception {
        // Given: 데이터를 저장하고 '강제로' DB에 반영합니다.
        Notice notice = noticeRepository.saveAndFlush(Notice.builder()
                .title("동시성 테스트")
                .content("내용")
                .author("tester")
                .noticeStartAt(fixedNow)
                .noticeEndAt(fixedNow.plusDays(7))
                .build());
        Long targetId = notice.getId();

        int threadCount = 10; // 10명이 동시에 클릭
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 10개의 스레드에서 동시에 API 호출
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/notices/" + targetId));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드가 끝날 때까지 대기

        // Then: 조회수가 정확히 10이 되었는지 확인
        mockMvc.perform(get("/api/notices/" + targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(11)); // 기존 10번 + 마지막 확인용 1번 = 11
    }
}
