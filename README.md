# 공지사항 관리 서비스 (Notice Service)

> **본 프로젝트는 공지사항 관리 REST API 과제를 기반으로,**
> **대용량 트래픽 환경에서도 안정적으로 동작하는 서버 아키텍처와**
> **보안·테스트·동시성 제어를 고려한 실무 지향 설계를 목표로 구현함.**
>
> 단순 CRUD 구현에 그치지 않고,  
> **대용량 파일 처리, 파일 보안, 테스트 신뢰성 확보**라는 비기능 요구사항을
> 명확한 기술적 근거를 바탕으로 해결함.

---
## 1. 빠른 실행 방법 (Quick Start)

### **환경 요구사항**
- **Java**: 21
- **Framework**: Spring Boot 4.0.1
- **DB**: 
  - **Production**: PostgreSQL
  - **Development/Test**: **H2 Database (In-Memory)**
    - *테스트 프로파일(`test`) 실행 시 별도 DB 설치 없이 즉시 검증 가능함.*

### **실행 명령어**
1. `./gradlew bootRun` 입력하여 애플리케이션 실행
2. **Swagger API 명세**: http://localhost:8080/swagger-ui/index.html 접속

## 2. 기술 스택 및 선택 이유
- **Language & Persistence**: Java 21, Hibernate (JPA), **Querydsl 5.1**
    - 가산점 항목인 **Hibernate(JPA)** 를 기반으로 영속성 계층을 설계함.
    - Querydsl을 도입하여 제목/내용/기간 조건이 조합되는 검색 요구사항을
      안전하게 구현하고, 런타임 쿼리 오류를 사전에 방지함.
- **Database Storage**: PostgreSQL & H2
    - `MODE=PostgreSQL` 설정을 통해 로컬 테스트 환경에서도 운영 환경과 동일한 쿼리 정합성을 보장함.
- **Security**: **Apache Tika** & **SHA-256**
    - 파일 변조 및 Path Traversal 공격을 원천 차단하는 다중 보안 레이어를 적용함.
- **Performance**: **Log4j2** & **8KB Stream Buffer**
    - 대용량 파일 처리를 위한 스트림 최적화 및 고성능 로깅 시스템을 구축함.

## 3. 프로젝트 구조

```
src/main/java/syboo/notice/
├── common/                # 공통 설정 및 전역 예외 처리
│   ├── config/            # Swagger, Querydsl, Clock 설정
│   ├── domain/            # 공통 도메인 모델 및 유틸리티
│   ├── exception/         # 전역 예외 처리 (GlobalExceptionHandler)
│   └── response/          # 공통 응답 규격 (ErrorResponse)
└── notice/                # 공지사항 도메인
    ├── api/               # Controller 및 Request/Response DTO
    ├── application/       # Service (비즈니스 로직 및 트랜잭션 처리)
    ├── domain/            # JPA Entity 및 도메인 핵심 모델
    ├── infra/             # 도메인 전용 인프라 (파일 스토리지 서비스 등)
    └── repository/        # Spring Data JPA 및 Querydsl DAO
```

## 4. 브랜치 전략
- **`main`**: 상용 배포 가능한 상태의 안정된 코드 보관
- **`chore`**: 시스템 설정, 빌드 스크립트 수정, 패키지 매니저 관리
- **`feature/`**: 단위 기능 개발 및 버그 수정
- **`docs/`**: 문서 작성 및 프로젝트 설정 관리

## 5. 핵심 문제 해결 전략 (비기능 요구사항 준수)

본 프로젝트는 과제에서 제시한 **대용량 트래픽**과 **테스트 코드 작성** 요구사항을 아래와 같이 해결했음.

### **[상세 기술 문서 링크]**
**과제 수행 중 직면한 기술적 제약과 그에 대한 설계적 의사결정 근거를 기록한 문서임.**
- [**기술적 난제 및 트러블슈팅 상세**](./docs/TROUBLE_SHOOTING.md)
- [**API 명세 및 에러 규격 가이드**](./docs/API_SPEC.md)

### **요약**
1. **대용량 트래픽 대응**: 파일 처리 시 메모리 점유를 최소화하기 위해 8KB 버퍼 기반 스트림 처리를 구현함.
2. **데이터 정합성**: Hibernate @Version 기반 **낙관적 락(Optimistic Lock)**으로 동시 수정 충돌을 방지함.
3. **보안성**: Tika를 이용한 MIME Type 검증 및 파일명 암호화로 시스템 보안을 강화함.
4. **테스트 신뢰도**: Clock 추상화와 H2 In-Memory 환경을 통해 환경에 독립적인 테스트 수트를 구축함.


## 6. 요구사항 충족 현황
- [x] **REST API**: 모든 엔드포인트 RESTful 설계 완료
- [x] **공지사항 CRUD**: 제목, 내용, 기간, 다중 첨부파일 지원
- [x] **상세 검색**: Querydsl을 이용한 제목/내용/등록일자 검색 구현
- [x] **테스트**: 단위/통합 테스트 완비 및 테스트 프로파일 분리 완료

## 7. 개발 프로세스 및 검증 전략 (Process & Verification)

본 프로젝트는 AI 협업 툴과 API 테스트 도구를 활용하여 개발 생산성을 높이고, 설계의 무결성을 검증하는 데 집중함.

### **1) AI-Assisted Architecture & Security**
- **설계 효율화**: AI 도구를 활용하여 아키텍처 초안을 빠르게 구성하고, `Java 21` 및 `Spring Boot 4.0.1` 환경에 최적화된 코드 구조를 도출함.
- **보안 검증**: AI와의 교차 검토를 통해 **Apache Tika 기반 MIME 타입 체크** 및 **Path Traversal 방어** 로직을 강화하여 보안 취약점을 사전에 차단함.

### **2) Postman을 이용한 API 샌드박스 검증**
- **실시간 엔드포인트 검증**: Swagger UI를 통한 단위 검증 외에도, Postman을 독립적인 샌드박스 환경으로 활용하여 실제 다중 파일 업로드 및 복합 검색 쿼리의 응답 정합성을 실시간으로 교차 검증함.
- **예외 시나리오 탐색**: 클라이언트 입장에서 발생할 수 있는 404(미존재), 400(제약사항 위반) 등 다양한 예외 상황을 직접 호출하며 API의 안정성을 확인 완료함.
- **API 콜렉션**: 시나리오별 API 호출 세트를 구성하여 통합 테스트 환경에서 일관된 응답을 보장함

## 8. 참고 사항 (Notes)

- **Java 21 실행 환경**: 본 프로젝트는 반드시 JDK 21 이상 환경에서 빌드 및 실행해 주시기 바람.
- **파일 저장 경로**: 첨부파일은 프로젝트 루트의 `./uploads/notice` 경로에 저장됨. (실행 환경에 따른 디렉토리 생성 및 쓰기 권한 확인 필요)
- **H2 Console 접속**: `test` 프로파일 실행 시 `http://localhost:8080/h2-console`을 통해 인메모리 데이터를 확인할 수 있음. (JDBC URL: `jdbc:h2:mem:testdb`)
- **테스트 격리**
  - 모든 통합 테스트는 `IntegrationTestSupport`를 기반으로 실행되며,
    테스트 간 상태 공유를 차단하여 반복 실행 시에도 항상 동일한 결과를 보장함.
  - 이는 과제 요구사항 중 **단위/통합 테스트 신뢰성**을 충족하기 위한 설계임.
