# Postman API 테스트 예시

## 기본 설정
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **인증**: Spring Security (Session 기반)

---

## 1. 인증 (Auth)

### 1.1 로그인
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "testpass"
}
```

**Request Body:**
- `username` (required): 사용자 아이디 (최대 30자)
- `password` (required): 비밀번호 (8-20자)

**응답 예시:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "user": {
      "id": 1,
      "name": "홍길동",
      "nickname": "홍길동",
      "username": "testuser",
      "birthDate": "1990-01-01",
      "gender": "MALE",
      "location": "경산시",
      "enabled": true
    }
  }
}
```

**참고:**
- 성공 시: 세션 쿠키 자동 설정 (JSESSIONID)
- 실패 시: 400 Bad Request 또는 401 Unauthorized

---

## 2. 장소 (Place)

### 2.1 장소 상세 조회
```
GET http://localhost:8080/api/places/1
```

**설명:**
- 장소의 기본 정보만 반환합니다 (리뷰 정보 제외)
- 리뷰 정보가 필요하면 `/api/places/{placeId}/reviews` API를 별도로 호출하세요
- 리뷰 사진이 필요하면 `/api/places/{placeId}/reviews/photos` API를 별도로 호출하세요
- 근처 가볼만한 곳이 필요하면 `/api/places/{placeId}/nearby` API를 별도로 호출하세요

**반환 정보:**
- 장소 기본 정보 (이름, 주소, 좌표)
- 평점 (rating)
- 대분류 (groupName: 관광지/맛집/카페)
- 세부 카테고리 (categoryName: 자연/힐링, 역사/전통, 문화/체험, 식도락)
- 공식 사진들 (officialPhotos)
- 요약 (summary)
- 상세 정보 (information)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "placeId": 1,
    "name": "경주 불국사",
    "groupName": "관광지",
    "categoryName": "역사/전통",
    "rating": 4.5,
    "address": "경상북도 경주시 불국로 385",
    "latitude": 35.7894,
    "longitude": 129.3320,
    "summary": "신라 불교 문화의 정수를 보여주는 사찰",
    "information": "불국사는 신라 경덕왕 10년(751)에 김대성이 창건을 시작하여...",
    "officialPhotos": [
      {
        "order": 0,
        "photoUrl": "https://s3.../photo1.jpg"
      },
      {
        "order": 1,
        "photoUrl": "https://s3.../photo2.jpg"
      }
    ]
  }
}
```

**프론트엔드 사용 예시:**
```javascript
// 1. 장소 기본 정보 조회
const placeResponse = await fetch('http://localhost:8080/api/places/1');
const placeData = await placeResponse.json();

// 2. 근처 가볼만한 곳 조회 (필요한 경우)
// 거리 순 조회
const nearbyResponse = await fetch('http://localhost:8080/api/places/1/nearby?count=10');
// 대분류별 조회 (좋아요 순)
// const nearbyResponse = await fetch('http://localhost:8080/api/places/1/nearby/group?group=맛집&count=6');
const nearbyData = await nearbyResponse.json();

// 3. 리뷰 목록 조회 (필요한 경우) - 커서 기반 페이징
const reviewsResponse = await fetch('http://localhost:8080/api/places/1/reviews?size=10&sort=latest');
const reviewsData = await reviewsResponse.json();
// 다음 페이지: reviewsData.data.nextCursor가 있으면
// const nextPageResponse = await fetch(`http://localhost:8080/api/places/1/reviews?lastReviewId=${reviewsData.data.nextCursor}&size=10&sort=latest`);

// 4. 리뷰 사진 조회 (필요한 경우) - 커서 기반 페이징
const photosResponse = await fetch('http://localhost:8080/api/places/1/reviews/photos?size=20');
const photosData = await photosResponse.json();
// 다음 페이지: photosData.data.nextCursor가 있으면
// const nextPhotosResponse = await fetch(`http://localhost:8080/api/places/1/reviews/photos?lastPhotoId=${photosData.data.nextCursor}&size=20`);
```

### 2.2 근처 가볼만한 곳 조회 (거리 순)
```
GET http://localhost:8080/api/places/1/nearby?count=10
```

**Query Parameters:**
- `count` (optional, default: 10): 조회할 개수

**설명:**
- 지도 아래 주변 탐색 버튼을 눌렀을 때 출력할 관광지를 조회합니다.
- 대분류 필터 없이 전체에서 가까운 순으로 조회합니다.
- 현재 장소 포함 (rank 0)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeId": 1,
      "name": "경주 불국사",
      "thumbnailUrl": "https://s3.../photo.jpg",
      "latitude": 35.7894,
      "longitude": 129.3320,
      "distanceInMeters": 0,
      "rank": 0
    },
    {
      "placeId": 2,
      "name": "경주 석굴암",
      "thumbnailUrl": "https://s3.../photo.jpg",
      "latitude": 35.8000,
      "longitude": 129.3400,
      "distanceInMeters": 1500,
      "rank": 1
    }
  ]
}
```

