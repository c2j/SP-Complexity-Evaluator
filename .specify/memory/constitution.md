<!--
SYNC IMPACT REPORT
==================
Version Change: [initial] → 1.0.0
Modified Principles: [none - initial constitution]
Added Sections:
  - Core Principles (5 principles)
  - Code Quality Standards
  - Development Workflow
Removed Sections: [none]
Templates Updated:
  ✅ .specify/templates/plan-template.md - Constitution Check section confirmed
  ✅ .specify/templates/spec-template.md - User stories aligned with principles
  ✅ .specify/templates/tasks-template.md - Task categories reflect principles
  ✅ .specify/templates/agent-file-template.md - Code style sections aligned
  ✅ .specify/templates/checklist-template.md - Categories can map to principles
Follow-up TODOs: None
-->

# SP-Complexity-Evaluator Constitution

## Core Principles

### I. SQL Dialect Extensibility

The project MUST support multiple SQL dialects through interface-based design. New dialects
MUST be implemented by creating separate SqlParser and ComplexityEvaluator classes that
implement the respective interfaces. Dialect-specific implementations MUST NOT introduce
breaking changes to existing dialect support. All new dialects MUST be registered in
ComplexityEvaluationServiceImpl. Rationale: Oracle, Gauss, and Hive already coexist;
future dialects (PostgreSQL, MySQL, etc.) require a plug-and-play architecture.

### II. Test Coverage and Validation

All code MUST have corresponding JUnit 5 tests using @SpringBootTest. Test classes
MUST be organized by dialect (e.g., OracleComplexityEvaluatorTest,
GaussComplexityEvaluatorTest, HiveComplexityEvaluatorTest). Test method naming
MUST follow the pattern: methodName_condition_expectedResult. Integration tests MUST
validate complete user journeys (SQL upload, evaluation, response parsing). Rationale:
Complex SQL parsing and scoring algorithms require comprehensive test coverage to prevent
regression across dialects.

### III. Error Resilience

The evaluation process MUST continue even when individual SQL statements or parsing
operations fail. Failed statements MUST be collected via SqlParserExceptionCollector and
included in the final ComplexityMetrics result. Methods that can fail MUST throw
Exception (or more specific exceptions) and log warnings for recoverable issues.
Silent failures are prohibited. Rationale: Batch processing of ZIP files with
hundreds of SQL files cannot fail entirely due to a single malformed statement.

### IV. REST API First

All core functionality MUST be exposed via RESTful endpoints with consistent patterns.
Endpoints MUST support both JSON and Excel response formats for batch operations.
File uploads MUST validate size (default 10MB limit) and type. Response formats
MUST be consistent across all dialects. Rationale: The primary usage model is
programmatic access via API for integrating complexity evaluation into CI/CD pipelines
and code review tools.

### V. Logging and Observability

All service and evaluator classes MUST use @Slf4j annotation. Logging levels:
DEBUG for detailed evaluation steps, INFO for general operations, WARN for recoverable
issues, ERROR for failures. The project packages (com.sdchat.ce.sp.complexity)
MUST log at DEBUG level by default. Rationale: Debugging SQL parsing and complexity
scoring issues requires visibility into intermediate evaluation steps.

## Code Quality Standards

### Lombok and Spring Usage

Model classes MUST use Lombok annotations: @Data, @Builder, @NoArgsConstructor,
@AllArgsConstructor. Components MUST use appropriate Spring annotations (@Component for
service-like classes, @Service for business logic, @Controller for REST endpoints).
Logging MUST use @Slf4j (manual logger fields are prohibited). Constructor injection
is preferred over field injection.

### Code Style

Naming conventions: camelCase for methods/variables, PascalCase for classes,
UPPER_SNAKE_CASE for constants. Indentation: 4 spaces (tabs are prohibited).
Wildcard imports (e.g., import java.util.*) are prohibited except in test files.
Imports MUST be organized: standard Java, third-party, project-specific. Unused
imports MUST be removed. Line length should be reasonable; prefer breaking long lines.

### Constants and Patterns

Constants MUST be declared as private static final with UPPER_SNAKE_CASE naming.
Regex patterns MUST be compiled as Pattern objects with Pattern.CASE_INSENSITIVE
where appropriate for SQL parsing. Example: private static final Pattern CURSOR_PATTERN
= Pattern.compile("\\bCURSOR\\b", Pattern.CASE_INSENSITIVE);

### Error Handling

Methods that can fail MUST throw Exception (consider more specific exceptions when
appropriate). Log warnings for recoverable issues, errors for failures. Avoid silent
failures—always log or throw exceptions.

### Javadoc

Public classes and methods MUST have Javadoc comments describing purpose, parameters,
and return values. Keep comments concise and meaningful.

## Development Workflow

### Build and Test Commands

Maven Wrapper (./mvnw) MUST be used instead of system mvn.
Build: ./mvnw clean package
Run: ./mvnw spring-boot:run
Test all: ./mvnw test
Test specific class: ./mvnw test -Dtest=ClassName
Test specific method: ./mvnw test -Dtest=ClassName#methodName

### Adding New SQL Dialect Support

1. Create {Dialect}SqlParser implementing SqlParser interface
2. Create {Dialect}ComplexityEvaluator implementing ComplexityEvaluator interface
3. Create corresponding test class: {Dialect}ComplexityEvaluatorTest
4. Register in ComplexityEvaluationServiceImpl
5. Add dialect to README.md documentation and API endpoint descriptions

### Package Structure

The project follows a standard Spring Boot structure:
com.sdchat.ce.sp.complexity/
├── Application.java          # Main Spring Boot application
├── config/                   # Configuration classes
├── controller/               # REST controllers
├── evaluator/                # Complexity evaluators (Oracle, Gauss, Hive)
├── model/                    # Data models (DTOs, entities)
├── parser/                   # SQL parsers
├── service/                  # Business logic services
└── util/                     # Utility classes

## Governance

This constitution supersedes all other development practices. Amendments require
documentation of changes, version increment according to semantic versioning rules,
and review of affected templates. All pull requests MUST verify compliance with
core principles. Complexity in implementation MUST be justified against the principles.

### Amendment Procedure

1. Propose change with rationale
2. Update constitution version (MAJOR for backward-incompatible changes to governance,
   MINOR for new principles or sections, PATCH for clarifications)
3. Update dependent templates to reflect changes
4. Generate Sync Impact Report (HTML comment at top)
5. Document amendment date in footer

### Versioning Policy

MAJOR: Backward incompatible governance changes, principle removals or redefinitions
MINOR: New principle or section added, materially expanded guidance
PATCH: Clarifications, wording corrections, typo fixes, non-semantic refinements

### Compliance Review

All code changes must pass:
- Constitution gates in plan-template.md Constitution Check section
- Test coverage requirements from Principle II
- Error handling requirements from Principle III
- API consistency requirements from Principle IV

Use AGENTS.md for runtime development guidance (build commands, code style, testing
practices, and dialect-specific conventions).

**Version**: 1.0.0 | **Ratified**: 2026-01-10 | **Last Amended**: 2026-01-10
