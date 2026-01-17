# Implementation Plan: GaussDB SQL Hint Validation

**Branch**: `005-gaussdb-hint-validate` | **Date**: 2026-01-16 | **Spec**: [link](../spec.md)
**Input**: Feature specification from `/specs/005-gaussdb-hint-validate/spec.md`

## Summary

This feature adds SQL hint validation for GaussDB stored procedures and SQL statements. The system extracts hint comments (`/*+ ... */`) from SQL code, validates them against the gaussdb_sql_plan_hints.json reference file, and reports results in both JSON and Excel formats. Valid hints are categorized by type (Scan, Join Method, Join Order, Stream, Aggregation, Query Rewrite, etc.); invalid hints are flagged as NOT_IN_REFERENCE or SYNTAX_ERROR.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3 (for Excel export), Jackson (for JSON)  
**Storage**: N/A (stateless REST API, reference JSON from classpath)  
**Testing**: JUnit 5 with @SpringBootTest  
**Target Platform**: Linux server (Spring Boot application)  
**Project Type**: Single Java project (Spring Boot REST API)  
**Performance Goals**: Analysis of stored procedures up to 1000 lines completes in under 10 seconds  
**Constraints**: File uploads limited to 10MB, stateless API, no persistence  
**Scale/Scope**: Supports single SQL statements and batch stored procedure analysis  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] SQL Dialect Extensibility: New feature integrates with existing Gauss dialect support via GaussSqlParser
- [x] Test Coverage: JUnit 5 tests with @SpringBootTest following naming pattern
- [x] Error Resilience: Failed statements collected without breaking evaluation
- [x] REST API First: Endpoints follow consistent patterns with JSON/Excel support
- [x] Logging: @Slf4j used, DEBUG level for project packages

## Project Structure

### Documentation (this feature)

```text
specs/005-gaussdb-hint-validate/
├── plan.md              # This file
├── research.md          # Not needed - spec has no clarifications
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── openapi.yaml     # OpenAPI 3.0 specification
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/sdchat/ce/sp/complexity/
├── model/
│   ├── HintValidationResult.java    # NEW: Single hint validation result
│   ├── HintAnalysisSummary.java     # NEW: Aggregated analysis results
│   ├── StoredProcedureAnalysis.java # NEW: Complete procedure analysis
│   └── HintValidationResponse.java  # NEW: API response wrapper
├── service/
│   ├── HintValidationService.java   # NEW: Core hint validation logic
│   └── ExcelExportService.java      # NEW: Excel export using Apache POI
├── controller/
│   └── HintValidationController.java # NEW: REST endpoints for hint validation
├── evaluator/
│   └── HintExtractor.java           # NEW: Extract hints from SQL text
├── parser/
│   └── HintReferenceLoader.java     # NEW: Load and cache hint reference JSON
└── util/
    └── HintSyntaxValidator.java     # NEW: Validate hint syntax patterns
```

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | All constitution checks pass | N/A |

## Phase 1 Artifacts Generated

- [x] `data-model.md` - Entity definitions and validation rules
- [x] `contracts/openapi.yaml` - REST API specification (OpenAPI 3.0.3)
- [x] `quickstart.md` - Quick start guide with curl examples
- [x] Agent context updated in `AGENTS.md`

## Next Steps

Run `/speckit.tasks` to generate implementation tasks and begin development.
