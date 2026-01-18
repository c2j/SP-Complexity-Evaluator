# Implementation Plan: GaussDB Transaction and Subtransaction Statistics Optimization

**Branch**: `006-gaussdb-transaction-stats` | **Date**: 2026-01-17 | **Spec**: [link](spec.md)
**Input**: Feature specification from `/specs/006-gaussdb-transaction-stats/spec.md`

## Summary

Optimize transaction and subtransaction counting for GaussDB stored procedures. The system will correctly identify and count all transaction control statements (BEGIN, START TRANSACTION, COMMIT, ROLLBACK, SAVEPOINT, ROLLBACK TO SAVEPOINT, RELEASE SAVEPOINT) and provide complexity metrics based on transaction nesting depth and structure. This is a dialect-specific enhancement to the existing GaussComplexityEvaluator, following the interface-based design pattern.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3  
**Storage**: N/A (stateless REST API)  
**Testing**: JUnit 5 with @SpringBootTest  
**Target Platform**: Linux server (Spring Boot application)  
**Project Type**: Single Java project (Spring Boot backend)  
**Performance Goals**: Evaluation completes within same time bounds as existing complexity evaluation (no degradation)  
**Constraints**: Must integrate with existing GaussComplexityEvaluator, must follow dialect extensibility pattern  
**Scale/Scope**: Per-evaluation basis (stored procedure complexity analysis)  

**Unknowns (NEEDS CLARIFICATION)**:
- Exact pattern matching strategy for distinguishing BEGIN (transaction) vs BEGIN (anonymous block) in GaussDB syntax

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] SQL Dialect Extensibility: New dialect uses interface-based design (SqlParser, ComplexityEvaluator) - **This is a dialect-specific feature extending GaussComplexityEvaluator**
- [x] Test Coverage: JUnit 5 tests with @SpringBootTest follow naming pattern - **Will create GaussTransactionMetricsTest**
- [x] Error Resilience: Failed statements collected without breaking evaluation - **Follows existing FR-EH requirements**
- [x] REST API First: Endpoints follow consistent patterns with JSON/Excel support - **Extends existing ComplexityMetrics response**
- [x] Logging: @Slf4j used, DEBUG level for project packages - **Follows existing pattern in GaussComplexityEvaluator**

## Project Structure

### Documentation (this feature)

```text
specs/006-gaussdb-transaction-stats/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (to be generated)
├── data-model.md        # Phase 1 output (to be generated)
├── quickstart.md        # Phase 1 output (to be generated)
└── contracts/           # Phase 1 output (to be generated)
```

### Source Code (repository root)

```text
src/main/java/com/sdchat/ce/sp/complexity/
├── evaluator/
│   └── GaussComplexityEvaluator.java  # Extended with transaction metrics
├── model/
│   └── TransactionMetrics.java        # NEW: Transaction metrics DTO
│   └── TransactionStructure.java      # NEW: Nested transaction structure
│   └── SavepointInfo.java             # NEW: Savepoint metadata
└── parser/
    └── GaussSqlParser.java            # Enhanced with transaction detection

src/test/java/com/sdchat/ce/sp/complexity/
└── evaluator/
    └── GaussComplexityEvaluatorTest.java  # Enhanced with transaction tests
```

**Structure Decision**: Single Java project following existing Spring Boot structure. Transaction metrics will be added to the existing GaussComplexityEvaluator and GaussSqlParser, with new model classes for the metrics.

## Phase 0: Research Summary

**Status**: Complete  
**Research Output**: [research.md](research.md)

### Key Decisions

1. **BEGIN vs Anonymous Block Distinction**: Pattern matching based on next token after BEGIN
   - Transaction: `BEGIN;`, `BEGIN WORK;`, `BEGIN TRANSACTION;`, `BEGIN` + isolation level/read mode
   - Anonymous Block: `BEGIN` + `DECLARE`, `BEGIN` + statements + `END;`, `BEGIN` + `END /`

2. **Transaction Control Patterns**: Regex-based pattern matching consistent with existing codebase

3. **Complexity Score Formula**: `Score = (TransactionCount * 10) + (SavepointCount * 5) + (NestingDepth * 3) + (UnbalancedPenalty)`

### Unknowns Resolved

- All unknowns from Technical Context have been resolved in research.md
- No additional clarification needed before proceeding to Phase 1

## Phase 1: Design

**Status**: Complete  
**Data Model**: [data-model.md](data-model.md) - Complete  
**Contracts**: [contracts/api-contract.md](contracts/api-contract.md) - Complete  
**Quickstart**: [quickstart.md](quickstart.md) - Complete

### Design Decisions

**Pattern 1: Parser Enhancement**
Extend existing `GaussSqlParser` with transaction detection methods for BEGIN/START TRANSACTION, COMMIT/END/ROLLBACK, and savepoint operations.

**Pattern 2: Model Classes**
- Extend existing `ComplexityMetrics` with new transaction fields
- Create `TransactionMetrics` for comprehensive transaction complexity
- Create `SavepointInfo` for detailed savepoint metadata

**Pattern 3: Integration with ComplexityEvaluator**
Enhance `GaussComplexityEvaluator` with:
- New transaction counting methods
- Transaction complexity score calculation
- Enhanced transaction metrics in results

### Design Validation

- **Constitution Check**: Re-evaluated after design (see below)
- **Pattern Consistency**: All patterns follow existing codebase conventions
- **Error Handling**: Continues parsing on failures (Principle III)

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | All requirements satisfied with existing architecture | - |

## Post-Design Constitution Check

*Re-evaluated after Phase 1 design completion*

- [x] SQL Dialect Extensibility: Feature extends GaussComplexityEvaluator without breaking changes
- [x] Test Coverage: Tests will follow JUnit 5 pattern with @SpringBootTest
- [x] Error Resilience: Parsing continues on failures, partial results returned
- [x] REST API First: Transaction metrics included in existing ComplexityMetrics response
- [x] Logging: @Slf4j used, DEBUG level for parsing steps

**Result**: All gates pass ✅
