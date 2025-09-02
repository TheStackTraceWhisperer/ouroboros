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

This application includes Alpine Linux-based Docker images for optimal size and security:

### Production Docker Image

Multi-stage Dockerfile using Alpine Java images for building and running with Java 21:

```bash
# Build the Docker image
docker build -t ouroboros .

# Run the container
docker run -p 8080:8080 ouroboros
```

### Development Docker Image

For development purposes, use the development Dockerfile with Alpine + Java 21 + Maven:

```bash
# Build the development image
docker build -f Dockerfile.dev -t ouroboros-dev .

# Run development container with volume mounting
docker run -it --rm -p 8080:8080 -v $(pwd):/home/developer/workspace ouroboros-dev

# Inside the container, you can run:
mvn clean compile
mvn test
mvn spring-boot:run
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