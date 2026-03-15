## URL Shortener Service

This project is a small but production‑oriented URL shortener implemented with Java and Spring Boot.  
It exposes a simple HTTP API to create short links and redirect users to the original target URLs.

### Architecture Overview

- **Layered structure**
  - **Controller**: `UrlController` exposes REST endpoints for shortening and redirecting URLs.
  - **Service**: `UrlService` / `UrlServiceImpl` contains business logic for ID generation and persistence.
  - **Persistence**: `UrlRepository` works with MongoDB and the `UrlEntity` document.
  - **DTOs**: `UrlRequestDTO`, `UrlResponseDTO` are used as input/output contracts for the API.
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
  - `UrlShortenerExceptionHandler` converts domain exceptions into a JSON error model (`UrlShortenerError`).

### Tech Stack

- Java 17
- Spring Boot 3 (Web, Data MongoDB)
- MongoDB with TTL index
- Maven
- Docker / Docker Compose
- Testcontainers (MongoDB) for integration testing

### Configuration

Main configuration is located in `src/main/resources/application.yml`:

- **MongoDB**
  - `spring.data.mongodb.host`
  - `spring.data.mongodb.port`
  - `spring.data.mongodb.database`
- **URL shortener**
  - `url-shortener.expiration-minutes` – lifetime of a short URL in minutes (default: `1`).

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

#### Redirect by short ID

- **Method**: `GET`
- **Path**: `/{id}`

Behavior:

- If the ID exists and is not yet removed by MongoDB TTL, the service responds with **302 Found** and `Location` header containing the original URL.
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

### Testing

The project focuses on **integration tests**:

- `UrlControllerIT` starts the full Spring context and a real MongoDB instance using **Testcontainers**.
- The test covers the full flow:
  - Creating a short URL through the HTTP API.
  - Validating the redirect response and `Location` header.

Run tests with:

```sh
mvn test
```

### What was implemented

- Clean layered architecture (controller → service → repository → MongoDB).
- Configurable expiration for short URLs via application properties.
- Robust URL generation that respects the incoming request context.
- Centralized error handling and a consistent error response model.
- Integration tests based on Testcontainers to verify real application behavior end‑to‑end.
