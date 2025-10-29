# Route API 구현 - PR 설명

## 📋 개요

여행 루트(경로) 생성, 수정, 삭제 및 일행 관리, 공유 기능을 제공하는 Route API를 구현했습니다. Google Maps 연동을 위한 위치 정보 저장 기능과 QR 코드 생성 기능을 포함합니다.

## ✨ 주요 기능

### 1. 루트 관리
- ✅ 루트 생성 (제목, 설명, 시작/종료 날짜, 공개/비공개 설정)
- ✅ 루트 조회 (상세 정보 포함)
- ✅ 루트 수정
- ✅ 루트 삭제
- ✅ 내 루트 목록 조회

### 2. 일행 관리
- ✅ 일행 추가
- ✅ 일행 목록 조회
- ✅ 일행 삭제 (여러 명 동시 삭제 가능)

### 3. 공유 기능
- ✅ 공유 링크 생성
- ✅ QR 코드 자동 생성 (Base64 인코딩)
- ✅ 공유 코드로 루트 조회 (인증 불필요)

### 4. 위치 정보 관리
- ✅ 루트별 위치(장소) 정보 저장
- ✅ Google Maps 연동을 위한 위도/경도 저장
- ✅ 일자별 위치 관리 (DAY 1, DAY 2, ...)

## 🏗️ 구현 내용

### 엔티티 (Entity)

#### Route
```java
- id: 루트 ID
- title: 루트 제목
- description: 루트 설명
- startDate/endDate: 여행 기간
- creator: 생성자 (User)
- isPublic: 공개/비공개 여부
- shareCode: 공유 코드 (고유)
- participants: 일행 목록 (OneToMany)
- locations: 위치 정보 목록 (OneToMany)
```

#### RouteParticipant
```java
- id: 참가자 ID
- route: 참가한 루트 (ManyToOne)
- user: 참가자 사용자 (ManyToOne)
- joinedAt: 참가 시각
```

#### RouteLocation
```java
- id: 위치 ID
- route: 소속 루트 (ManyToOne)
- dayNumber: 일차 (1, 2, 3, ...)
- name: 위치명
- address: 주소
- latitude/longitude: 위도/경도 (Google Maps용)
- description: 위치 설명
- displayOrder: 표시 순서
```

### API 엔드포인트

#### 루트 관리
- `POST /api/routes` - 루트 생성
- `GET /api/routes/{routeId}` - 루트 조회
- `PUT /api/routes/{routeId}` - 루트 수정
- `DELETE /api/routes/{routeId}` - 루트 삭제
- `GET /api/routes/my` - 내 루트 목록 조회

#### 일행 관리
- `POST /api/routes/{routeId}/participants` - 일행 추가
- `GET /api/routes/{routeId}/participants` - 일행 목록 조회
- `DELETE /api/routes/{routeId}/participants?participantIds=1,2,3` - 일행 삭제

#### 공유 기능
- `GET /api/routes/{routeId}/share` - 공유 링크 및 QR 코드 생성
- `GET /api/routes/share/{shareCode}` - 공유 코드로 루트 조회 (인증 불필요)

### 주요 구현 클래스

#### Service Layer
- `RouteService`: 루트 비즈니스 로직 처리
  - 루트 CRUD 작업
  - 일행 추가/삭제 관리
  - 공유 링크 및 QR 코드 생성
  - 권한 검증 (생성자만 수정/삭제 가능)

#### Controller Layer
- `RouteController`: REST API 엔드포인트 제공
  - Swagger 문서화 완료
  - 인증/인가 처리
  - 요청/응답 DTO 변환

#### Repository Layer
- `RouteRepository`: 루트 데이터 접근
- `RouteParticipantRepository`: 일행 데이터 접근

#### Util
- `QrCodeGenerator`: QR 코드 생성 유틸리티
  - ZXing 라이브러리 사용
  - Base64 인코딩된 이미지 반환

## 🔒 보안 및 권한 관리

- **인증**: 대부분의 API는 Spring Security를 통한 인증 필요
- **권한 검증**:
  - 루트 수정/삭제: 생성자만 가능
  - 일행 추가: 생성자 또는 기존 일행만 가능
  - 일행 삭제: 생성자만 가능
  - 루트 조회: 공개 루트 또는 생성자/일행만 가능