### 2.3 근처 가볼만한 곳 조회 (대분류별, 좋아요 순)
```
GET http://localhost:8080/api/places/1/nearby/group?group=맛집&count=6
```

**Query Parameters:**
- `group` (required): 대분류 (관광지, 맛집, 카페)
- `count` (optional, default: 6): 조회할 개수

**설명:**
- 관광지 상세페이지 하단의 "이 근처의 가볼만한 곳"에서 호출합니다.
- 현재 관광지에서 10km 이내이고 대분류에 속하는 관광지 중 좋아요가 높은 순으로 반환합니다.
- 기본값 6개 반환 (3개씩 출력하도록 설계됨)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeId": 2,
      "name": "경주 맛집",
      "thumbnailUrl": "https://s3.../photo.jpg",
      "latitude": 35.8000,
      "longitude": 129.3400,
      "distanceInMeters": 2500,
      "rank": 1
    },
    {
      "placeId": 3,
      "name": "경주 카페",
      "thumbnailUrl": "https://s3.../photo.jpg",
      "latitude": 35.8100,
      "longitude": 129.3500,
      "distanceInMeters": 3200,
      "rank": 2
    }
  ]
}
```

### 2.4 카테고리별 장소 조회
```
GET http://localhost:8080/api/places?category=자연/힐링&count=10
```

**Query Parameters:**
- `category` (required): 카테고리 (자연/힐링, 역사/전통, 문화/체험, 식도락)
- `count` (optional, default: 10): 조회할 개수

**참고**: 로그인한 경우 자동으로 `isLiked` 정보 포함

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeId": 1,
      "name": "경주 불국사",
      "address": "경상북도 경주시 불국로 385",
      "thumbnailUrl": "https://s3.../photo.jpg",
      "isLiked": true,
      "groupName": "관광지",
      "categoryName": "역사/전통"
    }
  ]
}
```

### 2.5 장소 좋아요 토글
```
POST http://localhost:8080/api/places/1/like
```
**인증 필요**: 로그인 필수

**응답 예시:**
```json
{
  "success": true,
  "message": "좋아요가 추가되었습니다.",
  "data": true
}
```

---

## 3. 리뷰 (Review)

### 3.1 리뷰 목록 조회 (커서 기반 페이징)
```
GET http://localhost:8080/api/places/1/reviews?size=10&sort=latest
```

**Query Parameters:**
- `lastReviewId` (optional): 마지막으로 조회한 리뷰 ID (첫 조회시 생략, 다음 페이지 조회시 사용)
- `size` (optional, default: 10): 페이지 크기 (한 번에 조회할 개수)
- `sort` (optional, default: latest): 정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [
      {
        "reviewId": 1,
        "user": {
          "userId": 1,
          "username": "testuser",
          "profileImageUrl": "https://s3.../profile.jpg"
        },
        "content": "정말 좋은 장소였습니다!",
        "rating": 5,
        "createdAt": "2024-11-05T10:30:00",
        "photos": [
          {
            "photoUrl": "https://s3.../photo1.jpg",
            "order": 0
          },
          {
            "photoUrl": "https://s3.../photo2.jpg",
            "order": 1
          }
        ]
      }
    ],
    "size": 10,
    "nextCursor": 123,  // 다음 페이지 조회시 사용할 리뷰 ID (null이면 더 이상 없음)
    "hasNext": true,
    "totalCount": 50  // 전체 리뷰 개수
  }
}
```

**다음 페이지 조회:**
```
GET http://localhost:8080/api/places/1/reviews?lastReviewId=123&size=10&sort=latest
```

### 3.2 리뷰 사진 목록 조회 (커서 기반 페이징)
```
GET http://localhost:8080/api/places/1/reviews/photos?size=20
```

**Query Parameters:**
- `lastPhotoId` (optional): 마지막으로 조회한 사진 ID (첫 조회시 생략, 다음 페이지 조회시 사용)
- `size` (optional, default: 20): 페이지 크기 (한 번에 조회할 개수)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [
      {
        "photoUrl": "https://s3.../photo1.jpg",
        "order": 0
      },
      {
        "photoUrl": "https://s3.../photo2.jpg",
        "order": 1
      }
    ],
    "size": 20,
    "nextCursor": 123,  // 다음 페이지 조회시 사용할 사진 ID (null이면 더 이상 없음)
    "hasNext": true,
    "totalCount": 100  // 전체 리뷰 사진 개수
  }
}
```

