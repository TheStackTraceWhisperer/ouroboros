# ouroboros

A Spring Boot 3.5.x WebMVC Maven Java 21 application.

## Features

- Java 21
- Spring Boot 3.5.x
- Maven build system
- REST API endpoints
- Docker support
- GitHub Actions CI/CD

## API Endpoints

- `GET /` - Hello message
- `GET /health` - Health check

## Docker

This application includes a multi-stage Dockerfile for building and running with Java 21:

```bash
# Build the Docker image
docker build -t ouroboros .

# Run the container
docker run -p 8080:8080 ouroboros
```

## CI/CD

The repository includes GitHub Actions workflow that:
- Builds and tests the application
- Creates Docker images
- Publishes to GitHub Container Registry (GHCR)

Images are published to: `ghcr.io/thestacktracewhisperer/ouroboros`

## Development

```bash
# Run tests
mvn test

# Build application
mvn clean package

# Run application
mvn spring-boot:run
```