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
- 관광지의 기본 정보를 조회합니다. 이름, 주소, 평점, 카테고리, 공식 사진을 반환합니다.
- 리뷰 정보는 `/api/places/{placeId}/reviews` API를 별도로 호출하세요.

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
- 특정 관광지 근처의 추천 장소 목록을 대분류 필터 없이 전체에서 가까운 순으로 조회합니다.
- count 값이 없을 시 기본값으로 조회합니다.
- 관광지 ID, 이름, 대표 사진 한 장, 위도/경도(지도 출력용), 현재 장소로부터 가까운 순위를 반환합니다. (0일 경우 현재 장소)

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
- 관광지 상세페이지 하단의 이 근처의 가볼만한 곳에서 호출하여 사용합니다.
- 현재 관광지에서 10km 이내이고 대분류에 속하는 관광지 중 좋아요가 높은 관광지 6개를 반환합니다.
- count 값이 없을 시 기본값으로 조회합니다.
- 3개씩 출력하게 되어있을텐데 원래는 버튼 누를 때마다 API 호출해야 하지만 편의상 6개를 한 번에 받고 3개씩 출력해주세요.

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

**설명:**
- 관광지 상세 페이지에서 호출합니다.
- 장소에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "좋아요가 추가되었습니다.",
  "data": true
}
```

### 2.6 HOT 관광지 조회
```
GET http://localhost:8080/api/places/hot
```

**설명:**
- 좋아요 수와 최근 활동 가중치를 기반으로 인기 관광지를 순위대로 반환합니다.
- 상위 5개의 HOT 관광지를 반환합니다.

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
      "address": "경상북도 경주시 불국로 385",
      "groupName": "관광지",
      "categoryName": "역사/전통",
      "rating": 4.5,
      "likeCount": 150,
      "hotScore": 95.5
    }
  ]
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

**설명:**
- 리뷰 작성 시 포인트가 자동으로 적립됩니다.
  - 사진이 포함된 리뷰: 사진 리뷰 포인트 적립
  - 사진이 없는 리뷰: 리뷰 포인트 적립
- 포인트 적립 실패 시에도 리뷰 작성은 정상적으로 완료됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": 1  // 작성된 리뷰 ID
}
```

**참고**: 
- 작성한 리뷰의 상세 정보가 필요하면 `GET /api/me/reviews/{reviewId}` API를 호출하세요.
- 리뷰 작성으로 적립된 포인트는 `GET /api/me/points/histories?type=EARN` API로 확인할 수 있습니다.

### 3.5 내가 작성한 리뷰 상세 조회(! 활용처는 X, 리뷰 수정용)
```
GET http://localhost:8080/api/me/reviews/1
```
**인증 필요**: 로그인 필수

**설명:**
- 리뷰 출력/리뷰 수정 시 호출합니다.
- 리뷰 ID, 리뷰 내용, 별점, 작성 일시, 사진 정보 리스트를 반환합니다.
- 작성자 정보는 null로 반환됩니다.

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

**설명:**
- 마이페이지: 내 리뷰 페이지에서 리뷰 수정 시 호출합니다.
- 별점, 리뷰 내용, 사진(순서, finalUrl)을 전송하면 리뷰를 수정하고 수정된 리뷰 ID를 반환합니다.
- finalUrl은 프론트에서 getPresignedUrls 호출 시 발급됩니다. 업로드에 성공한 finalUrl을 사용해주세요.
- 기존 사진 엔티티는 모두 삭제되고 새로운 사진으로 교체되지만, S3에 저장된 실제 파일은 삭제되지 않습니다.

**참고**: 
- 수정된 리뷰의 상세 정보가 필요하면 `GET /api/me/reviews/{reviewId}` API를 호출하세요.

### 3.8 리뷰 삭제
```
DELETE http://localhost:8080/api/me/reviews/1
```
**인증 필요**: 로그인 필수

