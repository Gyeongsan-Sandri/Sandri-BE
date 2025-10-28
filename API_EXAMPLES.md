# API 사용 예제 (세션 기반)

## 1. 회원가입 1단계
```bash
curl -X POST http://localhost:8080/api/auth/register/step1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "birthDate": "1990-01-01",
    "gender": "MALE",
    "telecomCarrier": "KT",
    "phoneNumber": "010-1234-5678"
  }'
```

## 2. 인증번호 발송
```bash
curl -X POST "http://localhost:8080/api/auth/verification/send?phoneNumber=010-1234-5678"
```

## 3. 휴대폰 인증
```bash
curl -X POST http://localhost:8080/api/auth/verification/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "010-1234-5678",
    "verificationCode": "123456"
  }'
```

## 4. 회원가입 2단계
```bash
curl -X POST "http://localhost:8080/api/auth/register/step2?phoneNumber=010-1234-5678" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "홍길동",
    "username": "hong123",
    "password": "password123!",
    "confirmPassword": "password123!"
  }'
```

## 5. 로그인 (세션 생성)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "hong123",
    "password": "password123!"
  }'
```

## 6. 통신사 목록 조회
```bash
curl -X GET http://localhost:8080/api/common/telecom-carriers
```

## 7. 사용자 프로필 조회 (세션 사용)
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -b cookies.txt
```

## 8. 로그아웃 (세션 무효화)
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

## 세션 기반 인증의 장점
- **간단한 구현**: JWT보다 구현이 간단하고 이해하기 쉬움
- **자동 만료**: 서버에서 세션 만료 시간 관리
- **보안성**: 서버에서 세션을 직접 관리하여 더 안전
- **로그아웃 즉시 반영**: 서버에서 세션을 즉시 무효화 가능