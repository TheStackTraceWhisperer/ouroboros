# Ouroboros Spring Boot Application

Ouroboros is a Spring Boot 3.5.0 web application built with Maven and Java 21. It provides a simple REST API with two endpoints for demonstration purposes.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Prerequisites and Environment Setup
- **CRITICAL**: Use Java 21. Switch to it using: `export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 && export PATH=$JAVA_HOME/bin:$PATH`
- Verify Java version: `java -version` (should show Java 21.0.8)
- Verify Maven: `mvn -version` (should show Apache Maven 3.9.11)

### Build and Test Commands
Always run these commands from the repository root directory:

- **Clean compile**: `mvn clean compile`
  - Takes approximately 17 seconds on first run (downloads dependencies)
  - **NEVER CANCEL**: Set timeout to 60+ minutes. BUILD MAY TAKE LONGER ON SLOW NETWORKS.
  
- **Run tests**: `mvn test`
  - Takes approximately 9 seconds 
  - **NEVER CANCEL**: Set timeout to 30+ minutes for test execution
  - Runs 3 tests: context loads test and 2 controller endpoint tests
  
- **Create JAR package**: `mvn package`
  - Takes approximately 10 seconds
  - **NEVER CANCEL**: Set timeout to 60+ minutes for packaging
  - Creates executable JAR: `target/ouroboros-0.0.1-SNAPSHOT.jar`

### Running the Application
**ALWAYS run the build commands first before starting the application.**

#### Method 1: Maven Spring Boot Plugin
```bash
mvn spring-boot:run
```
- Application starts on port 8080
- Takes about 1-2 seconds to start after dependencies are loaded

#### Method 2: Executable JAR
```bash
java -jar target/ouroboros-0.0.1-SNAPSHOT.jar
```
- Must run `mvn package` first to create the JAR
- Application starts on port 8080
- Takes about 2 seconds to start

### Validation Scenarios
**ALWAYS manually validate any changes by running through these complete end-to-end scenarios after making code changes:**

1. **Basic REST API Validation**:
   - Start the application using either method above
   - Test root endpoint: `curl http://localhost:8080/` 
     - Expected response: "Hello from Ouroboros Spring Boot Application!"
   - Test health endpoint: `curl http://localhost:8080/health`
     - Expected response: "OK"
   - Stop the application (Ctrl+C)

2. **Build Validation**:
   - Run `mvn clean compile test package` (takes ~36 seconds first time, ~5 seconds subsequent)
   - Verify JAR exists: `ls -la target/ouroboros-0.0.1-SNAPSHOT.jar`
   - Test JAR execution as shown above

## Common Tasks and Information

### Repository Structure
```
.
├── README.md                    (minimal project description)
├── pom.xml                     (Maven configuration, Spring Boot 3.5.0)
├── src/
│   ├── main/
│   │   ├── java/com/ouroboros/
│   │   │   ├── OuroborosApplication.java    (main Spring Boot application)
│   │   │   └── controller/
│   │   │       └── HelloController.java     (REST endpoints)
│   │   └── resources/
│   │       └── application.properties       (app config, port 8080)
│   └── test/
│       └── java/com/ouroboros/
│           ├── OuroborosApplicationTests.java     (context load test)
│           └── controller/
│               └── HelloControllerTest.java       (endpoint tests)
└── target/                     (build output directory)
```

### Key Files Content

#### application.properties
```properties
server.port=8080
spring.application.name=ouroboros
logging.level.com.ouroboros=INFO
logging.level.org.springframework=INFO
```

#### Available REST Endpoints
- `GET /` - Returns: "Hello from Ouroboros Spring Boot Application!"
- `GET /health` - Returns: "OK"

### Maven Dependencies
- spring-boot-starter-web (includes Tomcat, Spring MVC)
- spring-boot-starter-test (includes JUnit 5, MockMvc, AssertJ)

## Development Guidelines

### Making Changes
- **Always test both endpoints** after making changes to controllers or configuration
- **Always run the full test suite** (`mvn test`) before considering changes complete
- **No linting tools configured** - standard Java/Spring Boot conventions apply
- **No CI/CD configured** - all validation must be done locally

### Common Troubleshooting
- If build fails with Java version errors, ensure Java 21 is active (see Prerequisites section)
- If application fails to start, check if port 8080 is already in use: `lsof -i :8080`
- If tests fail, check the surefire-reports in `target/surefire-reports/` for details

### Time Expectations
- **Initial Maven dependency download**: 15-20 seconds (first time only)
- **Compile**: ~17 seconds (first time), ~2 seconds (subsequent)
- **Test execution**: ~9 seconds (first time), ~2-3 seconds (subsequent)
- **Package creation**: ~10 seconds (first time), ~1-2 seconds (subsequent)
- **Application startup**: 2 seconds
- **Total build and test cycle**: ~36 seconds (first time), ~5-6 seconds (subsequent with cached dependencies)

**REMEMBER**: These are typical times. On slower networks or systems, builds may take significantly longer. **NEVER CANCEL** long-running builds - wait for completion and increase timeouts accordingly.