**설명:**
- 마이페이지: 내 리뷰 페이지에서 리뷰 삭제 시 호출합니다.
- 연결된 사진 엔티티는 삭제되지만, S3에 저장된 실제 파일은 삭제되지 않습니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": null
}
```

---

## 4. 방문 기록 (Visit History)

### 4.1 장소 방문 확인 및 기록
```
POST http://localhost:8080/api/me/today/places/1
Content-Type: application/json

{
  "latitude": 35.8251,
  "longitude": 128.7405
}
```
**인증 필요**: 로그인 필수

**Path Parameters:**
- `placeId` (required): 장소 ID

**Request Body:**
- `latitude` (required): 현재 사용자 GPS 위도 (-90 ~ 90)
- `longitude` (required): 현재 사용자 GPS 경도 (-180 ~ 180)

**설명:**
- 홈: 오늘 일정: 방문 확정하기에서 호출합니다.
- 사용자의 현재 GPS 위치와 장소 위치의 거리를 계산하여 1km 이내인지 확인합니다.
- 조건에 맞으면 방문 기록을 저장하고 방문 여부를 반환합니다.
- 방문 성공 시 포인트가 자동으로 적립됩니다 (방문 포인트).

**응답 예시 (방문 성공):**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "visited": true,
    "visitHistoryId": 1
  }
}
```

**응답 예시 (방문 실패 - 거리 초과):**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "visited": false,
    "visitHistoryId": null
  }
}
```

**에러 응답:**
- `400 Bad Request`: 좌표 범위 초과, 필수 값 누락 등
- `401 Unauthorized`: 인증 필요
- `404 Not Found`: 사용자 또는 장소를 찾을 수 없음

### 4.2 오늘 일정 장소 조회
```
GET http://localhost:8080/api/me/today/places
```
**인증 필요**: 로그인 필수

**설명:**
- 로그인한 사용자가 참여한 루트 중 오늘 날짜에 해당하는 장소 목록을 조회합니다.
- 각 장소는 썸네일, 장소 이름, 한글 주소를 포함하며, 여행의 총 장소 개수와 해당 여행지의 방문 순서도 함께 반환됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeInfo": {
        "thumbnail": "https://s3.../photo.jpg",
        "placeName": "경복궁",
        "address": "서울특별시 종로구 사직로 161"
      },
      "totalPlaceCount": 5,
      "visitOrder": 1
    },
    {
      "placeInfo": {
        "thumbnail": "https://s3.../photo2.jpg",
        "placeName": "경복궁",
        "address": "서울특별시 종로구 사직로 161"
      },
      "totalPlaceCount": 5,
      "visitOrder": 2
    }
  ]
}
```

**에러 응답:**
- `401 Unauthorized`: 인증 필요
- `404 Not Found`: 사용자를 찾을 수 없음

---

## 5. 포인트 (Point)

### 5.1 포인트 히스토리 목록 조회
```
GET http://localhost:8080/api/me/points/histories?type=ALL
```
**인증 필요**: 로그인 필수

**Query Parameters:**
- `type` (optional, default: ALL): 조회 타입
  - `ALL`: 전체 조회 (기본값)
  - `EARN`: 적립 포인트만 조회
  - `USE`: 사용 포인트만 조회

**설명:**
- 로그인한 사용자의 포인트 적립/사용 내역 목록을 조회합니다.
- 최신순으로 정렬되어 반환됩니다.