**다음 페이지 조회:**
```
GET http://localhost:8080/api/places/1/reviews/photos?lastPhotoId=123&size=20
```

### 3.3 Presigned URL 발급 (리뷰 사진/영상 업로드용)
```
POST http://localhost:8080/api/me/files
Content-Type: application/json

{
  "files": [
    {
      "fileName": "photo1.jpg",
      "contentType": "image/jpeg",
      "order": 0
    },
    {
      "fileName": "photo2.png",
      "contentType": "image/png",
      "order": 1
    }
  ]
}
```
**인증 필요**: 로그인 필수

**Request Body:**
- `files` (required): 파일 정보 리스트 (최소 1개, 최대 10개)
  - `fileName` (required): 파일명
  - `contentType` (required): 파일 타입 (예: `image/jpeg`, `image/png`, `video/mp4`)
  - `order` (required): 사진 순서 (0부터 시작)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "presignedUrls": [
      {
        "fileName": "reviews/1234567890_abc123_photo1.jpg",
        "presignedUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/reviews/...?X-Amz-Algorithm=...",
        "finalUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/reviews/1234567890_abc123_photo1.jpg",
        "order": 0
      },
      {
        "fileName": "reviews/1234567890_def456_photo2.png",
        "presignedUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/reviews/...?X-Amz-Algorithm=...",
        "finalUrl": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/reviews/1234567890_def456_photo2.png",
        "order": 1
      }
    ]
  }
}
```

**프론트엔드 업로드 워크플로우:**

1. **Presigned URL 발급** (위 API 호출)
   - 백엔드에 파일명과 contentType을 전송
   - 백엔드가 각 파일에 대한 Presigned URL과 finalUrl을 반환

2. **각 Presigned URL로 S3에 직접 파일 업로드** (PUT 요청, **백엔드를 거치지 않음**):
   ```javascript
   // 각 파일에 대해 Presigned URL로 직접 S3에 업로드
   const files = [file1, file2]; // File 객체들
   const fileInfos = files.map((file, index) => ({
     fileName: file.name,
     contentType: file.type,  // 예: "image/jpeg", "image/png"
     order: index  // 0부터 시작하는 순서
   }));
   
   // 1단계: Presigned URL 발급
   const response = await fetch('/api/me/files', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ files: fileInfos })
   });
   const { data } = await response.json();
   
   // 2단계: 각 파일을 S3에 직접 업로드
   for (let i = 0; i < files.length; i++) {
     const file = files[i];
     const presignedUrlData = data.presignedUrls[i];
     const contentType = fileInfos[i].contentType;  // 발급 요청 시 보낸 contentType 사용
     
     // Presigned URL로 PUT 요청 (백엔드 서버를 거치지 않고 S3에 직접 업로드)
     const uploadResponse = await fetch(presignedUrlData.presignedUrl, {
       method: 'PUT',
       headers: {
         'Content-Type': contentType  // 발급 요청 시 보낸 contentType과 동일해야 함
       },
       body: file  // File 객체 (Blob)
     });
     
     if (!uploadResponse.ok) {
       throw new Error(`파일 업로드 실패: ${file.name}`);
     }
     
     // ⚠️ 중요: S3는 업로드 성공 시 HTTP 200만 반환합니다 (body 없음)
     // finalUrl은 1단계에서 이미 받았으므로, 여기서는 업로드 성공 여부만 확인하면 됩니다
   }
   ```

3. **리뷰 작성 시 finalUrl과 order 사용**:
   ```javascript
   // 업로드 완료 후 finalUrl과 order를 추출하여 리뷰 작성 API에 전송
   const photos = response.data.presignedUrls.map(item => ({
     photoUrl: item.finalUrl,  // S3에 업로드된 파일의 최종 URL
     order: item.order  // 사진 순서
   }));
   
   // 리뷰 작성 API 호출
   await fetch('/api/places/1/reviews', {
     method: 'POST',
     headers: {
       'Content-Type': 'application/json'
     },
     body: JSON.stringify({
       rating: 5,
       content: "정말 좋은 장소였습니다!",
       photos: photos  // photoUrl과 order를 포함한 사진 정보 리스트
     })
   });
   ```

**⚠️ 중요 사항:**
- Presigned URL은 **임시 URL**이며, 보통 5분 정도의 유효 기간이 있습니다.
- 파일 업로드는 **백엔드 서버를 거치지 않고** 프론트엔드에서 **S3에 직접** 업로드됩니다.
- 업로드 시 `Content-Type` 헤더는 Presigned URL 발급 시 전송한 `contentType`과 동일해야 합니다.


### 3.4 리뷰 작성
```
POST http://localhost:8080/api/places/1/reviews
Content-Type: application/json

