# Agent Development Guidelines

This file provides essential information for AI agents working on this codebase.

## Build and Test Commands

### Maven Commands
```bash
# Clean and build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Run a specific test method
./mvnw test -Dtest=ClassName#methodName

# Run tests with specific pattern
./mvnw test -Dtest="*Gauss*,*Oracle*"
```

### Application
- Main class: `com.sdchat.ce.sp.complexity.Application`
- Default port: 8080
- Java version: 17

## Tech Stack
- **Java 17** + Spring Boot 3.2.6
- **Lombok 1.18.32** for reducing boilerplate
- **JUnit 5** for testing
- **Apache POI 5.2.3** for Excel export
- **Apache Commons IO 2.11.0** for file utilities

## Code Style Guidelines

### General Conventions
- **Naming**: camelCase for methods/variables, PascalCase for classes, UPPER_SNAKE_CASE for constants
- **Indentation**: 4 spaces (tabs not used)
- **Line length**: Keep reasonable, prefer to break long lines

### Imports
- No wildcard imports (`import java.util.*`) except in test files
- Organize imports: standard Java, third-party, project-specific
- Remove unused imports

### Lombok Usage
- **Model classes**: Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Logging**: Use `@Slf4j` annotation (not manual logger fields)
- **Components**: Use Spring annotations with Lombok as needed

Example:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityMetrics {
    private double overallScore;
    private int tableCount;
    // ...
}
```

### Spring Annotations
- `@Component` for service-like classes
- `@Service` for business logic
- `@Controller` for REST endpoints
- `@Autowired` for dependency injection (constructor injection preferred)

### Logging
- Use `@Slf4j` annotation
- Log levels: DEBUG for detailed info, INFO for general, WARN for issues, ERROR for failures
- Example: `log.debug("Evaluating {} tables", tableCount);`

### Constants
- Use `private static final` for class constants
- UPPER_SNAKE_CASE naming
- Use `Pattern` compilation for regex patterns

```java
private static final int TABLE_WEIGHT = 10;
private static final Pattern CURSOR_PATTERN = Pattern.compile("\\bCURSOR\\b", Pattern.CASE_INSENSITIVE);
```

### Error Handling
- Methods that can fail throw `Exception` (consider more specific exceptions when appropriate)
- Log warnings for recoverable issues, errors for failures
- Avoid silent failures - log or throw exceptions

### Testing
- Use JUnit 5: `@Test`, `@BeforeEach`, `@SpringBootTest`
- Test naming: `methodName_condition_expectedResult`
- Setup test data in `@BeforeEach`
- Use `assertNotNull`, `assertTrue`, `assertEquals` from `org.junit.jupiter.api.Assertions`

Example:
```java
@SpringBootTest
class GaussComplexityEvaluatorTest {
    @Autowired
    private GaussComplexityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        // Setup code
    }

    @Test
    void evaluateStoredProcedure_WithTables() throws Exception {
        // Test implementation
    }
}
```

### Package Structure
```
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

### SQL Dialect Support
- **Oracle**: Original dialect
- **Gauss**: Added support for Gauss DB (similar to Oracle)
- **Hive**: Extended for Hive SQL with specific features (LATERAL VIEW, DISTRIBUTE BY, etc.)

When adding new dialect support:
1. Create `{Dialect}SqlParser` implementing `SqlParser`
2. Create `{Dialect}ComplexityEvaluator` implementing `ComplexityEvaluator`
3. Register in `ComplexityEvaluationServiceImpl`

### Comments
- Use Javadoc for public classes and methods
- Describe what the method does, parameters, and return values
- Keep comments concise and meaningful

```java
/**
 * Evaluates the complexity of a Gauss stored procedure.
 * @param procedure The stored procedure to evaluate
 * @return Comprehensive complexity metrics
 */
public ComplexityMetrics evaluateStoredProcedure(StoredProcedure procedure) throws Exception {
    // Implementation
}
```

## Important Notes
- The project uses Maven Wrapper (`./mvnw`) - always use this instead of system `mvn`
- Application properties are in `src/main/resources/application.properties`
- Default logging level for project packages is DEBUG
- File uploads limited to 10MB (configurable in application.properties)
- Supports JSON and Excel response formats for batch operations

## Active Technologies
- Java 17 + Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3 (001-procedure-call-details)
- N/A (stateless REST API, no persistence) (001-procedure-call-details)
- Java 17, Spring Boot 3.2.6 + Lombok 1.18.32, Apache POI 5.2.3, JUnit 5 (001-filter-builtin-functions)
- N/A (stateless REST API, JSON file from classpath) (001-filter-builtin-functions)
- Java 17 + Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3 (for Excel export), Jackson (for JSON) (005-gaussdb-hint-validate)
- N/A (stateless REST API, reference JSON from classpath) (005-gaussdb-hint-validate)

### Built-in Function Filtering Feature (001-filter-builtin-functions)
- **Models**: `BuiltInFunction`, `FunctionFilterResult` in `model/` package
- **Config**: `BuiltInFunctionConfig` in `config/` package
- **Utility**: `BuiltInFunctionFilter` in `util/` package (loads gaussdb_functions.json)
- **Integration**: Modified `GaussComplexityEvaluator` to filter built-in functions
- **JSON File**: `src/main/resources/gaussdb_functions.json` (1316 functions, 44 categories)
- **Features**: 
  - Case-insensitive exact matching for function names
  - Filters built-in functions from procedure and table analysis
  - Includes category breakdown in `FunctionFilterResult`
  - Graceful fallback if JSON file missing (warning logged)
- **Response Field**: `ComplexityMetrics.filteredFunctions` contains `FunctionFilterResult`

## Recent Changes
- 001-procedure-call-details: Added Java 17 + Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3
- 001-filter-builtin-functions: Added built-in function filtering from gaussdb_functions.json