**응답 예시 (전체 조회):**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "createdAt": "2024-11-06",
      "conditionTypeTitle": "방문 포인트",
      "pointAmount": 500,
      "balanceAfter": 1500
    },
    {
      "createdAt": "2024-11-05",
      "conditionTypeTitle": "회원가입",
      "pointAmount": 1000,
      "balanceAfter": 1000
    }
  ]
}
```

**응답 예시 (적립만 조회):**
```
GET http://localhost:8080/api/me/points/histories?type=EARN
```

**응답 예시 (사용만 조회):**
```
GET http://localhost:8080/api/me/points/histories?type=USE
```

**에러 응답:**
- `401 Unauthorized`: 인증 실패
- `404 Not Found`: 사용자 없음

### 5.2 소멸 예정 포인트 조회
```
GET http://localhost:8080/api/me/points/expiring-points
```
**인증 필요**: 로그인 필수

**설명:**
- 로그인한 사용자의 7일 이내 소멸 예정 포인트를 조회합니다.
- 포인트는 적립 후 30일이 지나면 소멸됩니다.
- 7일 이내 소멸 예정 = 적립일로부터 23~30일 사이인 포인트

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "expiringPointsWithin7Days": 500
  }
}
```

**에러 응답:**
- `401 Unauthorized`: 인증 실패
- `404 Not Found`: 사용자 없음

---

## 6. 매거진 (Magazine)

### 6.1 매거진 상세 조회
```
GET http://localhost:8080/api/magazines/1
```

**설명:**
- 매거진 상세 조회 시 호출합니다.
- 매거진 ID, 매거진 내용, 사용자 좋아요 여부, 카드뉴스 개수, 카드뉴스 리스트를 반환합니다.
- 카드뉴스 리스트에서는 카드뉴스 순서(order), 카드뉴스 URL(cardUrl)을 반환합니다.
- 로그인하지 않은 경우 좋아요 여부는 null입니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "magazineId": 1,
    "content": "경주는 신라 천년의 고도로, 불국사와 석굴암을 비롯한 많은 문화유산이 있습니다.",
    "cardCount": 5,
    "isLiked": true,
    "cards": [
      {
        "order": 0,
        "cardUrl": "https://s3.../card1.jpg"
      },
      {
        "order": 1,
        "cardUrl": "https://s3.../card2.jpg"
      }
    ]
  }
}
```

**참고**: 로그인한 경우 자동으로 `isLiked` 정보 포함

### 6.2 매거진 목록 조회 (커서 기반 페이징)
```
GET http://localhost:8080/api/magazines?size=10
```

**Query Parameters:**
- `lastMagazineId` (optional): 마지막으로 조회한 매거진 ID (첫 조회시 생략, 다음 페이지 조회시 사용)
- `size` (optional, default: 10): 페이지 크기 (한 번에 조회할 개수)

**설명:**
- 홈: 여행 매거진 모아보기 페이지에서 호출합니다.
- 매거진 목록을 커서 기반(마지막으로 호출한 매거진 ID + 추가로 호출할 매거진 개수를 받아 호출)으로 페이징하여 조회합니다.
- 매거진 객체, 반환한 매거진 개수(요청한 수), 마지막으로 호출한 매거진 ID(다음 요청 시 사용), 전체 매거진 개수를 반환합니다.
- 매거진 객체에서는 매거진 ID, 매거진 제목, 매거진 썸네일(첫 번째 카드 이미지), 매거진 요약, 매거진 태그, 사용자 좋아요 여부를 반환합니다.

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
        "tags": [
          {
            "tagId": 1,
            "name": "경주"
          },
          {
            "tagId": 2,
            "name": "관광지"
          }
        ],
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

### 6.3 매거진 좋아요 토글
```
POST http://localhost:8080/api/magazines/1/like
```
**인증 필요**: 로그인 필수

**설명:**
- 매거진 상세 페이지에서 호출합니다.
- 매거진에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "좋아요가 추가되었습니다.",
  "data": true
}
```

### 6.4 매거진 카드뉴스에 매핑된 장소 목록 조회
```
GET http://localhost:8080/api/magazines/1/places
```

**Query Parameters:**
- `thumbnailOnly` (optional, default: false): 썸네일만 반환 여부
- `count` (optional): 조회할 개수 (thumbnailOnly=true일 때만 적용)

