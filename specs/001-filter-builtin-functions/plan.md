# Implementation Plan: Filter GaussDB Built-in Functions from Analysis Results

**Branch**: `[001-filter-builtin-functions]` | **Date**: 2026-01-15 | **Spec**: [link](spec.md)
**Input**: Feature specification from `/specs/001-filter-builtin-functions/spec.md`

## Summary

This feature adds the ability to filter GaussDB built-in functions from Table and Procedure complexity analysis results. Using the `gaussdb_functions.json` file (containing 1316 functions across 44 categories), the system will identify and exclude database built-in functions from complexity metrics, ensuring analysis results reflect only user-defined code complexity. The filtering integrates with the existing GaussComplexityEvaluator and requires extending result models to include filtered function reports for transparency.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.2.6  
**Primary Dependencies**: Lombok 1.18.32, Apache POI 5.2.3, JUnit 5  
**Storage**: N/A (stateless REST API, JSON file from classpath)  
**Testing**: JUnit 5 with @SpringBootTest  
**Target Platform**: Linux server (Java 17 compatible)  
**Project Type**: Single Spring Boot application  
**Performance Goals**: Filtering adds negligible overhead; 1316 functions lookup in O(1) using HashSet  
**Constraints**: JSON file must be bundled into JAR; case-insensitive exact matching; graceful fallback on missing file  
**Scale/Scope**: Supports all GaussDB SQL parsing; handles batch analysis of multiple procedures/tables  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] SQL Dialect Extensibility: Feature extends existing Gauss dialect evaluator, no new dialect required
- [x] Test Coverage: JUnit 5 tests with @SpringBootTest follow naming pattern methodName_condition_expectedResult
- [x] Error Resilience: Failed parsing logged without breaking evaluation; graceful fallback on missing JSON
- [x] REST API First: Endpoints follow existing patterns; JSON response format extended with filteredFunctions field
- [x] Logging: @Slf4j used; DEBUG for filtering steps, WARN for missing JSON, ERROR for failures

**Post-Phase 1 Re-evaluation**: All gates continue to pass. No Constitution violations identified.

## Phase 1 Complete

**Deliverables**:
- [x] plan.md - Technical context and architecture
- [x] data-model.md - Entity definitions (BuiltInFunction, FunctionFilterResult, ComplexityMetrics extended)
- [x] quickstart.md - Build and usage instructions
- [x] contracts/openapi.yaml - API response schemas

**Next**: Phase 2 - Generate tasks.md with `/speckit.tasks`

## Project Structure

### Documentation (this feature)

```text
specs/001-filter-builtin-functions/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (N/A - no unknowns)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/sdchat/ce/sp/complexity/
├── config/
│   └── BuiltInFunctionConfig.java        # NEW: Loads JSON at startup
├── evaluator/
│   └── GaussComplexityEvaluator.java     # MODIFIED: Integrate filtering
├── model/
│   ├── BuiltInFunction.java              # NEW: Data model for JSON entries
│   ├── FunctionFilterResult.java         # NEW: Filtering outcome model
│   └── ComplexityMetrics.java            # MODIFIED: Add filteredFunctions field
└── util/
    └── BuiltInFunctionFilter.java        # NEW: Filtering utility

src/test/java/com/sdchat/ce/sp/complexity/
└── evaluator/
    └── GaussComplexityEvaluatorTest.java # MODIFIED: Add filtering tests
```

**Structure Decision**: Feature follows existing package structure. New models in `model/`, new configuration in `config/`, new utility in `util/`. Existing `GaussComplexityEvaluator` extended with filtering integration. Tests updated to verify filtering behavior.

## Phase 0: Research

No research required - all technical choices are determined by existing project architecture and Java/Spring Boot conventions. The feature extends existing components rather than introducing new technologies.

## Phase 1: Design

### Data Model

**BuiltInFunction**
- `name`: String - Function name (e.g., "gs_index_advise")
- `category`: String - Category from JSON (e.g., "AI特性函数")
- `parameters`: String - Parameter signature
- `description`: String - Function description
- `returnType`: String - Return type information

**FunctionFilterResult**
- `filteredFunctions`: List<BuiltInFunction> - List of functions that were filtered
- `filteredCount`: int - Number of functions filtered
- `categoryBreakdown`: Map<String, Integer> - Count by category
- `retainedCount`: int - Number of user-defined functions retained

**ComplexityMetrics** (extended)
- `filteredFunctions`: FunctionFilterResult - NEW FIELD - Functions excluded from analysis

### API Contracts

No new endpoints required. Existing REST endpoints extended with new response field:

**Response Format** (JSON):
```json
{
  "overallScore": 85.5,
  "tableCount": 3,
  "joinCount": 2,
  "procedureCallCount": 10,
  "filteredFunctions": {
    "filteredCount": 5,
    "retainedCount": 5,
    "categoryBreakdown": {
      "AI特性函数": 2,
      "HashFunc函数": 3
    },
    "filteredFunctions": [
      {"name": "gs_index_advise", "category": "AI特性函数"},
      {"name": "hash_array", "category": "HashFunc函数"}
    ]
  }
}
```

### Quickstart

1. **Build**: `./mvnw clean package` - JSON file bundled in JAR automatically
2. **Run**: `./mvnw spring-boot:run` - Built-in functions loaded at startup
3. **Test**: `./mvnw test -Dtest=GaussComplexityEvaluatorTest` - Run evaluator tests
4. **Verify**: Upload GaussDB procedure/table; check `filteredFunctions` in response

## Complexity Tracking

No Constitution Check violations requiring justification.