{
  "rating": 5,
  "content": "정말 좋은 장소였습니다!",
  "photos": [
    {
      "photoUrl": "https://s3.../photo1.jpg",
      "order": 0
    },
    {
      "photoUrl": "https://s3.../photo2.jpg",
      "order": 1
    }
  ]
}
```
**인증 필요**: 로그인 필수

**Request Body:**
- `rating` (required): 별점 (1-5)
- `content` (required): 리뷰 내용 (최대 1000자)
- `photos` (optional): 사진 정보 리스트
  - `photoUrl` (required): AWS S3에 업로드된 사진/영상 URL
  - `order` (required): 사진 순서 (0부터 시작)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": 1  // 작성된 리뷰 ID
}
```

**참고**: 작성한 리뷰의 상세 정보가 필요하면 `GET /api/me/reviews/{reviewId}` API를 호출하세요.

### 3.5 내가 작성한 리뷰 상세 조회(! 활용처는 X, 리뷰 수정용)
```
GET http://localhost:8080/api/me/reviews/1
```
**인증 필요**: 로그인 필수

### 3.6 내가 작성한 리뷰 목록 조회 (커서 기반 페이징)
```
GET http://localhost:8080/api/me/reviews?size=10
```
**인증 필요**: 로그인 필수

**Query Parameters:**
- `lastReviewId` (optional): 마지막으로 조회한 리뷰 ID (첫 조회시 생략, 다음 페이지 조회시 사용)
- `size` (optional, default: 10): 페이지 크기 (한 번에 조회할 개수)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [
      {
        "reviewId": 1,
        "user": null,  // 내가 작성한 리뷰이므로 사용자 정보 제외
        "content": "정말 좋은 장소였습니다!",
        "rating": 5,
        "createdAt": "2024-11-05T10:30:00",
        "photos": [
          {
            "photoUrl": "https://s3.../photo1.jpg",
            "order": 0
          }
        ]
      }
    ],
    "size": 10,
    "nextCursor": 123,  // 다음 페이지 조회시 사용할 리뷰 ID (null이면 더 이상 없음)
    "hasNext": true
  }
}
```

**참고**: 내가 작성한 리뷰 목록이므로 사용자 정보(`user`)는 `null`로 반환됩니다.

**다음 페이지 조회:**
```
GET http://localhost:8080/api/me/reviews?lastReviewId=123&size=10
```

### 3.7 리뷰 수정
```
PUT http://localhost:8080/api/me/reviews/1
Content-Type: application/json

{
  "rating": 4,
  "content": "수정된 리뷰 내용입니다.",
  "photos": [
    {
      "photoUrl": "https://s3.../new-photo.jpg",
      "order": 0
    }
  ]
}
```
**인증 필요**: 로그인 필수

**Request Body:**
- `rating` (required): 별점 (1-5)
- `content` (required): 리뷰 내용 (최대 1000자)
- `photos` (optional): 사진 정보 리스트
  - `photoUrl` (required): AWS S3에 업로드된 사진/영상 URL
  - `order` (required): 사진 순서 (0부터 시작)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": 1  // 수정된 리뷰 ID
}
```

**참고**: 
- 기존 사진 엔티티는 모두 삭제되고 새로운 사진으로 교체됩니다.
- S3에 저장된 실제 파일은 삭제되지 않습니다.
- 수정된 리뷰의 상세 정보가 필요하면 `GET /api/me/reviews/{reviewId}` API를 호출하세요.

