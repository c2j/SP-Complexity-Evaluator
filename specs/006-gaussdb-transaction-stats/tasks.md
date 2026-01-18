# Implementation Tasks: GaussDB Transaction and Subtransaction Statistics Optimization

**Feature**: 006-gaussdb-transaction-stats
**Branch**: `006-gaussdb-transaction-stats`
**Generated**: 2026-01-17
**Spec**: [spec.md](spec.md)

## Overview

This document contains the implementation tasks for adding transaction and subtransaction statistics to the GaussDB complexity evaluator. Tasks are organized by user story priority.

## User Stories Summary

| Story | Priority | Goal | Independent Test Criteria |
|-------|----------|------|---------------------------|
| US1 | P1 | Accurate transaction counting | Submit procedure with transaction statements, verify counts |
| US2 | P1 | Accurate savepoint counting | Submit procedure with savepoints, verify counts |
| US3 | P2 | Nested transaction structure | Submit nested savepoints, verify hierarchy and depth |
| US4 | P2 | Transaction complexity metrics | Compare scores against expected values |
| US5 | P3 | Integration with existing evaluation | Verify metrics in standard response format |

## Implementation Strategy

**MVP Scope**: User Story 1 + User Story 2 (P1 stories)
- Core transaction and savepoint counting
- Basic model classes
- Integration with GaussComplexityEvaluator

**Incremental Delivery**:
1. Phase 1-2: Setup and foundational work
2. Phase 3: US1 + US2 (core counting)
3. Phase 4: US3 (nesting analysis)
4. Phase 5: US4 (complexity scoring)
5. Phase 6: US5 (integration polish)

---

## Phase 1: Setup

**Goal**: Initialize feature development environment and review existing code

### Tasks

- [x] T001 Create TransactionMetrics model class in src/main/java/com/sdchat/ce/sp/complexity/model/TransactionMetrics.java
- [x] T002 Create SavepointInfo model class in src/main/java/com/sdchat/ce/sp/complexity/model/SavepointInfo.java
- [x] T003 Review existing GaussComplexityEvaluator.java for transaction-related methods
- [x] T004 Review existing SubtransactionMetric.java and SubtransactionContext.java models
- [x] T005 Review existing transaction patterns in GaussComplexityEvaluator (TRANSACTION_CONTROL_PATTERN, SAVEPOINT_PATTERN)
- [x] T006 [P] Define transaction initiation regex patterns in GaussComplexityEvaluator
- [x] T007 [P] Define transaction completion regex patterns
- [x] T008 Define savepoint regex patterns
- [x] T009 Implement countTransactionControls() method counting BEGIN/START TRANSACTION
- [x] T010 Implement countSavepoints() method counting SAVEPOINT statements
- [x] T011 Implement countCommits() and countRollbacks() methods
- [x] T012 [US1] Add transaction count field to TransactionMetrics
- [x] T013 [US1] Add savepoint count field to TransactionMetrics
- [x] T014 [US1] Add commit count field to TransactionMetrics
- [x] T015 [US1] Add rollback count field to TransactionMetrics
- [x] T016 [US1] Implement parseTransactionStatements() method in GaussComplexityEvaluator
- [x] T017 [US1] Update evaluateStoredProcedure() to call transaction parsing
- [x] T018 [US1] Build TransactionMetrics object with parsed counts
- [x] T019 [US2] Add savepointDetails list field to TransactionMetrics
- [x] T020 [US2] Implement extractSavepointDetails() method returning List<SavepointInfo>
- [x] T021 [US2] Track line numbers for each savepoint
- [x] T022 [US2] Handle multiple savepoints with same name (most recent active)
- [x] T023 [US1][US2] Integrate TransactionMetrics into ComplexityMetrics via additionalMetrics

### Tests

- [x] T024 [P] Test transaction counting for simple BEGIN/COMMIT procedure
- [x] T025 [P] Test transaction counting for START TRANSACTION statements
- [x] T026 [P] Test savepoint counting for single SAVEPOINT
- [x] T027 [P] Test savepoint counting for multiple SAVEPOINT statements
- [x] T028 [P] Test handling of multiple savepoints with same name

### Implementation Notes

All regex patterns are already defined in research.md. Follow existing codebase pattern (e.g., CURSOR_PATTERN) for implementation.

