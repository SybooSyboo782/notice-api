package syboo.notice.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

@Schema(description = "공통 에러 응답 객체")
@Builder
public record ErrorResponse(
        @Schema(description = "에러 코드 (대문자 스네이크 케이스)", example = "NOT_FOUND")
        String error,

        @Schema(description = "에러 메시지 (사용자용)", example = "해당 공지사항을 찾을 수 없습니다.")
        String message,

        @Schema(description = "상세 에러 정보 (필드 검증 실패 시 사용)", nullable = true)
        Map<String, String> details
) {
    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .build();
    }

    public static ErrorResponse of(String error, Map<String, String> details) {
        return ErrorResponse.builder()
                .error(error)
                .message("입력값 검증에 실패했습니다.")
                .details(details)
                .build();
    }
}