**설명:**
- 매거진 상세: 마지막 페이지의 관광지 보러가기 버튼 상단 혹은 버튼을 눌렀을 때 호출합니다.
- 매거진 ID를 받아 해당 매거진의 카드뉴스에 연결된 Place를 반환합니다.
- `thumbnailOnly=true`인 경우: 장소 ID, 이름, 썸네일만 반환하며, `count` 파라미터로 개수를 제한할 수 있습니다.
- `thumbnailOnly=false`이거나 생략된 경우: 모든 필드를 포함한 SimplePlaceDto를 반환합니다. (distanceInMeters 필드는 null)
- 로그인한 경우 사용자가 좋아요한 장소 여부도 함께 반환됩니다.

**전체 정보 조회 예시:**
```
GET http://localhost:8080/api/magazines/1/places
```

**응답 예시 (전체 정보):**
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
      "categoryName": "역사/전통",
      "distanceInMeters": null
    }
  ]
}
```

**썸네일만 조회 예시:**
```
GET http://localhost:8080/api/magazines/1/places?thumbnailOnly=true&count=3
```

**응답 예시 (썸네일만):**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "placeId": 1,
      "name": "경주 불국사",
      "thumbnailUrl": "https://s3.../photo.jpg"
    },
    {
      "placeId": 2,
      "name": "경주 석굴암",
      "thumbnailUrl": "https://s3.../photo.jpg"
    }
  ]
}
```

---

## 8. 광고 (Advertise)

### 8.1 공식 광고 조회
```
GET http://localhost:8080/api/advertise/official
```

**설명:**
- 유효한 공식 광고 목록을 조회합니다.
- enabled=true이고 현재 시간이 노출 기간 내인 광고만 반환됩니다.

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

### 8.2 개인 광고 조회
```
GET http://localhost:8080/api/advertise/private
```

**설명:**
- 유효한 개인 광고 목록을 조회합니다.
- enabled=true이고 현재 시간이 노출 기간 내인 광고만 반환됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": {
    "totalCount": 1,
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

---

## 9. 관리자 (Admin) - 데이터 생성

**참고**: 관리자 API는 **인증 없이** 사용할 수 있습니다.

### 9.1 장소 생성
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

### 9.2 장소 사진 추가 (여러 장)
```
POST http://localhost:8080/api/admin/places/1/photos
Content-Type: application/json

{
  "photos": [
    {
      "photoUrl": "https://s3.../place-photo1.jpg",
      "order": 0
    },
    {
      "photoUrl": "https://s3.../place-photo2.jpg",
      "order": 1
    }
  ]
}
```

**Path Parameters:**
- `placeId` (required): 장소 ID

**Request Body:**
- `photos` (required): 사진 정보 리스트 (최소 1개)
  - `photoUrl` (required): 사진 URL (AWS S3 URL)
  - `order` (required): 사진 순서 (0부터 시작)

**응답 예시:**
```json
{
  "success": true,
  "message": "장소 사진이 추가되었습니다.",
  "data": [1, 2]  // 생성된 사진 ID 리스트
}
```

### 9.3 장소 수정
```
PUT http://localhost:8080/api/admin/places/1
Content-Type: application/json

{
  "name": "경주 불국사 (수정)",
  "address": "경상북도 경주시 불국로 385",
  "latitude": 35.7894,
  "longitude": 129.3320,
  "summary": "신라 불교 문화의 정수를 보여주는 사찰 (수정)",
  "information": "불국사는 신라 경덕왕 10년(751)에 김대성이 창건을 시작하여...",
  "group": "관광지",
  "category": "역사_전통",
  "photos": [
    {
      "photoUrl": "https://s3.../place-photo1.jpg",
      "order": 0
    },
    {
      "photoUrl": "https://s3.../place-photo2.jpg",
      "order": 1
    }
  ]
}
```

