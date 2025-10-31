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

## 회원가입 필드 설명
- **name**: 이름 (필수)
- **username**: 아이디 (필수, 4-30자, 영문/숫자)
- **password/confirmPassword**: 비밀번호와 확인 (필수)
- **nickname**: 닉네임 (필수, 2-30자, 중복 불가)
- **birthDate**: 생년월일 (필수, YYYY-MM-DD 형식)
- **gender**: 성별 (필수, MALE/FEMALE/OTHER)
- **location**: 사는 곳 (필수, 100자 이하)
- **referrerUsername**: 추천인 아이디 (선택, 30자 이하)