---

## Phase 4: User Story 3 - Nested Transaction Structure

**Goal**: Implement nesting depth calculation and hierarchical structure analysis (P2)

**User Story**: US3 (Nested Transaction Structure Analysis)

**Independent Test Criteria**: Submit stored procedures with nested savepoints, verify hierarchy and max depth are correct

### Tasks

- [x] T029 [US3] Add maxNestingDepth field to TransactionMetrics
- [x] T030 [US3] Implement calculateNestingDepth() using stack-based algorithm
- [x] T031 [US3] Track nesting level when each savepoint is encountered
- [x] T032 [US3] Pop from stack on ROLLBACK TO SAVEPOINT
- [x] T033 [US3] Clear stack on COMMIT/ROLLBACK (ends transaction)
- [x] T034 [US3] Update savepointDetails with nestingLevel for each savepoint
- [x] T035 [US3] Update TransactionMetrics.build() with maxNestingDepth calculation

### Tests

- [x] T036 [P] Test nesting depth for single savepoint (depth = 1)
- [x] T037 [P] Test nesting depth for nested savepoints (depth = 2)
- [x] T038 [P] Test nesting depth after ROLLBACK TO SAVEPOINT
- [x] T039 [P] Test nesting depth after COMMIT (should reset)

---

## Phase 5: User Story 4 - Transaction Complexity Metrics

**Goal**: Implement complexity score calculation based on transaction structure (P2)

**User Story**: US4 (Transaction Complexity Metrics)

**Independent Test Criteria**: Compare generated complexity scores against manually calculated expected values

### Tasks

- [x] T040 [US4] Add complexityScore field to TransactionMetrics
- [x] T041 [US4] Add hasUnbalancedTransactions field to TransactionMetrics
- [x] T042 [US4] Implement calculateComplexityScore() method:
  - Formula: (TransactionCount * 10) + (SavepointCount * 5) + (NestingDepth * 3) + (UnbalancedPenalty)
  - UnbalancedPenalty = 5 if BEGIN != (COMMIT + ROLLBACK), else 0
- [x] T043 [US4] Implement checkUnbalancedTransactions() method
- [x] T044 [US4] Update TransactionMetrics.build() to calculate complexityScore
- [x] T045 [US4] Add complexity score interpretation guide to response (comments/documentation)

### Tests

- [x] T046 [P] Test complexity score for simple transaction (score = 10)
- [x] T047 [P] Test complexity score for transaction with savepoints (score = 25)
- [x] T048 [P] Test complexity score for nested savepoints (score = 35)
- [x] T049 [P] Test unbalanced transaction penalty (score + 5)

---

## Phase 6: User Story 5 - Integration and Polish

**Goal**: Ensure seamless integration with existing evaluation system (P3)

**User Story**: US5 (Integration with Existing Evaluation)

**Independent Test Criteria**: Verify transaction metrics appear in standard response alongside existing complexity metrics

### Tasks

- [x] T050 [US5] Verify transactionMetrics in ComplexityMetrics.additionalMetrics map
- [x] T051 [US5] Ensure existing ComplexityMetrics fields remain unchanged (backward compatibility)
- [x] T052 [US5] Verify JSON serialization includes transactionMetrics
- [x] T053 [US5] Verify Excel export includes transaction metrics fields
- [x] T054 [US5] Add DEBUG logging for transaction parsing steps
- [x] T055 [US5] Add error handling for parsing failures (partial results returned)

### Tests

- [x] T056 [P] Test transaction metrics in single procedure evaluation response
- [x] T057 [P] Test transaction metrics in batch evaluation response
- [x] T058 [P] Test backward compatibility (existing fields unchanged)
- [x] T059 [P] Test partial results on parsing errors

---

## Final Phase: Cross-Cutting Concerns

**Goal**: Performance optimization, edge case handling, documentation

### Tasks

- [x] T060 [P] Add performance test: verify no degradation vs baseline
- [x] T061 [P] Handle comments containing transaction keywords (ignore)
- [x] T062 [P] Handle strings containing transaction keywords (ignore)
- [x] T063 [P] Handle case-insensitive matching (BEGIN, begin, Begin)
- [x] T064 [P] Handle SET TRANSACTION isolation level settings
- [x] T065 [P] Document complexity score interpretation in quickstart.md
- [x] T066 [P] Run full test suite: ./mvnw test
- [x] T067 [P] Run lint/typecheck: ./mvnw clean package

