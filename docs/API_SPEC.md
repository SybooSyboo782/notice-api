# API 명세 및 문서화 전략 (API_SPEC.md)

> 본 프로젝트는 **SpringDoc OpenAPI 3.0**을 도입하여 API 명세를 자동화하고, 클라이언트(프론트엔드)와의 원활한 협업을 위한 가독성 높은 UI를 제공함.

---

## Swagger UI 접속 정보
- **Local URL**: `http://localhost:8080/swagger-ui/index.html`
- **API Docs(JSON)**: `http://localhost:8080/v3/api-docs`

---

## 문서화 핵심 포인트

### 1. 에러 응답 규격화 (`ErrorResponse`)
단순한 String 메시지가 아닌, 구조화된 에러 응답을 제공하여 클라이언트의 예외 처리 편의성을 극대화함.
- **구조**:
    - `error`: 에러 코드 (예: `NOT_FOUND`, `VALIDATION_FAILED`)
    - `message`: 사용자 친화적인 에러 메시지
    - `details`: (선택) 필드 유효성 검증 실패 시 상세 이유 (Map 구조)

### 2. 바이너리 데이터 처리 시각화
파일 업로드 및 다운로드 API가 Swagger UI에서 직관적으로 동작하도록 설정함.
- **파일 업로드**: `MultipartFile` 필드에 `@Schema(format = "binary")`를 적용하여 파일 선택창(Choose File) 활성화함.
- **파일 다운로드**: 응답 미디어 타입을 `application/octet-stream`으로 명시하여 **[Download file]** 버튼이 생성되도록 구성함.

### 3. 유동적 설정값 동적 주입
`application.yml`에 정의된 파일 용량 제한(Max File Size) 값을 `SwaggerConfig`에서 `@Value`로 읽어와 문서 메인 설명에 동적으로 노출함.

---

## 공통 에러 코드 명세

| HTTP 상태 | 에러 코드                     | 발생 사유                              |
|:--------|:--------------------------|:-----------------------------------|
| **400** | `VALIDATION_FAILED`       | 필수 파라미터 누락 또는 유효성 검증 실패            |
| **400** | `INVALID_INPUT`           | 입력 값의 비즈니스 로직 위반 (예: 공지 기간 역전)     |
| **403** | `FILE_SECURITY_VIOLATION` | Path Traversal 등 보안 정책 위반 파일 접근 시도 |
| **404** | `NOT_FOUND`               | 존재하지 않는 공지사항 또는 리소스 요청             |
| **409** | `CONCURRENCY_CONFLICT`    | 동시 수정 충돌 발생 (Optimistic Lock)      |
| **413** | `FILE_TOO_LARGE`          | 설정된 최대 업로드 용량 초과                   |
| **500** | `INTERNAL_SERVER_ERROR`   | 서버 내부 물리적 오류 또는 시스템 장애             |

---