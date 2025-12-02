# Logstash API Logger

Spring Boot 기반의 API 로깅 시스템으로, HTTP 요청/응답을 자동으로 캡처하여 Logstash를 통해 MySQL에 저장합니다.

## 주요 기능

- **자동 HTTP 로깅**: 모든 API 요청/응답을 자동으로 캡처
- **민감 정보 마스킹**: 비밀번호, 토큰 등 민감한 필드를 자동으로 마스킹 처리
- **비동기 로그 전송**: Kotlin Coroutines를 사용한 논블로킹 로그 전송
- **자동 재연결**: Logstash 연결 실패 시 자동으로 재시도
- **요청 추적**: X-Trace-Id 헤더를 통한 요청 추적
- **구조화된 로그 저장**: MySQL에 인덱싱된 로그 데이터 저장

## 기술 스택

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 4.0.0
- **Build Tool**: Gradle
- **Java Version**: 21
- **Database**: MySQL 8.0
- **Log Processing**: Logstash 8.11.0
- **비동기 처리**: Kotlin Coroutines

## 시스템 아키텍처

```
HTTP Request
    ↓
LoggingFilter (요청/응답 캡처 + 민감정보 마스킹)
    ↓
LogstashLogger (비동기 TCP 전송)
    ↓
Logstash Pipeline (로그 파싱 및 변환)
    ↓
MySQL (영구 저장)
```

## 빠른 시작

### 사전 요구사항

- Docker & Docker Compose
- 또는 JDK 21 + Gradle (로컬 실행 시)

### Docker Compose로 실행

```bash
# 모든 서비스 시작 (App + Logstash + MySQL)
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build

# 서비스 중지
docker-compose down

# 로그 확인
docker-compose logs -f app
docker-compose logs -f logstash
```

서비스 접속:
- Application: http://localhost:8080/api
- MySQL: localhost:3309 (root/rootpassword)

### 로컬 실행

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

## API 엔드포인트

### Health Check
```bash
GET /api/health
```

응답:
```json
{
  "status": "ok"
}
```

### 테스트 엔드포인트
```bash
POST /api/test
Content-Type: application/json

{
  "name": "테스트",
  "password": "secret123"
}
```

응답:
```json
{
  "message": "Received: 테스트",
  "timestamp": 1234567890
}
```

## 로그 데이터 구조

### API 로그 엔트리

```json
{
  "timestamp": "2025-12-02T10:30:00.123Z",
  "level": "INFO",
  "method": "POST",
  "path": "/api/test?param=value",
  "status_code": 200,
  "response_time_ms": 45,
  "user_id": "user123",
  "ip_address": "192.168.1.1",
  "user_agent": "Mozilla/5.0...",
  "request_body": "{\"name\":\"test\",\"password\":\"***MASKED***\"}",
  "response_body": "{\"message\":\"success\"}",
  "error_message": null,
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 자동 마스킹되는 필드

다음 필드명이 포함된 JSON 속성은 자동으로 `***MASKED***`로 치환됩니다:
- `password`
- `token`
- `accessToken`
- `refreshToken`
- `secret`

## 데이터베이스 스키마

```sql
CREATE TABLE api_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME(3) NOT NULL,
    level VARCHAR(20) NOT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status_code INT,
    response_time_ms INT,
    user_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_body TEXT,
    response_body TEXT,
    error_message TEXT,
    trace_id VARCHAR(100),
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_timestamp (timestamp),
    INDEX idx_path (path),
    INDEX idx_status_code (status_code),
    INDEX idx_trace_id (trace_id)
);
```

## 설정

### 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `LOGSTASH_HOST` | localhost | Logstash 호스트 |
| `LOGSTASH_PORT` | 5000 | Logstash 포트 |
| `LOGSTASH_ENABLED` | true | Logstash 로깅 활성화 여부 |

### application.yaml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

logstash:
  host: ${LOGSTASH_HOST:localhost}
  port: ${LOGSTASH_PORT:5000}
  enabled: ${LOGSTASH_ENABLED:true}
```

## 로그 확인

### MySQL에서 로그 조회

```bash
# MySQL 컨테이너 접속
docker exec -it <mysql-container-id> mysql -uroot -prootpassword logs_db

# 최근 로그 조회
SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT 10;

# 특정 경로 로그 조회
SELECT * FROM api_logs WHERE path LIKE '/api/test%' ORDER BY timestamp DESC;

# 에러 로그만 조회
SELECT * FROM api_logs WHERE error_message IS NOT NULL ORDER BY timestamp DESC;

# Trace ID로 요청 추적
SELECT * FROM api_logs WHERE trace_id = 'your-trace-id';
```

### Logstash 로그 확인

```bash
docker-compose logs -f logstash
```

## 프로젝트 구조

```
src/main/kotlin/com/example/logstash/
├── LogstashApplication.kt          # 메인 애플리케이션
├── common/
│   ├── dto/
│   │   └── ApiLogEntry.kt          # 로그 엔트리 데이터 모델
│   ├── filter/
│   │   └── LoggingFilter.kt        # HTTP 요청/응답 캡처 필터
│   └── logger/
│       └── LogstashLogger.kt       # Logstash TCP 연결 및 전송
├── controller/
│   └── HealthController.kt         # 테스트 컨트롤러
└── dto/
    ├── TestRequest.kt
    └── TestResponse.kt

docker/
├── logstash/
│   ├── Dockerfile
│   └── pipeline/
│       └── logstash.conf           # Logstash 파이프라인 설정
└── mysql/
    └── init.sql                    # MySQL 초기화 스크립트
```

## 주요 컴포넌트

### LoggingFilter
- `OncePerRequestFilter`를 상속하여 모든 HTTP 요청을 인터셉트
- `ContentCachingRequestWrapper`로 요청 본문을 1MB까지 캐싱
- 민감한 정보를 자동으로 마스킹
- `/actuator`, `/favicon.ico` 경로는 로깅에서 제외

### LogstashLogger
- Logstash와 TCP 소켓 연결 유지
- 연결 실패 시 5초 간격으로 자동 재연결
- Kotlin Coroutines로 비동기 로그 전송
- Logstash 비활성화 시 로컬 로그로 폴백

### Logstash Pipeline
- TCP 5000 포트로 JSON 로그 수신
- 타임스탬프 파싱 및 불필요한 메타데이터 제거
- MySQL JDBC를 통해 로그 저장

## 트러블슈팅

### ObjectMapper 빈을 찾을 수 없는 경우

Spring Boot 4.0+에서 ObjectMapper가 자동 설정되지 않을 수 있습니다. Configuration 클래스를 추가하세요:

```kotlin
@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()
}
```

### Logstash 연결 실패

```bash
# Logstash 상태 확인
docker-compose logs logstash

# Logstash 헬스체크
curl http://localhost:9600/_node/stats

# 네트워크 연결 확인
docker-compose exec app nc -zv logstash 5000
```

### MySQL 연결 문제

```bash
# MySQL 컨테이너 상태 확인
docker-compose ps mysql

# MySQL 로그 확인
docker-compose logs mysql

# MySQL 연결 테스트
docker-compose exec mysql mysqladmin ping -h localhost
```