### 3.8 리뷰 삭제
```
DELETE http://localhost:8080/api/me/reviews/1
```
**인증 필요**: 로그인 필수

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": null
}
```

**참고**: 연결된 사진 엔티티는 삭제되지만, S3에 저장된 실제 파일은 삭제되지 않습니다.

---

## 4. 매거진 (Magazine)

### 4.1 매거진 상세 조회
```
GET http://localhost:8080/api/magazines/1
```

### 4.2 매거진 목록 조회 (커서 기반 페이징)
```
GET http://localhost:8080/api/magazines?size=10
```

**Query Parameters:**
- `lastMagazineId` (optional): 마지막으로 조회한 매거진 ID (첫 조회시 생략, 다음 페이지 조회시 사용)
- `size` (optional, default: 10): 페이지 크기 (한 번에 조회할 개수)

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "magazines": [
      {
        "magazineId": 1,
        "title": "경주 여행 완벽 가이드",
        "thumbnail": "https://s3.../card1.jpg",
        "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
        "isLiked": true
      }
    ],
    "size": 10,
    "nextCursor": 123,  // 다음 페이지 조회시 사용할 매거진 ID (null이면 더 이상 없음)
    "hasNext": true,
    "totalCount": 50  // 전체 매거진 개수
  }
}
```

**다음 페이지 조회:**
```
GET http://localhost:8080/api/magazines?lastMagazineId=123&size=10
```

**참고**: 
- 로그인한 경우 자동으로 `isLiked` 정보 포함
- ID 기준 내림차순으로 정렬되어 반환됩니다 (최신순)

### 4.3 매거진 좋아요 토글
```
POST http://localhost:8080/api/magazines/1/like
```
**인증 필요**: 로그인 필수

**응답 예시:**
```json
{
  "success": true,
  "message": "좋아요가 추가되었습니다.",
  "data": true
}
```

---

## 5. 광고 (Advertise)

### 5.1 공식 광고 조회
```
GET http://localhost:8080/api/advertise/official
```

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "totalCount": 2,
    "ads": [
      {
        "adId": 1,
        "imageUrl": "https://s3.../ad1.jpg",
        "title": "광고 제목",
        "description": "광고 설명",
        "linkUrl": "https://..."
      }
    ]
  }
}
```

### 5.2 개인 광고 조회
```
GET http://localhost:8080/api/advertise/private
```

---

## 6. 관리자 (Admin) - 데이터 생성

**참고**: 관리자 API는 **인증 없이** 사용할 수 있습니다.

### 6.1 장소 생성
```
POST http://localhost:8080/api/admin/places
Content-Type: application/json

{
  "name": "경주 불국사",
  "address": "경상북도 경주시 불국로 385",
  "latitude": 35.7894,
  "longitude": 129.3320,
  "summary": "신라 불교 문화의 정수를 보여주는 사찰",
  "information": "불국사는 신라 경덕왕 10년(751)에 김대성이 창건을 시작하여...",
  "group": "관광지",
  "category": "역사_전통"
}
```

**⚠️ 중요**: POST 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다. URL에 쿼리 파라미터로 넣으면 안 됩니다!

**Postman 사용법:**
1. Method: `POST` 선택
2. URL: **전체 URL 입력** `http://localhost:8080/api/admin/places`
   - ⚠️ 주의: `http:///api/admin/places` (X) - 호스트가 없으면 안 됩니다!
   - ✅ 올바름: `http://localhost:8080/api/admin/places`
3. Headers 탭에서 `Content-Type: application/json` 추가
4. Body 탭 선택 → `raw` 선택 → `JSON` 선택
5. 아래 JSON 데이터를 Body에 복사하여 붙여넣기

**Request Body:**
- `name` (required): 장소 이름
- `address` (optional): 한글 주소
- `latitude` (required): 위도
- `longitude` (required): 경도
- `summary` (optional): 요약
- `information` (optional): 상세 정보
- `group` (required): 대분류 (`관광지`, `맛집`, `카페`)
- `category` (required): 세부 카테고리 (`자연_힐링`, `역사_전통`, `문화_체험`, `식도락`)

**응답 예시:**
```json
{
  "success": true,
  "message": "장소가 생성되었습니다.",
  "data": 1
}
```

### 6.2 장소 사진 추가
```
POST http://localhost:8080/api/admin/places/1/photos
Content-Type: application/json

{
  "photoUrl": "https://s3.../place-photo1.jpg"
}
```

**Path Parameters:**
- `placeId` (required): 장소 ID

**Request Body:**
- `photoUrl` (required): 사진 URL (AWS S3 URL)

**응답 예시:**
```json
{
  "success": true,
  "message": "장소 사진이 추가되었습니다.",
  "data": 1
}
```

