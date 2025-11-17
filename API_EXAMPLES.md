# API 사용 예제

## 1. 회원가입 (단일 단계)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "username": "hong123",
    "password": "password123!",
    "confirmPassword": "password123!",
    "nickname": "홍길동",
    "birthDate": "1990-01-01",
    "gender": "MALE",
    "location": "경산시",
    "referrerUsername": "friend123"
  }'
```

## 2. 로그인
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "hong123",
    "password": "password123!"
  }'
```

## 3. 사용자 프로필 조회
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -b cookies.txt
```

## 4. 장소 검색 (Google Places API 기반)
```bash
curl -X GET "http://localhost:8080/api/places/search?keyword=경주%20카페&page=1&size=10" \
  -b cookies.txt
```

**참고사항:**
- 장소 검색은 내부 DB를 우선으로 사용합니다
- DB 결과가 부족하면(요청 size 미만) Google Places API로 자동 보충합니다
- DB 장소는 `placeId`가 있어 상세보기/좋아요 기능을 사용할 수 있습니다
- Google 보충 결과는 `placeId`가 `null`이며, 이름/주소/사진만 제공됩니다
- 평점과 좋아요 수는 모두 앱 자체 데이터를 사용합니다
- 카테고리 필터(`category` 파라미터)는 DB와 Google 결과 모두에 적용됩니다

## 회원가입 필드 설명
- **name**: 이름 (필수)
- **username**: 아이디 (필수, 4-30자, 영문/숫자)
- **password/confirmPassword**: 비밀번호와 확인 (필수)
- **nickname**: 닉네임 (필수, 2-30자, 중복 불가)
- **birthDate**: 생년월일 (필수, YYYY-MM-DD 형식)
- **gender**: 성별 (필수, MALE/FEMALE/OTHER)
- **location**: 사는 곳 (필수, 100자 이하)
- **referrerUsername**: 추천인 아이디 (선택, 30자 이하)

## Google Places API 설정
환경 변수 `GOOGLE_MAPS_API_KEY`에 유효한 API 키를 설정해야 합니다:
```bash
export GOOGLE_MAPS_API_KEY=your-actual-google-api-key
```

API 키가 없거나 할당량 초과 시 자동으로 내부 DB 검색으로 전환됩니다.