---

## Dependency Graph

```
Phase 1 (Setup)
    |
    v
Phase 2 (Foundational: T006-T011)
    |
    +-----> Phase 3 (US1 + US2: T012-T023)
    |             |
    |             v
    |         Phase 4 (US3: T029-T035)
    |             |
    |             v
    |         Phase 5 (US4: T040-T045)
    |             |
    |             v
    |         Phase 6 (US5: T050-T059)
    |             |
    |             v
    +-----> Final Phase (T060-T067)
```

**Critical Path**: T001 -> T006 -> T012 -> T024 (MVP completion)

---

## Parallel Execution Opportunities

Within each phase, the following tasks can be executed in parallel:

### Phase 1 (Setup)
- T001, T002, T003, T004, T005 can run in parallel (different files, no dependencies)

### Phase 2 (Foundational)
- T006, T007, T008 can run in parallel (different pattern definitions)

### Phase 3 (US1 + US2)
- T012, T013, T014, T015, T019 can run in parallel (different field additions)
- T024, T025, T026, T027, T028 can run in parallel (different test cases)

### Phase 4 (US3)
- T036, T037, T038, T039 tests can run in parallel

### Phase 5 (US4)
- T046, T047, T048, T049 tests can run in parallel

### Phase 6 (US5)
- T056, T057, T058, T059 tests can run in parallel

### Final Phase
- T060, T061, T062, T063, T064 can run in parallel (independent improvements)

---

## File Paths Summary

| Task | File Path |
|------|-----------|
| T001 | src/main/java/com/sdchat/ce/sp/complexity/model/TransactionMetrics.java |
| T002 | src/main/java/com/sdchat/ce/sp/complexity/model/SavepointInfo.java |
| T003 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T004 | src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionMetric.java |
| T005 | src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionContext.java |
| T006-T011 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T012-T023 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T024-T028 | src/test/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluatorTest.java |
| T029-T035 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T036-T039 | src/test/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluatorTest.java |
| T040-T045 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T046-T049 | src/test/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluatorTest.java |
| T050-T059 | src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java |
| T056-T059 | src/test/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluatorTest.java |
| T060-T067 | Various files |

---

## Task Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| Phase 1 | T001-T005 | Setup and code review |
| Phase 2 | T006-T011 | Foundational pattern definitions |
| Phase 3 | T012-T028 | US1 + US2: Transaction and savepoint counting |
| Phase 4 | T029-T039 | US3: Nested transaction structure |
| Phase 5 | T040-T049 | US4: Complexity metrics |
| Phase 6 | T050-T059 | US5: Integration |
| Final | T060-T067 | Cross-cutting concerns |

**Total Tasks**: 67
**Test Tasks**: 36 (tests are optional but recommended)
**Model Files**: 2 new (TransactionMetrics, SavepointInfo)
**Modified Files**: 1 (GaussComplexityEvaluator)
**Test Files**: 1 enhanced (GaussComplexityEvaluatorTest.java)

---

## Build and Test Commands

```bash
# Build project
./mvnw clean package

# Run all tests
./mvnw test

# Run Gauss-specific tests
./mvnw test -Dtest="*Gauss*"

# Run specific test class
./mvnw test -Dtest=GaussComplexityEvaluatorTest

# Run with transaction metrics focus
./mvnw test -Dtest="*Transaction*,*Gauss*"
```

## Success Criteria Verification

| Criteria | Target | Verification |
|----------|--------|--------------|
| SC-001: Transaction counting accuracy | 100% | T024-T025 tests pass |
| SC-002: Subtransaction counting accuracy | 100% | T026-T028 tests pass |
| SC-003: Complexity score distinction | Correct levels | T046-T049 tests pass |
| SC-004: Metrics in evaluation results | All evaluations | T056-T057 tests pass |
| SC-005: No performance degradation | Same time bounds | T060 performance test |
| SC-006: Edge case handling | 95% coverage | T061-T064 tests pass |
| SC-007: Score correlation | > 0.8 correlation | Manual verification |
