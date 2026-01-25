package syboo.notice.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    // yml의 설정값을 동적으로 읽어온다.
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:50MB}")
    private String maxRequestSize;

    @Bean
    public OpenAPI noticeOpenAPI() {
        String description = """
                공지사항 관리 및 첨부파일 기능을 제공하는 API 문서입니다.
                
                ### 주요 기능
                - 공지사항 등록/수정/삭제/조회
                - 다중 첨부파일 업로드 및 안전한 다운로드
                - 조회수 중복 방지 및 동시성 처리
                - 정렬 필드 유효성 검증
                
                **[파일 업로드 제한]**
                - 개별 파일 최대: %s
                - 전체 요청 최대: %s
                """.formatted(maxFileSize, maxRequestSize);

        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080").description("로컬 서버")))
                .info(new Info()
                        .title("Notice Service API")
                        .description(description)
                        .version("v1.0.0"));
    }
}