**⚠️ 중요**: PUT 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Postman 사용법:**
1. Method: `PUT` 선택
2. URL: `http://localhost:8080/api/admin/places/{placeId}` (예: `http://localhost:8080/api/admin/places/1`)
3. Headers 탭에서 `Content-Type: application/json` 추가
4. Body 탭 선택 → `raw` 선택 → `JSON` 선택
5. 아래 JSON 데이터를 Body에 복사하여 붙여넣기

**Path Parameters:**
- `placeId` (required): 장소 ID

**Request Body:**
- `name` (optional): 장소 이름
- `address` (optional): 한글 주소
- `latitude` (optional): 위도
- `longitude` (optional): 경도
- `summary` (optional): 요약
- `information` (optional): 상세 정보
- `group` (optional): 대분류 (`관광지`, `맛집`, `카페`)
- `category` (optional): 세부 카테고리 (`자연_힐링`, `역사_전통`, `문화_체험`, `식도락`)
- `photos` (optional): 사진 정보 리스트
  - `photoUrl` (required): 사진 URL (빈 문자열("")이면 해당 사진 비활성화)
  - `order` (required): 사진 순서 (0부터 시작)

**정보만 수정하는 예시 (사진 유지):**
```json
{
  "name": "경주 불국사 (수정)",
  "summary": "새로운 요약"
}
```

**사진만 수정하는 예시:**
```json
{
  "name": "경주 불국사",
  "photos": [
    {
      "photoUrl": "https://s3.../new-photo1.jpg",
      "order": 0
    },
    {
      "photoUrl": "https://s3.../new-photo2.jpg",
      "order": 1
    }
  ]
}
```

**사진 비활성화 예시:**
```json
{
  "name": "경주 불국사",
  "photos": [
    {
      "photoUrl": "",  // 빈 문자열이면 order 0의 사진이 비활성화됨
      "order": 0
    }
  ]
}
```

