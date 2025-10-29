# Route API 테스트 가이드

## 사전 준비사항

### 1. 데이터베이스 준비
- MySQL 데이터베이스가 실행 중이어야 합니다
- 데이터베이스 이름: `sandri`
- 연결 정보는 `application.yml`에서 확인하세요

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

또는 IDE에서 `SandriWebApplication` 실행

### 3. Swagger UI 접속
애플리케이션 실행 후 브라우저에서 접속:
```
http://localhost:8080/swagger-ui.html
```

## 테스트 시나리오

### Step 1: 사용자 로그인 또는 회원가입

#### 1-1. 회원가입 (처음 사용하는 경우)
**POST** `/api/auth/register`

Request Body:
```json
{
  "username": "testuser",
  "password": "test1234",
  "confirmPassword": "test1234",
  "name": "테스트유저",
  "nickname": "testuser",
  "birthDate": "1990-01-01",
  "gender": "MALE",
  "location": "서울"
}
```

#### 1-2. 로그인
**POST** `/api/auth/login`

Request Body:
```json
{
  "username": "testuser",
  "password": "test1234"
}
```

**중요**: 로그인 후 세션 쿠키를 저장해야 인증된 API를 사용할 수 있습니다.

### Step 2: 루트 생성

**POST** `/api/routes`

Request Body 예시:
```json
{
  "title": "뚜벅뚜벅 산책 루트",
  "description": "서울 한강 공원 산책 코스",
  "startDate": "2025-07-17",
  "endDate": "2025-07-18",
  "isPublic": false,
  "locations": [
    {
      "dayNumber": 1,
      "name": "반곡지",
      "address": "서울특별시 강서구",
      "latitude": 37.5665,
      "longitude": 126.9780,
      "description": "한강 공원 산책 시작점",
      "displayOrder": 1
    },
    {
      "dayNumber": 1,
      "name": "여의도 공원",
      "address": "서울특별시 영등포구",
      "latitude": 37.5275,
      "longitude": 126.9250,
      "description": "산책 중간 지점",
      "displayOrder": 2
    }
  ]
}
```

Response에서 `id` 값을 확인하세요 (다음 단계에서 사용).

### Step 3: 루트 조회

**GET** `/api/routes/{routeId}`

예: `/api/routes/1`

### Step 4: 내 루트 목록 조회

**GET** `/api/routes/my`

### Step 5: 루트 수정

**PUT** `/api/routes/{routeId}`

Request Body:
```json
{
  "title": "수정된 루트 제목",
  "description": "수정된 설명",
  "startDate": "2025-07-17",
  "endDate": "2025-07-19"
}
```

### Step 6: 일행 추가

먼저 추가할 사용자를 생성하고 로그인한 상태여야 합니다.

**POST** `/api/routes/{routeId}/participants`

Request Body:
```json
{
  "userId": 2
}
```

### Step 7: 일행 목록 조회

**GET** `/api/routes/{routeId}/participants`

### Step 8: 일행 삭제

**DELETE** `/api/routes/{routeId}/participants?participantIds=2&participantIds=3`

### Step 9: 공유 링크 생성 (QR 코드 포함)

**GET** `/api/routes/{routeId}/share`

Response 예시:
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "shareUrl": "http://localhost:8080/routes/share/route_1234567890_1234",
    "shareCode": "route_1234567890_1234",
    "qrCodeUrl": "data:image/png;base64,iVBORw0KGgoAAAANS..."
  }
}
```

### Step 10: 공유 코드로 루트 조회 (인증 불필요)

**GET** `/api/routes/share/{shareCode}`

예: `/api/routes/share/route_1234567890_1234`

### Step 11: 루트 삭제

**DELETE** `/api/routes/{routeId}`

## Postman으로 테스트하기

### 1. Collection 설정
1. Postman에서 새 Collection 생성: "Route API Test"
2. 환경 변수 설정:
   - `baseUrl`: `http://localhost:8080`
   - `routeId`: (루트 생성 후 업데이트)

### 2. 쿠키 저장
- 로그인 API 실행 후 **Cookies** 탭에서 세션 쿠키 확인
- Postman이 자동으로 쿠키를 관리합니다

### 3. 순차적으로 테스트
위 Step 순서대로 API를 테스트하세요.

## 주의사항

1. **인증**: 대부분의 API는 인증이 필요합니다. 먼저 로그인을 해야 합니다.
2. **세션 쿠키**: 브라우저 또는 Postman에서 세션 쿠키가 자동으로 관리되어야 합니다.
3. **데이터베이스**: `ddl-auto: create-drop` 설정으로 테스트 시 데이터가 초기화될 수 있습니다.
4. **좌표 정보**: Google Maps API 키 없이도 테스트 가능합니다. 좌표는 수동으로 입력하거나 클라이언트에서 받아서 사용하세요.

## Google Maps API 키 (선택사항)

현재 서버 테스트에는 필요 없지만, 나중에 주소→좌표 변환 등이 필요하면:

1. [Google Cloud Console](https://console.cloud.google.com/)에서 API 키 발급
2. 환경 변수 설정:
   ```bash
   export GOOGLE_MAPS_API_KEY=your-api-key-here
   ```
3. 또는 `application.yml`에 직접 입력 (보안상 권장하지 않음)

## 문제 해결

### 인증 실패
- 로그인이 제대로 되었는지 확인
- 세션 쿠키가 전송되는지 확인

### 데이터베이스 연결 실패
- MySQL이 실행 중인지 확인
- `application.yml`의 데이터베이스 연결 정보 확인

### QR 코드 생성 실패
- ZXing 라이브러리가 제대로 빌드되었는지 확인
- `./gradlew build` 실행하여 의존성 확인

