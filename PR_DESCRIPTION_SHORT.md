# Route API 구현

## 📋 개요

여행 루트 생성/수정/삭제, 일행 관리, 공유 기능을 제공하는 Route API를 구현했습니다. Google Maps 연동을 위한 위치 정보 저장 기능과 QR 코드 생성 기능을 포함합니다.

## ✨ 주요 기능

- ✅ 루트 CRUD (생성/조회/수정/삭제)
- ✅ 일행 관리 (추가/조회/삭제)
- ✅ 공유 링크 및 QR 코드 생성
- ✅ 위치 정보 관리 (위도/경도, 일자별 관리)

## 🏗️ 구현 내용

### 엔티티
- **Route**: 루트 정보 (제목, 설명, 날짜, 공개/비공개, 공유코드)
- **RouteParticipant**: 루트 일행 관계
- **RouteLocation**: 위치 정보 (위도/경도, 일차별 관리)

### API 엔드포인트

**루트 관리**
- `POST /api/routes` - 루트 생성
- `GET /api/routes/{routeId}` - 루트 조회
- `PUT /api/routes/{routeId}` - 루트 수정
- `DELETE /api/routes/{routeId}` - 루트 삭제
- `GET /api/routes/my` - 내 루트 목록 조회

**일행 관리**
- `POST /api/routes/{routeId}/participants` - 일행 추가
- `GET /api/routes/{routeId}/participants` - 일행 목록 조회
- `DELETE /api/routes/{routeId}/participants?participantIds=1,2,3` - 일행 삭제

**공유 기능**
- `GET /api/routes/{routeId}/share` - 공유 링크 및 QR 코드 생성
- `GET /api/routes/share/{shareCode}` - 공유 코드로 루트 조회 (인증 불필요)

### 주요 클래스
- `RouteService`: 루트 비즈니스 로직 처리, 권한 검증
- `RouteController`: REST API 엔드포인트 제공 (Swagger 문서화)
- `QrCodeGenerator`: QR 코드 생성 유틸리티 (ZXing 라이브러리 사용)

## 🔒 보안 및 권한

- 인증: Spring Security 기반 인증 필요
- 권한 검증:
  - 루트 수정/삭제: 생성자만 가능
  - 일행 추가: 생성자 또는 기존 일행만 가능
  - 일행 삭제: 생성자만 가능
  - 루트 조회: 공개 루트 또는 생성자/일행만 가능

## 📦 의존성 추가

```gradle
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.google.zxing:javase:3.5.2'
```

## ⚙️ 설정 변경

### application.yml
- Google Maps API 키 설정 (향후 확장용)
- 공유 링크 기본 URL 설정

### SecurityConfig
- `/api/routes/share/**` 경로 공개 접근 허용

## 🗄️ 데이터베이스

### 테이블
- `routes`: 루트 정보
- `route_participants`: 루트 일행 관계
- `route_locations`: 루트 위치 정보

## 📝 주요 특징

1. **Google Maps 연동 준비**: 위치 정보에 위도/경도 저장
2. **QR 코드 자동 생성**: 공유 링크 생성 시 Base64 인코딩된 QR 코드 제공
3. **유연한 위치 관리**: 일자별 위치 추가 및 표시 순서 관리

## 🧪 테스트

- Swagger UI: `/swagger-ui.html`
- 테스트 가이드: `ROUTE_API_TEST_GUIDE.md`

---

## 변경 파일

### 새로 추가 (14개)
- Route 도메인 전체 (Entity, Repository, DTO, Service, Controller)
- `QrCodeGenerator` 유틸리티
- `ROUTE_API_TEST_GUIDE.md`

### 수정 (3개)
- `build.gradle`: QR 코드 라이브러리 의존성 추가
- `application.yml`: Google Maps API 및 공유 링크 URL 설정
- `SecurityConfig.java`: 공유 링크 접근 허용

