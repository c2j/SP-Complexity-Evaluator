# Implementation Plan: GaussDB Subtransaction Metrics

**Branch**: `002-gauss-subtransaction-metrics` | **Date**: 2026-01-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-gauss-subtransaction-metrics/spec.md`

## Summary

Add subtransaction detection and quantification capability for GaussDB stored procedures. The feature introduces explicit subtransaction detection (SAVEPOINT, ROLLBACK TO, RELEASE statements) and implicit subtransaction detection (DML statements, EXCEPTION blocks). Subtransactions from called procedures are aggregated to parent procedure complexity. Results are exported to Excel with detailed subtransaction information while preserving all existing complexity metrics.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.6, Lombok 1.18.32, JUnit 5, Apache POI 5.2.3
**Storage**: N/A (stateless evaluation service, files processed in memory/temp)
**Testing**: JUnit 5 with @SpringBootTest
**Target Platform**: JVM (Linux server)
**Project Type**: Single project (Spring Boot application)
**Performance Goals**: < 10% evaluation time degradation with subtransaction detection enabled (compared to baseline)
**Constraints**: Must not modify existing dialect support (Oracle, Hive); subtransaction depth limited to <10,000 for performance
**Scale/Scope**: Adds subtransaction metrics to existing GaussDB evaluator; impacts Excel export format

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] SQL Dialect Extensibility: New dialect uses interface-based design (SqlParser, ComplexityEvaluator)
- [x] Test Coverage: JUnit 5 tests with @SpringBootTest follow naming pattern
- [x] Error Resilience: Failed statements collected without breaking evaluation
- [x] REST API First: Endpoints follow consistent patterns with JSON/Excel support
- [x] Logging: @Slf4j used, DEBUG level for project packages

**Gate Status**: ✅ All gates passed - Proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/002-gauss-subtransaction-metrics/
 ├── plan.md              # This file (/speckit.plan command output)
 ├── research.md          # Phase 0 output (no unknowns to research)
 ├── data-model.md        # Phase 1 output (/speckit.plan command)
 ├── quickstart.md        # Phase 1 output (/speckit.plan command)
 ├── contracts/           # Phase 1 output (/speckit.plan command)
 └── checklists/
     └── requirements.md # Specification quality validation (completed)
```

### Source Code (repository root)

```text
src/main/java/com/sdchat/ce/sp/complexity/
 ├── evaluator/
 │   ├── ComplexityEvaluator.java              # Existing interface
 │   ├── OracleComplexityEvaluator.java        # Existing (unchanged)
 │   ├── GaussComplexityEvaluator.java          # MODIFY: Add subtransaction detection
 │   └── HiveComplexityEvaluator.java           # Existing (unchanged)
 ├── model/
 │   ├── ComplexityMetrics.java               # MODIFY: Add subtransaction fields
 │   ├── SubtransactionMetric.java            # NEW: Subtransaction detail model
 │   ├── StoredProcedure.java                  # Existing model
 │   └── SqlStatement.java                      # Existing model
 └── service/
     ├── ComplexityEvaluationService.java         # Existing interface
     └── ComplexityEvaluationServiceImpl.java    # MODIFY: Add subtransaction logic

src/test/java/com/sdchat/ce/sp/complexity/evaluator/
 ├── OracleComplexityEvaluatorTest.java         # Existing (unchanged)
 ├── GaussComplexityEvaluatorTest.java          # MODIFY: Add subtransaction tests
 └── HiveComplexityEvaluatorTest.java           # Existing (unchanged)

src/main/java/com/sdchat/ce/sp/complexity/util/
 └── ExcelExportUtil.java                      # MODIFY: Add subtransaction columns
```

**Structure Decision**: Single project Spring Boot structure - no frontend/backend separation. New SubtransactionMetric model added to model package, GaussComplexityEvaluator modified for subtransaction logic, Excel export utility updated for new columns.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations detected - no complexity tracking required.

