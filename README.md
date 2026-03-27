## URL Shortener Service

This project is a small but production‑oriented URL shortener implemented with Java and Spring Boot.  
It exposes a simple HTTP API to create short links and redirect users to the original target URLs.

### Architecture Overview

- **Layered structure**
  - **Controller**: `UrlController` exposes REST endpoints for shortening and redirecting URLs; `StatsController` exposes click statistics for a short ID.
  - **Service**: `UrlService` / `UrlServiceImpl` contains business logic for ID generation and persistence.
  - **Rate limiting**: `RateLimiterService` / `RedisTokenBucketRateLimiter` protects `POST /shorten-url` from abuse.
  - **Redirect cache**: `RedirectUrlCacheService` / `RedisRedirectUrlCacheService` caches redirect targets in Redis (cache-aside) to offload read-heavy `GET /{id}` traffic from MongoDB.
  - **Click analytics**: `ClickCounterService` / `RedisClickCounterService` stores per-link click counts in Redis using atomic `INCR`, with TTL aligned to link expiry.
  - **Persistence**: `UrlRepository` works with MongoDB and the `UrlEntity` document.
  - **DTOs**: `UrlRequestDTO`, `UrlResponseDTO`, `UrlClickStatsDTO` are used as input/output contracts for the API.
- **Persistence and expiration**
  - Each short URL is stored as a `UrlEntity` document in MongoDB.
  - A TTL index on the `expiredAt` field automatically deletes expired records.
  - The expiration time is controlled via configuration property `url-shortener.expiration-minutes`.
  - Example document in the `urls` collection:

    ```json
    {
      "_id": "AbC123",
      "url": "https://example.com",
      "expiredAt": "2024-01-01T12:34:56"
    }
    ```
- **Error handling**
  - `UrlNotFoundException` is raised when an unknown short ID is requested.
  - `RateLimitExceededException` is raised when a client exceeds the create-link limit.
  - `UrlShortenerExceptionHandler` converts domain exceptions into a JSON error model (`UrlShortenerError`).
- **Rate limiting strategy**
  - Implemented with Redis token bucket and an atomic Lua script.
  - Client identity is resolved from `X-Forwarded-For` header, otherwise from request remote address.
  - Redis key format: `rate-limit:shorten-url:<clientId>`.
- **Redirect cache strategy**
  - On successful `POST /shorten-url`, the target URL is written to Redis with a TTL aligned to the link expiry.
  - On `GET /{id}`, the service reads from Redis first; on a miss it loads from MongoDB and repopulates Redis.
  - Redis key format: `cache:url:redirect:<id>`.
- **Click analytics strategy**
  - On successful `POST /shorten-url`, a click counter is initialized in Redis with the same TTL as the short link.
  - Each successful `GET /{id}` redirect increments the counter atomically.
  - Redis key format: `stats:url:clicks:<id>`.

### Tech Stack

- Java 17
- Spring Boot 3 (Web, Data MongoDB)
- Spring Data Redis
- MongoDB with TTL index
- Redis
- Maven
- Docker / Docker Compose
- Testcontainers (MongoDB, Redis) for integration testing

### Configuration

Main configuration is located in `src/main/resources/application.yml`:

- **MongoDB**
  - `spring.data.mongodb.host`
  - `spring.data.mongodb.port`
  - `spring.data.mongodb.database`
- **Redis**
  - `spring.data.redis.host`
  - `spring.data.redis.port`
  - `spring.data.redis.connect-timeout`
  - `spring.redis.timeout`
- **URL shortener**
  - `url-shortener.expiration-minutes` – lifetime of a short URL in minutes (default: `1`).
  - `url-shortener.rate-limit.capacity` – max tokens in bucket (default: `10`).
  - `url-shortener.rate-limit.refill-tokens` – tokens added each refill interval (default: `10`).
  - `url-shortener.rate-limit.refill-duration-seconds` – refill interval in seconds (default: `60`).

You can override these properties via environment variables or command‑line arguments if needed.

### How to Run Locally

#### Prerequisites

- Java 17+
- Maven 3.9+
- Local MongoDB instance (if you are not using Docker Compose)

#### Build and run with Maven

```sh
mvn clean install
mvn spring-boot:run
```

By default, the application starts on `http://localhost:8080`.

#### Run with Docker

Build an image and run a container:

```sh
docker build -t url-shortener .
docker run --rm -p 8080:8080 url-shortener
```

#### Run with Docker Compose

This project includes a minimal `docker-compose.yml` for MongoDB.  
You can start MongoDB via:

```sh
cd docker
docker-compose up -d
```

Then run the Spring Boot application as described above.

### API Usage

#### Create short URL

- **Method**: `POST`
- **Path**: `/shorten-url`
- **Request body**:

```json
{
  "url": "https://example.com"
}
```

- **Response body**:

```json
{
  "url": "https://example.com",
  "shortenUrl": "http://localhost:8080/AbC123"
}
```

`shortenUrl` is a full redirect URL that you can share with clients.

Possible error when rate limit is exceeded:

- **Status**: `429 Too Many Requests`
- **Body**:

```json
{
  "timestamp": "01-01-2024 12:00:00",
  "code": 429,
  "status": "TOO_MANY_REQUESTS",
  "errors": [
    "Rate limit exceeded for client client-a"
  ]
}
```

#### Redirect by short ID

- **Method**: `GET`
- **Path**: `/{id}`

Behavior:

- If the ID exists and is not yet removed by MongoDB TTL, the service responds with **302 Found** and `Location` header containing the original URL.
- The target URL is resolved from Redis when cached; otherwise MongoDB is used and the entry is cached.
- If the ID does not exist (or is already expired and removed), the service returns **404 Not Found** with a JSON error payload:

```json
{
  "timestamp": "01-01-2024 12:00:00",
  "code": 404,
  "status": "NOT_FOUND",
  "errors": [
    "URL AbC123 not found"
  ]
}
```

#### Click statistics for a short ID

- **Method**: `GET`
- **Path**: `/stats/{id}`
- **Response body** (when the short ID exists):

```json
{
  "id": "AbC123",
  "clicks": 42
}
```

- If the ID does not exist, the service returns **404 Not Found** with the same JSON error model as other endpoints.

### Testing

The project focuses on **integration tests**:

- `UrlControllerIT` starts the full Spring context and real MongoDB/Redis instances using **Testcontainers**.
- The test covers the full flow:
  - Creating a short URL through the HTTP API.
  - Validating the redirect response and `Location` header.
  - Verifying that the redirect target is stored in Redis after creation.
  - Verifying click counters and `GET /stats/{id}` after multiple redirects.
  - Returning `429 Too Many Requests` when create-link rate limit is exceeded.

Run tests with:

```sh
mvn test
```

### What was implemented

- Clean layered architecture (controller → service → repository → MongoDB).
- Configurable expiration for short URLs via application properties.
- Redis-based token bucket rate limiting for create-link endpoint abuse protection.
- Redis cache-aside for redirect lookups to reduce MongoDB read load on popular links.
- Redis-backed click analytics with an HTTP API for per-link statistics.
- Robust URL generation that respects the incoming request context.
- Centralized error handling and a consistent error response model.
- Integration tests based on Testcontainers to verify real application behavior end‑to‑end.