**⚠️ 주의사항:**
- 요청에 포함된 필드만 업데이트되며, 생략된 필드는 기존 값을 유지합니다.
- 사진 정보(photos)가 포함된 경우, order 값에 따라 기존 사진을 업데이트하거나 새로 생성하며, photoUrl이 빈 문자열("")이면 해당 order의 사진을 비활성화합니다.
- 요청에 없는 order의 사진은 유지됩니다 (비활성화되지 않음).
- `photos`를 포함하지 않으면 사진은 변경되지 않습니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "장소가 수정되었습니다.",
  "data": 1
}
```

### 9.4 매거진 생성
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
- `cards` (optional): 매거진 카드 정보 리스트
  - `order` (required): 카드 순서 (0부터 시작)
  - `cardUrl` (required): 카드 이미지 URL

**전체 예시 (카드 포함):**
```json
{
  "name": "경주 여행 완벽 가이드",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로, 불국사와 석굴암을 비롯한 많은 문화유산이 있습니다. 이 매거진에서는 경주의 주요 관광지를 소개합니다.",
  "cards": [
    {
      "order": 0,
      "cardUrl": "https://s3.../magazine-card1.jpg"
    },
    {
      "order": 1,
      "cardUrl": "https://s3.../magazine-card2.jpg"
    },
    {
      "order": 2,
      "cardUrl": "https://s3.../magazine-card3.jpg"
    }
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
  "cards": [
    {
      "order": 0,
      "cardUrl": "https://s3.../card1.jpg"
    },
    {
      "order": 1,
      "cardUrl": "https://s3.../card2.jpg"
    }
  ]
}
```

**참고:**
- `cards` 배열의 각 카드는 `order`와 `cardUrl`을 명시적으로 지정해야 합니다.
- 카드 수정은 매거진 수정 API(`PUT /api/admin/magazines/{magazineId}`)를 사용하세요.

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

### 9.5 매거진 수정
```
PUT http://localhost:8080/api/admin/magazines/1
Content-Type: application/json

{
  "name": "경주 여행 완벽 가이드 (수정)",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로...",
  "cards": [
    {
      "order": 0,
      "cardUrl": "https://s3.../magazine-card1.jpg"
    },
    {
      "order": 1,
      "cardUrl": "https://s3.../magazine-card2.jpg"
    }
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
- `cards` (optional): 매거진 카드 정보 리스트
  - `order` (required): 카드 순서 (0부터 시작)
  - `cardUrl` (required): 카드 이미지 URL (빈 문자열("")이면 해당 카드 비활성화)

**전체 예시:**
```json
{
  "name": "경주 여행 완벽 가이드 (수정)",
  "summary": "경주의 대표 관광지를 한눈에 볼 수 있는 가이드",
  "content": "경주는 신라 천년의 고도로, 불국사와 석굴암을 비롯한 많은 문화유산이 있습니다.",
  "cards": [
    {
      "order": 0,
      "cardUrl": "https://s3.../magazine-card1.jpg"
    },
    {
      "order": 1,
      "cardUrl": "https://s3.../magazine-card2.jpg"
    },
    {
      "order": 2,
      "cardUrl": "https://s3.../magazine-card3.jpg"
    }
  ]
}
```

**카드만 수정하는 예시:**
```json
{
  "name": "경주 여행 완벽 가이드",
  "cards": [
    {
      "order": 0,
      "cardUrl": "https://s3.../new-card1.jpg"
    },
    {
      "order": 1,
      "cardUrl": "https://s3.../new-card2.jpg"
    }
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

**카드 비활성화 예시:**
```json
{
  "name": "경주 여행 완벽 가이드",
  "cards": [
    {
      "order": 1,
      "cardUrl": ""  // 빈 문자열이면 order 1의 카드가 비활성화됨
    }
  ]
}
```

**⚠️ 주의사항:**
- `cards`가 포함된 경우, `order` 값에 따라 기존 카드를 업데이트하거나 새로 생성합니다.
- `cardUrl`이 빈 문자열("")이면 해당 `order`의 카드를 비활성화합니다.
- 요청에 없는 `order`의 카드는 유지됩니다 (비활성화되지 않음).
- `cards`를 포함하지 않으면 카드는 변경되지 않습니다.

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
   - Request Body에 `name`과 `cards` 포함하여 전송
   - 예상 응답: `200 OK` + 수정된 매거진 ID

2. **정보만 수정 (카드 유지):**
   - Request Body에 `name`, `summary`, `content`만 포함 (cards 제외)
   - 예상 응답: `200 OK` + 수정된 매거진 ID (카드는 변경되지 않음)

3. **카드만 수정:**
   - Request Body에 `name`과 `cards` 포함
   - 예상 응답: `200 OK` + 요청된 order의 카드만 업데이트/생성됨

4. **카드 비활성화:**
   - Request Body에 `name`과 `cards` 포함 (일부 카드의 `cardUrl`을 빈 문자열("")로 설정)
   - 예상 응답: `200 OK` + 빈 문자열인 카드는 비활성화됨

5. **매거진 없음:**
   - 존재하지 않는 `magazineId` 전송
   - 예상 응답: `404 Not Found` + 에러 메시지

### 9.6 태그 목록 조회
```
GET http://localhost:8080/api/admin/tags
```

**설명:**
- 전체 태그 목록을 조회합니다.
- 태그 ID와 태그 이름을 반환합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "성공",
  "data": [
    {
      "tagId": 1,
      "name": "경주"
    },
    {
      "tagId": 2,
      "name": "관광지"
    }
  ]
}
```

### 9.7 태그 생성
```
POST http://localhost:8080/api/admin/tags
Content-Type: application/json

{
  "name": "사진명소"
}
```

**⚠️ 중요**: POST 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Request Body:**
- `name` (required): 태그 이름 (중복 불가)

**응답 예시:**
```json
{
  "success": true,
  "message": "태그가 생성되었습니다.",
  "data": 1
}
```

### 9.8 태그 수정
```
PUT http://localhost:8080/api/admin/tags/1
Content-Type: application/json

{
  "name": "수정된 태그 이름"
}
```

**⚠️ 중요**: PUT 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Path Parameters:**
- `tagId` (required): 태그 ID

**Request Body:**
- `name` (required): 새로운 태그 이름 (중복 불가)

**설명:**
- 기존 매거진 태그의 이름을 수정합니다.
- 태그 이름은 중복될 수 없습니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "태그가 수정되었습니다.",
  "data": 1
}
```

### 9.9 매거진에 태그 추가
```
POST http://localhost:8080/api/admin/magazines/1/tags/1
```

**Path Parameters:**
- `magazineId` (required): 매거진 ID
- `tagId` (required): 태그 ID

**설명:**
- 매거진에 태그를 추가합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진에 태그가 추가되었습니다.",
  "data": 1
}
```

### 9.10 매거진에서 태그 삭제
```
DELETE http://localhost:8080/api/admin/magazines/1/tags/1
```

**Path Parameters:**
- `magazineId` (required): 매거진 ID
- `tagId` (required): 태그 ID

**설명:**
- 매거진에서 태그를 삭제합니다. (소프트 삭제)
- 매거진에 해당 태그가 추가되어 있지 않은 경우 404 에러가 발생합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진에서 태그가 삭제되었습니다.",
  "data": 1
}
```

**에러 응답 예시:**
```json
{
  "success": false,
  "message": "매거진에 해당 태그가 추가되어 있지 않습니다.",
  "data": null
}
```

### 9.11 매거진 카드에 장소 매핑
```
PUT http://localhost:8080/api/admin/magazines/1/cards/0/place/1
```

**Path Parameters:**
- `magazineId` (required): 매거진 ID
- `cardOrder` (required): 카드 순서 (0부터 시작)
- `placeId` (required): 장소 ID

**설명:**
- 매거진 카드에 장소를 매핑합니다.
- 카드는 매거진 ID와 order로 식별됩니다.
- 일대일 매핑이므로 기존 매핑이 있으면 새로운 장소로 교체됩니다.
- 비활성화된 장소는 매핑할 수 없습니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진 카드에 장소가 매핑되었습니다.",
  "data": 1
}
```

**에러 응답 예시:**
```json
{
  "success": false,
  "message": "매거진 카드를 찾을 수 없습니다: magazineId=1, order=0",
  "data": null
}
```

### 9.12 매거진 카드에서 장소 매핑 해제
```
DELETE http://localhost:8080/api/admin/magazines/1/cards/0/place
```

**Path Parameters:**
- `magazineId` (required): 매거진 ID
- `cardOrder` (required): 카드 순서 (0부터 시작)

**설명:**
- 매거진 카드에서 장소 매핑을 해제합니다.
- 카드는 매거진 ID와 order로 식별됩니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "매거진 카드에서 장소 매핑이 해제되었습니다.",
  "data": 1
}
```

**에러 응답 예시:**
```json
{
  "success": false,
  "message": "매거진 카드를 찾을 수 없습니다: 999",
  "data": null
}
```

### 9.13 공식 광고 생성
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

### 9.14 개인 광고 생성
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

### 9.15 전체 장소 목록 조회 (간단 정보, 관리자용)
```
GET http://localhost:8080/api/admin/places
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

