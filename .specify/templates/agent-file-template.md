# SP-Complexity-Evaluator Development Guidelines

Auto-generated from all feature plans. Last updated: [DATE]

## Active Technologies

- Java 17
- Spring Boot 3.2.6
- Lombok 1.18.32
- JUnit 5
- Apache POI 5.2.3
- Apache Commons IO 2.11.0

## Project Structure

```text
com.sdchat.ce.sp.complexity/
├── Application.java          # Main Spring Boot application
├── config/                   # Configuration classes
├── controller/               # REST controllers
├── evaluator/                # Complexity evaluators (Oracle, Gauss, Hive)
├── model/                    # Data models (DTOs, entities)
├── parser/                   # SQL parsers
├── service/                  # Business logic services
└── util/                     # Utility classes
```

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Test
./mvnw test

# Test specific class
./mvnw test -Dtest=ClassName

# Test specific method
./mvnw test -Dtest=ClassName#methodName
```

## Code Style

### Java Conventions
- **Naming**: camelCase for methods/variables, PascalCase for classes, UPPER_SNAKE_CASE for constants
- **Indentation**: 4 spaces (tabs not used)
- **Line length**: Keep reasonable, prefer to break long lines
- **Imports**: No wildcard imports except in test files; organize: standard Java, third-party, project-specific

### Lombok Usage
- **Model classes**: Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Logging**: Use `@Slf4j` annotation (not manual logger fields)
- **Components**: Use Spring annotations with Lombok as needed

### Spring Annotations
- `@Component` for service-like classes
- `@Service` for business logic
- `@Controller` for REST endpoints
- Constructor injection preferred

### Logging
- Use `@Slf4j` annotation
- Log levels: DEBUG for detailed info, INFO for general, WARN for issues, ERROR for failures
- Example: `log.debug("Evaluating {} tables", tableCount);`

### Testing
- Use JUnit 5: `@Test`, `@BeforeEach`, `@SpringBootTest`
- Test naming: `methodName_condition_expectedResult`
- Test classes organized by dialect: `{Dialect}ComplexityEvaluatorTest`

### Constants
- Use `private static final` for class constants
- UPPER_SNAKE_CASE naming
- Use `Pattern` compilation for regex patterns

### Error Handling
- Methods that can fail throw `Exception` (consider more specific exceptions when appropriate)
- Log warnings for recoverable issues, errors for failures
- Avoid silent failures - log or throw exceptions

## Recent Changes

[LAST 3 FEATURES AND WHAT THEY ADDED]

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