### 6.3 매거진 생성
```
POST http://localhost:8080/api/admin/magazines
Content-Type: application/json

{
  "name": "경주 여행 완벽 가이드",
  "summary": "경주의 대표 관광지를 한눈에",
  "content": "경주는 신라 천년의 고도로..."
}
```

**⚠️ 중요**: POST 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Postman 사용법:**
1. Method: `POST` 선택
2. URL: `http://localhost:8080/api/admin/magazines`
3. Headers 탭에서 `Content-Type: application/json` 추가
4. Body 탭 선택 → `raw` 선택 → `JSON` 선택
5. 아래 JSON 데이터를 Body에 복사하여 붙여넣기

**Request Body:**
- `name` (required): 매거진 이름
- `summary` (optional): 매거진 요약
- `content` (optional): 매거진 내용
- `cardUrls` (optional): 매거진 카드 이미지 URL 리스트 (순서대로 저장됨)

**전체 예시 (카드 포함):**
```json
{
  "name": "경주 여행 완벽 가이드",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로, 불국사와 석굴암을 비롯한 많은 문화유산이 있습니다. 이 매거진에서는 경주의 주요 관광지를 소개합니다.",
  "cardUrls": [
    "https://s3.../magazine-card1.jpg",
    "https://s3.../magazine-card2.jpg",
    "https://s3.../magazine-card3.jpg"
  ]
}
```

**최소 예시 (필수 필드만):**
```json
{
  "name": "경주 관광 가이드"
}
```

**카드만 포함한 예시:**
```json
{
  "name": "경주 관광 가이드",
  "cardUrls": [
    "https://s3.../card1.jpg",
    "https://s3.../card2.jpg"
  ]
}
```

**참고:**
- `cardUrls` 배열의 순서대로 카드가 저장됩니다 (order 필드에 0부터 순서대로 저장됨)
- 카드 수정은 매거진 수정 API(`PUT /api/admin/magazines/{magazineId}`)를 사용하세요

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진이 생성되었습니다.",
  "data": 1
}
```

**테스트 시나리오:**

1. **정상 생성:**
   - Request Body에 `name` 포함하여 전송
   - 예상 응답: `200 OK` + 생성된 매거진 ID

2. **필수 필드 누락:**
   - Request Body에서 `name` 제거
   - 예상 응답: `400 Bad Request` + 에러 메시지

3. **빈 문자열:**
   - `name: ""` 전송
   - 예상 응답: `400 Bad Request` + 에러 메시지

### 6.4 매거진 수정
```
PUT http://localhost:8080/api/admin/magazines/{magazineId}
Content-Type: application/json

{
  "name": "경주 여행 완벽 가이드 (수정)",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로...",
  "cardUrls": [
    "https://s3.../magazine-card1.jpg",
    "https://s3.../magazine-card2.jpg",
    "https://s3.../magazine-card3.jpg"
  ]
}
```

**⚠️ 중요**: PUT 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Postman 사용법:**
1. Method: `PUT` 선택
2. URL: `http://localhost:8080/api/admin/magazines/{magazineId}` (예: `http://localhost:8080/api/admin/magazines/1`)
3. Headers 탭에서 `Content-Type: application/json` 추가
4. Body 탭 선택 → `raw` 선택 → `JSON` 선택
5. 아래 JSON 데이터를 Body에 복사하여 붙여넣기

**Request Body:**
- `name` (required): 매거진 이름
- `summary` (optional): 매거진 요약
- `content` (optional): 매거진 내용
- `cardUrls` (optional): 매거진 카드 이미지 URL 리스트 (기존 카드는 모두 삭제되고 새로운 카드로 교체됨)

**전체 예시:**
```json
{
  "name": "경주 여행 완벽 가이드 (수정)",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로, 불국사와 석굴암을 비롯한 많은 문화유산이 있습니다.",
  "cardUrls": [
    "https://s3.../magazine-card1.jpg",
    "https://s3.../magazine-card2.jpg",
    "https://s3.../magazine-card3.jpg"
  ]
}
```

**카드만 교체하는 예시:**
```json
{
  "name": "경주 여행 완벽 가이드",
  "cardUrls": [
    "https://s3.../new-card1.jpg",
    "https://s3.../new-card2.jpg"
  ]
}
```

**정보만 수정하는 예시 (카드는 유지):**
```json
{
  "name": "경주 여행 완벽 가이드 (수정)",
  "summary": "새로운 요약",
  "content": "새로운 내용"
}
```