---

## Phase 0: Outline & Research

### Summary

No unknowns in Technical Context - all requirements are clear and documented. Specification has no [NEEDS CLARIFICATION] markers. Research phase can be skipped.

**Research Status**: ✅ Complete - No research tasks required

---

## Phase 1: Design & Contracts

### Summary

Design data models for subtransaction metrics and update Excel export to include subtransaction details. Extend GaussComplexityEvaluator with subtransaction detection logic while preserving existing complexity metrics.

### Data Model Design

**Entity**: SubtransactionMetric

**Purpose**: Represents detailed information about a single subtransaction detected in a stored procedure.

**Fields**:
- `name`: String - Savepoint name for explicit subtransactions, generated name for implicit subtransactions
- `type`: Enum (EXPLICIT, IMPLICIT) - Type of subtransaction
- `dmlStatements`: List<String> - Associated DML statements for implicit subtransactions
- `savepointOperations`: List<String> - Operations (SAVEPOINT, ROLLBACK, RELEASE) for explicit subtransactions
- `nestingLevel`: Integer - Nesting depth of this subtransaction
- `sourceProcedure`: String - Name of procedure containing this subtransaction
- `sourceLine`: Integer - Line number in source code where subtransaction detected

**Rationale**: Separate model allows for detailed subtransaction tracking and Excel export without complicating ComplexityMetrics model.

### Excel Export Contract

**New Columns** (to be added to existing Excel export):

| Column | Type | Description |
|--------|------|-------------|
| Subtransaction Count | Integer | Total number of subtransactions detected |
| Subtransaction Details | String | JSON-formatted string of SubtransactionMetric[] for detailed export |
| Max Subtransaction Nesting Level | Integer | Maximum nesting depth of subtransactions |

**Backward Compatibility**: Existing Excel columns (Overall Score, Table Count, Loop Count, etc.) remain unchanged.

### API Contract Updates

No new API endpoints required - subtransaction detection is an internal enhancement to existing evaluation process.

### Agent Context Update

No new technologies introduced - agent context update not required.

---

## Phase 2: Implementation Tasks

**Note**: Tasks will be generated by `/speckit.tasks` command when implementation phase begins.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 0 (Research)**: No dependencies - already complete
- **Phase 1 (Design)**: Depends on Phase 0 - BLOCKS implementation
- **Phase 2 (Implementation)**: Depends on Phase 1 design completion

### User Story Dependencies

- **User Story 1 (Explicit Subtransactions)**: Depends on data model and Excel export design
- **User Story 2 (Implicit Subtransactions)**: Depends on data model and GaussComplexityEvaluator modifications
- **User Story 3 (Nested Aggregation)**: Depends on User Stories 1 and 2

### Implementation Order

1. Create SubtransactionMetric model (supports all user stories)
2. Update ComplexityMetrics model with subtransaction fields (supports all user stories)
3. Implement explicit subtransaction detection (User Story 1)
4. Implement implicit subtransaction detection (User Story 2)
5. Implement nested procedure aggregation (User Story 3)
6. Update Excel export to include subtransaction columns (cross-cutting concern)
7. Write comprehensive tests for all subtransaction types (cross-cutting concern)

### Parallel Opportunities

- SubtransactionMetric model and ComplexityMetrics updates can be done in parallel
- Explicit, implicit, and nested subtransaction detection implementations can be developed in parallel (if team capacity allows)
- Tests can be written in parallel once models are complete

---

## Notes

- Subtransaction detection applies ONLY to GaussDB dialect (as specified in constraints)
- Oracle and Hive dialects remain unchanged - this is a dialect-specific enhancement
- Loop multiplier is an estimation parameter (exact loop counting is out of scope)
- Performance impact must be measured against baseline to ensure < 10% degradation
- Excel export format changes must maintain backward compatibility
- All subtransaction detection errors must be logged (not fail entire evaluation)
