---

**description**: "Task list for feature implementation"
---

# Tasks: GaussDB Subtransaction Metrics

**Input**: Design documents from `/specs/002-gauss-subtransaction-metrics/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md
**Tests**: Tests are NOT requested for this feature - test tasks will NOT be generated.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/`, `src/test/java/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure
- Models in `src/main/java/com/sdchat/ce/sp/complexity/model/`
- Evaluators in `src/main/java/com/sdchat/ce/sp/complexity/evaluator/`
- Tests in `src/test/java/com/sdchat/ce/sp/complexity/evaluator/`
- Utilities in `src/main/java/com/sdchat/ce/sp/complexity/util/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create SubtransactionMetric model in src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionMetric.java
- [X] T002 Update ComplexityMetrics model with subtransaction fields in src/main/java/com/sdchat/ce/sp/complexity/model/ComplexityMetrics.java
- [X] T003 [P] Update ExcelExportUtil to add subtransaction columns in src/main/java/com/sdchat/ce/sp/complexity/util/ExcelExportUtil.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Add SubtransactionType enum in src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionType.java
- [X] T005 [P] Add SubtransactionContext tracking class in src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionContext.java
- [X] T006 [P] Add LoopMultiplierConfig model in src/main/java/com/sdchat/ce/sp/complexity/model/LoopMultiplierConfig.java
- [X] T007 Create Jackson ObjectMapper configuration for JSON serialization (if not exists)
- [X] T008 [P] Update GaussComplexityEvaluator to add subtransaction tracking fields in src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Explicit Subtransaction Detection (Priority: P1) 🎯 MVP

**Goal**: Detect and quantify explicit subtransactions in GaussDB stored procedures by identifying SAVEPOINT, ROLLBACK TO, and RELEASE SAVEPOINT statements.

**Independent Test**: Can be tested by evaluating GaussDB stored procedures containing SAVEPOINT statements and verifying that subtransaction count, names, and operations are correctly extracted from source code.

### Implementation for User Story 1

- [ ] T009 [US1] Add explicit subtransaction detection constants to GaussComplexityEvaluator
- [ ] T010 [US1] Implement SAVEPOINT statement detection in GaussComplexityEvaluator
- [ ] T011 [US1] Implement ROLLBACK TO SAVEPOINT detection and association in GaussComplexityEvaluator
- [ ] T012 [US1] Implement RELEASE SAVEPOINT detection and tracking in GaussComplexityEvaluator
- [ ] T013 [US1] Implement savepoint nesting level calculation in GaussComplexityEvaluator
- [ ] T014 [US1] Implement savepoint stack tracking for GaussDB same-named SAVEPOINT behavior in GaussComplexityEvaluator
- [ ] T015 [US1] Integrate subtransaction counting into existing complexity score calculation in GaussComplexityEvaluator
- [ ] T016 [US1] Update ComplexityEvaluationServiceImpl to pass loop multiplier to GaussComplexityEvaluator
- [ ] T017 [US1] Update ExcelExportUtil to write subtransaction count, details, and max nesting columns

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Implicit Subtransaction Detection (Priority: P2)

**Goal**: Detect and quantify implicit subtransactions created by DML statements (INSERT, DELETE, UPDATE) and EXCEPTION blocks in GaussDB stored procedures.

**Independent Test**: Can be tested by evaluating GaussDB stored procedures with DML statements and EXCEPTION blocks without explicit SAVEPOINT statements, verifying correct subtransaction counting.

### Implementation for User Story 2

- [ ] T018 [US2] Add implicit subtransaction detection constants to GaussComplexityEvaluator
- [ ] T019 [US2] Implement DML statement detection (INSERT, DELETE, UPDATE) in GaussComplexityEvaluator
- [ ] T020 [US2] Implement EXCEPTION block detection for implicit subtransactions in GaussComplexityEvaluator
- [ ] T021 [US2] Implement implicit subtransaction counting with DML detection in GaussComplexityEvaluator
- [ ] T022 [US2] Implement EXCEPTION block counting in GaussComplexityEvaluator
- [ ] T023 [US2] Integrate implicit subtransaction detection into existing complexity score in GaussComplexityEvaluator
- [ ] T024 [US2] Apply loop multiplier to implicit subtransaction count in GaussComplexityEvaluator
- [ ] T025 [US2] Update ExcelExportUtil to include implicit subtransactions in details column

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Nested Procedure Subtransaction Aggregation (Priority: P3)

**Goal**: Aggregate subtransactions from called procedures when stored procedures call other stored procedures. If procedure A calls procedure B, B's transactions should be counted as A's subtransactions.

**Independent Test**: Can be tested by evaluating a parent procedure that calls a child procedure, verifying that parent's subtransaction count includes both its own and child's subtransactions.