**⚠️ 주의사항:**
- `cardUrls`를 포함하면 기존 카드가 모두 삭제되고 새로운 카드로 교체됩니다
- `cardUrls`를 포함하지 않으면 카드는 변경되지 않습니다
- `cardUrls`는 빈 배열 `[]`로도 전송 가능합니다 (모든 카드 삭제)

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진이 수정되었습니다.",
  "data": 1
}
```

**테스트 시나리오:**

1. **정상 수정 (카드 포함):**
   - Request Body에 `name`과 `cardUrls` 포함하여 전송
   - 예상 응답: `200 OK` + 수정된 매거진 ID

2. **정보만 수정 (카드 유지):**
   - Request Body에 `name`, `summary`, `content`만 포함 (cardUrls 제외)
   - 예상 응답: `200 OK` + 수정된 매거진 ID

3. **카드만 교체:**
   - Request Body에 `name`과 `cardUrls` 포함
   - 예상 응답: `200 OK` + 기존 카드는 삭제되고 새로운 카드로 교체됨

4. **매거진 없음:**
   - 존재하지 않는 `magazineId` 전송
   - 예상 응답: `404 Not Found` + 에러 메시지

### 6.5 공식 광고 생성
```
POST http://localhost:8080/api/admin/advertise/official
Content-Type: application/json

{
  "title": "여름 휴가 특가",
  "description": "경주 호텔 최대 50% 할인",
  "imageUrl": "https://s3.../ad-summer.jpg",
  "linkUrl": "https://example.com/promotion",
  "startDate": "2024-07-01T00:00:00",
  "endDate": "2024-08-31T23:59:59",
  "displayOrder": 1
}
```

**Request Body:**
- `title` (required): 광고 제목
- `description` (optional): 광고 설명
- `imageUrl` (required): 광고 이미지 URL (AWS S3 URL)
- `linkUrl` (optional): 클릭 시 이동할 URL
- `startDate` (optional): 노출 시작일 (ISO 8601 형식)
- `endDate` (optional): 노출 종료일 (ISO 8601 형식)
- `displayOrder` (optional, default: 0): 표시 순서

**응답 예시:**
```json
{
  "success": true,
  "message": "공식 광고가 생성되었습니다.",
  "data": 1
}
```

### 6.6 개인 광고 생성
```
POST http://localhost:8080/api/admin/advertise/private
Content-Type: application/json

{
  "title": "맛집 추천",
  "description": "경주 맛집 리스트",
  "imageUrl": "https://s3.../ad-restaurant.jpg",
  "linkUrl": "https://example.com/restaurants",
  "startDate": "2024-07-01T00:00:00",
  "endDate": "2024-08-31T23:59:59",
  "displayOrder": 1
}
```

**Request Body:** (공식 광고와 동일)

**응답 예시:**
```json
{
  "success": true,
  "message": "개인 광고가 생성되었습니다.",
  "data": 1
}
```

### 6.6 전체 장소 목록 조회 (간단 정보, 관리자용)
```
GET http://localhost:8080/api/admin/places/list
```

**설명:**
- 전체 관광지의 ID와 이름만 반환합니다.
- 전체 DB 목록 확인용

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeId": 1,
      "name": "경주 불국사"
    },
    {
      "placeId": 2,
      "name": "경주 석굴암"
    }
  ]
}
```

### 6.7 전체 리뷰 목록 조회 (관리자용)
```
GET http://localhost:8080/api/admin/reviews/list
```

**설명:**
- 관리자가 전체 리뷰 목록을 조회합니다.
- 리뷰 ID와 리뷰 내용만 반환합니다.
- 활성화된 리뷰만 반환됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "reviewId": 1,
      "content": "정말 좋은 장소였습니다!"
    },
    {
      "reviewId": 2,
      "content": "다시 가고 싶은 곳입니다."
    }
  ]
}
```

---

## Postman Collection 설정 팁

### 1. Environment Variables 설정
Postman에서 Environment를 생성하고 다음 변수들을 설정하세요:

- `base_url`: `http://localhost:8080`
- `username`: 테스트용 사용자명
- `password`: 테스트용 비밀번호

### 2. Collection Pre-request Script
```javascript
// 모든 요청에 base_url 자동 적용
pm.environment.set("base_url", "http://localhost:8080");
```

### 3. 테스트 스크립트 예시 (로그인 후 자동 세션 저장)
```javascript
// 로그인 요청의 Tests 탭에 추가
if (pm.response.code === 200) {
    // 세션 쿠키가 자동으로 저장됩니다
    pm.test("로그인 성공", function () {
        pm.response.to.have.status(200);
    });
}
```

