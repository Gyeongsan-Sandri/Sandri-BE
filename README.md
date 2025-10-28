# Sandri

---

경산 관광 웹/앱 서비스입니다.

## 🚀 API 문서

### Swagger UI
- 개발 서버: http://localhost:8080/swagger-ui.html
- API 문서: http://localhost:8080/v3/api-docs

### 주요 API 엔드포인트

#### 인증 관련 API (`/api/auth`)
- `POST /api/auth/login` - 로그인
- `POST /api/auth/register/step1` - 회원가입 1단계 (개인정보 입력)
- `POST /api/auth/verification/send` - 인증번호 발송
- `POST /api/auth/verification/verify` - 휴대폰 인증
- `POST /api/auth/register/step2` - 회원가입 2단계 (계정 정보 입력)

#### 사용자 관련 API (`/api/user`)
- `GET /api/user/profile` - 사용자 프로필 조회

#### 공통 API (`/api/common`)
- `GET /api/common/telecom-carriers` - 통신사 목록 조회

### 환경 변수 설정
```bash
DB_URL=jdbc:mysql://localhost:3306/sandri
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 세션 기반 인증
- 로그인 시 세션에 사용자 정보 저장
- 브라우저 쿠키를 통한 세션 관리
- 로그아웃 시 세션 무효화

### 🕰️ 개발 기간

- 2025.9.3.수 ~

### 👥팀원 소개

---

| 김가영 | 김서연 | 한수빈 |
| --- | --- | --- |
| <img src="https://avatars.githubusercontent.com/u/165864773?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/162548811?v=4" width="150"> | <img src= "https://avatars.githubusercontent.com/u/135015634?v=4" width="150"> |
| FE | FE, BE | BE |
| https://github.com/kayeong97 | https://github.com/seoyeoki | https://github.com/hansubsub |
### 🌿커밋 메시지

---

| 메시지 | 설명 |
| --- | --- |
| feat | 새로운 기능 추가 또는 요구사항에 따른 기존 기능 수정 |
| fix | 기능에 대한 버그 수정 |
| docs | 문서(주석, README 등) 수정 |
| ref | 기능 변화가 없는 리팩터링 |
| chore | 빌드 시스템, 라이브러리, 환경설정 등 기타 수정 |

### 브랜치 전략

---

`main` : 배포가 가능한 안정적인 코드 관리 브랜치

`develop` : 개발이 이루어지는 주 브랜치

`feature/*` : 새로운 기능 개발을 위한 브랜치(ex:feature/login)

`fix/*` :버그와 같은 오류 수정을 위한 브랜치