### Implementation for User Story 3

- [ ] T026 [US3] Add nested procedure call detection constants to GaussComplexityEvaluator
- [ ] T027 [US3] Implement nested procedure call parsing in GaussComplexityEvaluator
- [ ] T028 [US3] Implement recursive subtransaction aggregation logic in GaussComplexityEvaluator
- [ ] T029 [US3] Add called procedures set tracking in GaussComplexityEvaluator
- [ ] T030 [US3] Mark aggregated subtransactions with sourceProcedure name in GaussComplexityEvaluator
- [ ] T031 [US3] Integrate nested aggregation into complexity score in GaussComplexityEvaluator
- [ ] T032 [US3] Update ExcelExportUtil to include nested aggregation information in details column

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T033 [P] Update README.md with subtransaction metrics documentation
- [ ] T034 [P] Update API endpoint documentation with loop multiplier parameter
- [ ] T035 [P] Code cleanup and refactoring in GaussComplexityEvaluator
- [ ] T036 Code cleanup in SubtransactionContext and SubtransactionMetric models
- [ ] T037 [P] Add logging for subtransaction detection operations in GaussComplexityEvaluator
- [ ] T038 [P] Add logging for Excel export updates
- [ ] T039 Run full test suite: ./mvnw test
- [ ] T040 [P] Validate subtransaction metrics accuracy in test procedures
- [ ] T041 Performance baseline measurement: Evaluate sample procedure without subtransaction detection to establish baseline

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase - No dependencies on other stories
- **User Story 2 (Phase 4)**: Depends on Foundational phase - No dependencies on other stories
- **User Story 3 (Phase 5)**: Depends on User Stories 1 and 2 - Must wait for both user stories to complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on US1 and US2 completion (nested aggregation needs base detection)

### Within Each User Story

- Models before evaluator logic
- Detection before complexity score integration
- Excel export after all evaluation logic complete

### Parallel Opportunities

- **Phase 1 (Setup)**: T001, T002, T003 can run in parallel [P]
- **Phase 2 (Foundational)**: T004, T005, T006, T007, T008 can run in parallel [P]
- **Phase 3 (US1)**: T009-T016 are sequential (depend on each other), T017 can run in parallel with T016
- **Phase 4 (US2)**: T018-T024 are sequential (depend on each other), T025 can run in parallel with T024
- **Phase 5 (US3)**: T026-T032 are sequential (depend on each other), can start in parallel once US1, US2 detection is ready
- **Phase 6 (Polish)**: T033-T041 can run in parallel [P], some cleanup tasks can start before all user stories complete

### MVP First Strategy (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test explicit subtransaction detection independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

---

## Implementation Strategy

### Explicit Subtransaction Detection (US1)

The explicit subtransaction detection uses pattern matching for SAVEPOINT keywords:

1. **SAVEPOINT Statement**: Pattern `\\bSAVEPOINT\\s+([\\w_]+)\\b` captures savepoint name
2. **ROLLBACK TO**: Pattern `\\bROLLBACK\\s+TO\\s+(?:SAVEPOINT\\s)?\\s+([\\w_]+)\\b` associates rollback with savepoint
3. **RELEASE SAVEPOINT**: Pattern `\\bRELEASE\\s+(?:SAVEPOINT\\s)?\\s+([\\w_]+)\\b` marks savepoint completion

Nesting is tracked using a stack:
- Each SAVEPOINT pushes savepoint name to stack
- Each ROLLBACK TO or RELEASE pops from stack
- Current stack size = nesting level

### Implicit Subtransaction Detection (US2)

Implicit subtransactions are detected in two ways:

1. **DML Statements**: Count INSERT, DELETE, UPDATE statements that do not have explicit SAVEPOINT markers
2. **EXCEPTION Blocks**: Each EXCEPTION block in BEGIN...EXCEPTION...END structure creates one implicit subtransaction

Implicit subtransactions are multiplied by loopMultiplier parameter (default: 1).

### Nested Procedure Aggregation (US3)

When procedure A calls procedure B:

1. Parse procedure A's source code for nested procedure calls
2. For each called procedure name, parse that procedure's source code
3. Count all subtransactions in the called procedure
4. Add to procedure A's subtransaction count
5. Mark aggregated subtransactions with sourceProcedure = "B"

Recursive handling is implemented to support chains like A → B → C.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests are NOT requested for this feature (per spec)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Subtransaction detection applies ONLY to GaussDB dialect
- Oracle and Hive evaluators remain unchanged
- Loop multiplier is an estimation parameter (exact loop counting is out of scope)
- All subtransaction detection errors must be logged but not block evaluation (Error Resilience principle)