### 9.16 전체 리뷰 목록 조회 (관리자용)
```
GET http://localhost:8080/api/admin/reviews
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

### 9.17 포인트 적립 조건 등록/수정
```
POST http://localhost:8080/api/admin/point/conditions
Content-Type: application/json

{
  "conditionType": "REVIEW_CREATE",
  "pointAmount": 100
}
```

**⚠️ 중요**: POST 요청이므로 **Request Body**에 JSON 데이터를 넣어야 합니다.

**Request Body:**
- `conditionType` (required): 포인트 적립 조건 타입
  - `SIGN_UP`: 회원가입
  - `REFERRAL`: 친구 초대
  - `PLACE_VISIT`: 방문 포인트
  - `REVIEW_CREATE`: 리뷰 포인트
  - `REVIEW_WITH_PHOTO`: 사진 리뷰 포인트
  - `PROFILE_COMPLETE`: 프로필 완성
- `pointAmount` (required): 적립 포인트 양 (0 이상의 정수)

**설명:**
- 포인트 적립 조건을 등록하거나 수정합니다.
- 조건 타입(conditionType)은 unique하므로, 이미 존재하는 조건 타입이면 포인트 양만 업데이트되고, 존재하지 않는 조건 타입이면 새로 생성됩니다.
- `pointAmount`는 0 이상의 값이어야 합니다.

**응답 예시 (생성):**
```json
{
  "success": true,
  "message": "포인트 적립 조건이 등록/수정되었습니다.",
  "data": 1
}
```

**응답 예시 (수정):**
```json
{
  "success": true,
  "message": "포인트 적립 조건이 등록/수정되었습니다.",
  "data": 1
}
```

**에러 응답:**
- `400 Bad Request`: 
  - 필수 필드 누락 (conditionType 또는 pointAmount가 null)
  - pointAmount가 0 미만인 경우
  - 잘못된 conditionType 값

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
4. `GET /api/admin/places` - 전체 장소 목록 조회 (간단 정보, 관리자용)
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

### 시나리오 3: 방문 기록 및 포인트
1. `POST /api/auth/login` - 로그인
2. `GET /api/me/today/places` - 오늘 일정 장소 조회
3. `POST /api/me/today/places/1` - 장소 방문 확인 및 기록 (GPS 위치 전송)
4. `GET /api/me/points/histories?type=ALL` - 포인트 히스토리 전체 조회
5. `GET /api/me/points/histories?type=EARN` - 포인트 적립 내역만 조회
6. `GET /api/me/points/expiring-points` - 소멸 예정 포인트 조회

### 시나리오 4: 매거진 조회 및 좋아요
1. `GET /api/magazines?size=5` - 매거진 목록 조회 (첫 페이지)
2. `GET /api/magazines?lastMagazineId=123&size=5` - 매거진 목록 조회 (다음 페이지)
3. `GET /api/magazines/1` - 매거진 상세 조회
4. `GET /api/magazines/1/places` - 매거진 카드뉴스에 매핑된 장소 목록 조회 (전체 정보)
5. `GET /api/magazines/1/places?thumbnailOnly=true&count=3` - 매거진 카드뉴스에 매핑된 장소 썸네일 목록 조회
6. `POST /api/magazines/1/like` - 좋아요 토글 (로그인 필요)

### 시나리오 5: 관리자가 데이터 생성 (전체 플로우)
1. `POST /api/admin/places` - 장소 생성
2. `POST /api/admin/places/1/photos` - 장소 사진 추가 (여러 장 한번에)
3. `PUT /api/admin/places/1` - 장소 수정 (사진 포함)
4. `POST /api/admin/magazines` - 매거진 생성
5. `PUT /api/admin/magazines/1` - 매거진 수정 (카드 포함)
6. `GET /api/admin/tags` - 태그 목록 조회
7. `POST /api/admin/tags` - 태그 생성
8. `PUT /api/admin/tags/1` - 태그 수정
9. `POST /api/admin/magazines/1/tags/1` - 매거진에 태그 추가
10. `DELETE /api/admin/magazines/1/tags/1` - 매거진에서 태그 삭제
11. `PUT /api/admin/magazines/1/cards/0/place/1` - 매거진 카드에 장소 매핑
12. `DELETE /api/admin/magazines/1/cards/0/place` - 매거진 카드에서 장소 매핑 해제
13. `POST /api/admin/advertise/official` - 공식 광고 생성
14. `GET /api/places/1` - 생성된 장소 확인
15. `GET /api/magazines/1` - 생성된 매거진 확인
16. `GET /api/advertise/official` - 생성된 광고 확인

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