- **공유 링크**: 인증 없이 공유 코드로 조회 가능 (SecurityConfig 설정)

## 📦 의존성 추가

```gradle
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.google.zxing:javase:3.5.2'
```
- QR 코드 생성을 위한 ZXing 라이브러리

## ⚙️ 설정 변경

### application.yml
```yaml
google:
  maps:
    api-key: ${GOOGLE_MAPS_API_KEY:your-google-maps-api-key-here}
    base-url: https://maps.googleapis.com/maps/api

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
```
- Google Maps API 키 설정 (향후 확장용)
- 공유 링크 기본 URL 설정

### SecurityConfig
- `/api/routes/share/**` 경로 공개 접근 허용 추가

## 🗄️ 데이터베이스 스키마

### 테이블 생성
- `routes`: 루트 정보
- `route_participants`: 루트 일행 관계
- `route_locations`: 루트 위치 정보

### 관계 설정
- Route ↔ User (ManyToOne) - 생성자
- Route ↔ RouteParticipant (OneToMany)
- Route ↔ RouteLocation (OneToMany)
- RouteParticipant ↔ User (ManyToOne) - 일행

## 📝 주요 특징

1. **Google Maps 연동 준비**
   - 위치 정보에 위도/경도 저장
   - 클라이언트에서 지도 표시 시 활용 가능

2. **QR 코드 자동 생성**
   - 공유 링크 생성 시 자동으로 QR 코드 생성
   - Base64 인코딩된 이미지로 제공

3. **유연한 위치 관리**
   - 일자별 위치 추가 가능
   - 표시 순서 관리
   - 위도/경도 정보 저장

4. **공유 기능**
   - 고유한 shareCode 생성
   - 인증 없이 공유 링크 접근 가능

## 🧪 테스트

### Swagger UI
- `/swagger-ui.html`에서 모든 API 테스트 가능
- 인증 필요 API는 먼저 로그인 필요

### 테스트 가이드
- `ROUTE_API_TEST_GUIDE.md` 파일 참조

## 🔄 향후 확장 계획

1. Google Maps API 연동
   - 주소 → 좌표 변환 (Geocoding API)
   - 장소 검색 (Places API)
   - 경로 계산 (Directions API)

2. 추가 기능
   - 루트 좋아요
   - 루트 댓글
   - 루트 카테고리/태그
   - 루트 이미지 업로드

## 📚 참고 문서

- API 상세 사용법: `ROUTE_API_TEST_GUIDE.md`
- Swagger 문서: http://localhost:8080/swagger-ui.html

---

## 변경 파일 목록

### 새로 추가된 파일
- `src/main/java/sandri/sandriweb/domain/route/entity/Route.java`
- `src/main/java/sandri/sandriweb/domain/route/entity/RouteParticipant.java`
- `src/main/java/sandri/sandriweb/domain/route/entity/RouteLocation.java`
- `src/main/java/sandri/sandriweb/domain/route/repository/RouteRepository.java`
- `src/main/java/sandri/sandriweb/domain/route/repository/RouteParticipantRepository.java`
- `src/main/java/sandri/sandriweb/domain/route/dto/CreateRouteRequestDto.java`
- `src/main/java/sandri/sandriweb/domain/route/dto/UpdateRouteRequestDto.java`
- `src/main/java/sandri/sandriweb/domain/route/dto/RouteResponseDto.java`
- `src/main/java/sandri/sandriweb/domain/route/dto/AddParticipantRequestDto.java`
- `src/main/java/sandri/sandriweb/domain/route/dto/ShareLinkResponseDto.java`
- `src/main/java/sandri/sandriweb/domain/route/service/RouteService.java`
- `src/main/java/sandri/sandriweb/domain/route/controller/RouteController.java`
- `src/main/java/sandri/sandriweb/domain/route/util/QrCodeGenerator.java`
- `ROUTE_API_TEST_GUIDE.md`

### 수정된 파일
- `build.gradle` - QR 코드 라이브러리 의존성 추가
- `src/main/resources/application.yml` - Google Maps API 및 공유 링크 URL 설정
- `src/main/java/sandri/sandriweb/config/SecurityConfig.java` - 공유 링크 접근 허용

