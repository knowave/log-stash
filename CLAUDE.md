# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that implements structured API logging with Logstash integration. The application captures HTTP requests/responses, sanitizes sensitive data, and sends logs asynchronously to Logstash, which stores them in MySQL.

## Build and Run Commands

### Local Development
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application locally
./gradlew bootRun
```

### Docker Environment
```bash
# Start all services (app, logstash, mysql)
docker-compose up --build

# Stop all services
docker-compose down

# View logs
docker-compose logs -f app
docker-compose logs -f logstash
docker-compose logs -f mysql

# Rebuild specific service
docker-compose up --build app
```

The application runs on port 8080 with context path `/api`.

## Architecture

### Log Flow
1. **LoggingFilter** (Filter Layer) - Intercepts all HTTP requests
   - Wraps request/response with caching wrappers to read body content
   - Generates/forwards `X-Trace-Id` for request tracing
   - Captures request/response metadata and bodies
   - Sanitizes sensitive fields (password, token, etc.) before logging

2. **LogstashLogger** (Transport Layer) - Async TCP connection to Logstash
   - Maintains persistent TCP socket connection to Logstash (host:5000)
   - Auto-reconnects on connection failure (5-second retry interval)
   - Uses Kotlin coroutines for non-blocking async log transmission
   - Falls back to debug logging if Logstash is unavailable

3. **Logstash Pipeline** - Processes and stores logs
   - Receives JSON logs via TCP on port 5000
   - Parses timestamps and removes metadata fields
   - Inserts structured logs into MySQL `api_logs` table

4. **MySQL Storage** - Persistent log storage
   - Stores all API interaction logs with indexed columns (timestamp, path, status_code, trace_id)
   - Schema defined in `docker/mysql/init.sql`

### Key Components

**Common Layer** (`src/main/kotlin/com/example/logstash/common/`):
- `filter/LoggingFilter.kt`: OncePerRequestFilter that captures all HTTP traffic. Uses `ContentCachingRequestWrapper` with 1MB cache limit to read request bodies without consuming the input stream.
- `logger/LogstashLogger.kt`: Manages TCP connection lifecycle and async log transmission using coroutines
- `dto/ApiLogEntry.kt`: Log entry schema with snake_case JSON serialization

**Configuration**:
- Logstash connection configured via environment variables: `LOGSTASH_HOST`, `LOGSTASH_PORT`, `LOGSTASH_ENABLED`
- Filter excludes actuator endpoints and favicon from logging

### Sensitive Data Handling

The LoggingFilter automatically masks these fields in request/response bodies:
- password
- token
- accessToken
- refreshToken
- secret

Masked fields are replaced with `"***MASKED***"` before transmission.

## Known Issues and Fixes

### ObjectMapper Bean Registration
Spring Boot 4.0+ with Kotlin may not auto-configure ObjectMapper. If you see "ObjectMapper bean not found" errors, create a configuration:

```kotlin
@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()
}
```

### ContentCachingRequestWrapper cacheLimit
Recent Spring versions require explicit `cacheLimit` parameter. The current implementation uses 1MB (1024 * 1024 bytes).

## Testing the Log Pipeline

1. Start services: `docker-compose up`
2. Send test request: `curl -X POST http://localhost:8080/api/test -H "Content-Type: application/json" -d '{"name":"test"}'`
3. Check logs in MySQL:
   ```bash
   docker exec -it <mysql-container> mysql -uroot -prootpassword logs_db
   SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT 5;
   ```