### 4. 인증이 필요한 API 테스트 순서
1. 먼저 로그인 API 호출 → 세션 쿠키 자동 저장
2. 이후 인증 필요한 API 호출 (세션 자동 사용)

---

## 테스트 시나리오 예시

### 시나리오 1: 장소 조회 및 좋아요
1. `GET /api/places?category=자연/힐링&count=5` - 카테고리별 장소 조회
2. `GET /api/places/1/nearby?count=10` - 근처 가볼만한 곳 조회 (거리 순)
3. `GET /api/places/1/nearby/group?group=맛집&count=6` - 근처 가볼만한 곳 조회 (대분류별, 좋아요 순)
4. `GET /api/places/list` - 전체 장소 목록 조회 (간단 정보)
5. `POST /api/places/1/like` - 좋아요 추가 (로그인 필요)

### 시나리오 2: 리뷰 작성 및 조회
1. `POST /api/auth/login` - 로그인
2. `POST /api/me/files` - Presigned URL 발급 (파일명, contentType, order 전송)
3. 각 Presigned URL로 PUT 요청 - S3에 직접 파일 업로드
4. `POST /api/places/1/reviews` - 리뷰 작성 (finalUrl과 order 사용)
5. `GET /api/places/1/reviews` - 리뷰 목록 조회 (커서 기반 페이징)
6. `GET /api/places/1/reviews/photos` - 리뷰 사진 목록 조회 (커서 기반 페이징)
7. `GET /api/me/reviews/1` - 내 리뷰 상세 조회
8. `PUT /api/me/reviews/1` - 리뷰 수정 (finalUrl과 order 사용)
9. `DELETE /api/me/reviews/1` - 리뷰 삭제

### 시나리오 3: 매거진 조회 및 좋아요
1. `GET /api/magazines?size=5` - 매거진 목록 조회 (첫 페이지)
2. `GET /api/magazines?lastMagazineId=123&size=5` - 매거진 목록 조회 (다음 페이지)
3. `GET /api/magazines/1` - 매거진 상세 조회
4. `POST /api/magazines/1/like` - 좋아요 토글 (로그인 필요)

### 시나리오 4: 관리자가 데이터 생성 (전체 플로우)
1. `POST /api/admin/places` - 장소 생성
3. `POST /api/admin/places/photos` - 장소 사진 추가 (여러 번)
4. `POST /api/admin/magazines` - 매거진 생성
5. `PUT /api/admin/magazines/1` - 매거진 수정 (카드 포함)
6. `POST /api/admin/advertise/official` - 공식 광고 생성
7. `GET /api/places/1` - 생성된 장소 확인
8. `GET /api/magazines/1` - 생성된 매거진 확인
9. `GET /api/advertise/official` - 생성된 광고 확인

---

## 주의사항

1. **인증**: 로그인이 필요한 API는 먼저 `/api/auth/login`을 호출해야 합니다.
   - 일반 사용자 API: `/api/me/**` (리뷰 작성/수정/삭제 등)
   - 관리자 API: `/api/admin/**` (데이터 생성)
2. **세션**: Spring Security는 세션 기반 인증을 사용하므로, 로그인 후 쿠키가 자동으로 저장됩니다.
3. **파일 업로드**: AWS S3가 아직 구현되지 않았다면 파일 업로드 API는 에러가 발생합니다.
4. **데이터베이스**: MySQL 데이터베이스가 실행 중이어야 합니다.
5. **환경 변수**: 다음 환경 변수가 설정되어 있어야 합니다:
   - `DATABASE_HOST`
   - `DATABASE_PORT`
   - `DATABASE_NAME`
   - `DATABASE_USER`
   - `DATABASE_PASS`
6. **관리자 API 사용 시**:
   - 관리자 API는 인증 없이 사용 가능합니다
   - 장소 생성 시 `group` 값: `"관광지"`, `"맛집"`, `"카페"` 중 하나
   - 장소 생성 시 `category` 값: `"자연_힐링"`, `"역사_전통"`, `"문화_체험"`, `"식도락"` 중 하나 (언더스코어 사용)
   - 매거진 카드는 `order` 필드에 따라 정렬됩니다 (0부터 시작)
   - 광고의 `startDate`와 `endDate`가 없으면 항상 노출됩니다

---

## Swagger UI 사용

Postman 대신 Swagger UI에서도 테스트할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```

Swagger UI에서는:
- 모든 API 엔드포인트 확인 가능
- 직접 테스트 가능 (로그인 기능 포함)
- API 문서 확인 가능

