# 공지사항 API

## 설명

Spring Boot로 구현한 공지사항 관리 REST API입니다.

## 기술 스택

* Java
* Spring Boot
* JPA (Hibernate)
* RDBMS

## 주요 기능

* 공지사항 CRUD
* 공지사항 검색
* 첨부파일 관리

## 프로젝트 구조

```
com.example.notice
 ├─ notice
 │  ├─ api
 │  ├─ application
 │  ├─ domain
 │  └─ repository
 ├─ attachment
 │  ├─ domain
 │  └─ repository
 └─ common
    ├─ exception
    ├─ response
    └─ config
```

## 브랜치 전략

* main
* chore/*
* setup/*
* feature/*

## 테스트

* 단위 테스트
* 통합 테스트

## 실행 방법

```bash
./gradlew build
java -jar build/libs/app.jar
```

## 참고 사항

본 README는 스켈레톤이며, 개발 진행에 따라 업데이트됩니다.