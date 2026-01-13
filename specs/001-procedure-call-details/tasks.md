# Tasks: Procedure Call Details in Output Formats

**Input**: Design documents from `/specs/001-procedure-call-details/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included - Constitution requires JUnit 5 tests for all evaluator changes.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

For SP-Complexity-Evaluator:
- `src/main/java/com/sdchat/ce/sp/complexity/model/` - Data models (DTOs, entities)
- `src/main/java/com/sdchat/ce/sp/complexity/evaluator/` - Dialect evaluators
- `src/main/java/com/sdchat/ce/sp/complexity/util/` - Utility classes (Excel export)
- `src/main/java/com/sdchat/ce/sp/complexity/controller/` - REST controllers
- `src/test/java/com/sdchat/ce/sp/complexity/evaluator/` - Evaluator tests

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Verify feature branch `001-procedure-call-details` is active
- [ ] T002 Confirm all design documents are available (research.md, data-model.md, contracts/)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 [P] Create `ProcedureCallMetric.java` model in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/model/ProcedureCallMetric.java`
- [ ] T004 Add `procedureCallCount` and `procedureCallDetails` fields to `ComplexityMetrics.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/model/ComplexityMetrics.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Single Procedure JSON Output (Priority: P1) 🎯 MVP

**Goal**: Add procedure call detection to evaluators and output results in JSON format for single stored procedure evaluation

**Independent Test**: Evaluate a single stored procedure via API endpoint `/api/complexity/stored-procedure` with known procedure calls and verify JSON response includes `procedureCallCount` and `procedureCallDetails` with correct values

### Tests for User Story 1

- [ ] T005 [P] [US1] Contract test: Single procedure evaluation with procedure calls in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/test/java/com/sdchat/ce/sp/complexity/evaluator/OracleComplexityEvaluatorTest.java`
- [ ] T006 [P] [US1] Contract test: Single procedure evaluation with procedure calls in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/test/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluatorTest.java`
- [ ] T007 [P] [US1] Contract test: Single procedure evaluation with function calls in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/test/java/com/sdchat/ce/sp/complexity/evaluator/HiveComplexityEvaluatorTest.java`

### Implementation for User Story 1

- [ ] T008 [US1] Add procedure call regex patterns to `OracleComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/OracleComplexityEvaluator.java`
- [ ] T009 [US1] Add loop depth tracking to `OracleComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/OracleComplexityEvaluator.java`
- [ ] T010 [US1] Add procedure call aggregation logic to `OracleComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/OracleComplexityEvaluator.java`
- [ ] T011 [US1] Update `ComplexityMetrics` builder calls in `OracleComplexityEvaluator.java` to include `procedureCallCount` and `procedureCallDetails`
- [ ] T012 [P] [US1] Add procedure call regex patterns to `GaussComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java`
- [ ] T013 [P] [US1] Add loop depth tracking to `GaussComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java`
- [ ] T014 [US1] Add procedure call aggregation logic to `GaussComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java`
- [ ] T015 [US1] Update `ComplexityMetrics` builder calls in `GaussComplexityEvaluator.java` to include `procedureCallCount` and `procedureCallDetails`
- [ ] T016 [P] [US1] Add function call regex patterns to `HiveComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/HiveComplexityEvaluator.java`
- [ ] T017 [P] [US1] Add loop depth tracking to `HiveComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/HiveComplexityEvaluator.java`
- [ ] T018 [US1] Add function call aggregation logic to `HiveComplexityEvaluator.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/evaluator/HiveComplexityEvaluator.java`
- [ ] T019 [US1] Update `ComplexityMetrics` builder calls in `HiveComplexityEvaluator.java` to include `procedureCallCount` and `procedureCallDetails`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Excel Export (Priority: P1)

**Goal**: Add "Procedure Call Count" and "Procedure Call Details" columns to Excel export output

**Independent Test**: Evaluate stored procedures with procedure calls via batch upload, request Excel format, and verify columns 12-13 contain correct data (counts and comma-separated summaries with "(in loop)" suffix)

### Tests for User Story 2

- [ ] T020 [P] [US2] Contract test: Excel export includes procedure call columns in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/test/java/com/sdchat/ce/sp/complexity/controller/ComplexityEvaluationControllerTest.java`

### Implementation for User Story 2

- [ ] T021 [US2] Add "Procedure Call Count" column to `columns[]` array in `ExcelExportUtil.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/util/ExcelExportUtil.java`
- [ ] T022 [US2] Add "Procedure Call Details" column to `columns[]` array in `ExcelExportUtil.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/util/ExcelExportUtil.java`
- [ ] T023 [US2] Add `formatProcedureCallDetails()` helper method to `ExcelExportUtil.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/util/ExcelExportUtil.java`
- [ ] T024 [US2] Populate "Procedure Call Count" cell with `metrics.getProcedureCallCount()` in data row loop in `ExcelExportUtil.java`
- [ ] T025 [US2] Populate "Procedure Call Details" cell with `formatProcedureCallDetails(metrics.getProcedureCallDetails())` in data row loop in `ExcelExportUtil.java`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Batch JSON Export (Priority: P2)

**Goal**: Ensure batch JSON evaluation exports include procedure call fields for all stored procedures in the batch

**Independent Test**: Evaluate multiple stored procedures via batch upload, request JSON format, and verify each procedure object includes `procedureCallCount` and `procedureCallDetails` fields

### Tests for User Story 3

- [ ] T026 [P] [US3] Contract test: Batch JSON export includes procedure call fields in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/test/java/com/sdchat/ce/sp/complexity/controller/ComplexityEvaluationControllerTest.java`

### Implementation for User Story 3

- [ ] T027 [US3] Add `procedureCallCount(metrics.getProcedureCallCount())` to `ComplexityMetrics.builder()` in package body processing in `ComplexityEvaluationController.java` in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/src/main/java/com/sdchat/ce/sp/complexity/controller/ComplexityEvaluationController.java`
- [ ] T028 [US3] Add `procedureCallDetails(metrics.getProcedureCallDetails())` to `ComplexityMetrics.builder()` in package body processing in `ComplexityEvaluationController.java`
- [ ] T029 [US3] Add `procedureCallCount(metrics.getProcedureCallCount())` to `ComplexityMetrics.builder()` in stored procedure processing in `ComplexityEvaluationController.java`
- [ ] T030 [US3] Add `procedureCallDetails(metrics.getProcedureCallDetails())` to `ComplexityMetrics.builder()` in stored procedure processing in `ComplexityEvaluationController.java`
- [ ] T031 [US3] Add `procedureCallCount(metrics.getProcedureCallCount())` to `ComplexityMetrics.builder()` in regular SQL processing in `ComplexityEvaluationController.java`
- [ ] T032 [US3] Add `procedureCallDetails(metrics.getProcedureCallDetails())` to `ComplexityMetrics.builder()` in regular SQL processing in `ComplexityEvaluationController.java`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T033 [P] Update `AGENTS.md` with procedure call detection patterns and Excel column information in `/Volumes/Raiden_C2J/Projects/Desktop_Projects/DB/SP-Complexity-Evaluator/AGENTS.md`
- [ ] T034 [P] Code cleanup: Remove debug logging, verify error handling in evaluators
- [ ] T035 [P] Verify all tests pass: `./mvnw test` in repository root
- [ ] T036 [P] Run specific evaluator tests to validate dialect-specific behavior: `./mvnw test -Dtest="*Oracle*Procedure*"` and `./mvnw test -Dtest="*Gauss*Procedure*"` and `./mvnw test -Dtest="*Hive*Procedure*"`
- [ ] T037 [P] Quick validation: Evaluate test procedure with procedure calls via API and verify JSON response fields
- [ ] T038 [P] Quick validation: Export to Excel and verify new columns contain correct data
- [ ] T039 [P] Add comments for new fields in `ComplexityMetrics.java` following existing Javadoc pattern
- [ ] T040 [P] Add Javadoc comments for new methods in evaluators (procedure call detection, loop tracking)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Stories 1 & 2 can proceed in parallel (both P1)
  - User Story 3 (P2) can proceed after User Stories 1 & 2 (may integrate)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on US1, but both are P1
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Depends on US1 & US2 for consistency

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD approach)
- No model modifications after evaluator implementations start
- Evaluators before Excel export (US1 before US2)
- Excel export before batch controller (US2 before US3)
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, User Stories 1 & 2 (both P1) can start in parallel (if team capacity allows)
- User Story 3 (P2) can start after US1 & US2 are complete
- All tests for a user story marked [P] can run in parallel

## Parallel Example: User Stories 1 & 2 (P1)

```bash
# Launch all tests for User Story 1 together:
Task: "Contract test: Single procedure evaluation with procedure calls in OracleComplexityEvaluatorTest.java"
Task: "Contract test: Single procedure evaluation with procedure calls in GaussComplexityEvaluatorTest.java"
Task: "Contract test: Single procedure evaluation with function calls in HiveComplexityEvaluatorTest.java"

# Launch all tests for User Story 2 together:
Task: "Contract test: Excel export includes procedure call columns in ComplexityEvaluationControllerTest.java"

# Launch all evaluator implementations together (Oracle, Gauss, Hive):
Task: "Add procedure call regex patterns to OracleComplexityEvaluator.java"
Task: "Add procedure call regex patterns to GaussComplexityEvaluator.java"
Task: "Add function call regex patterns to HiveComplexityEvaluator.java"
```

## Implementation Strategy

### MVP First (User Story 1 + User Story 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (detection in evaluators)
4. **STOP and VALIDATE**: Test User Story 1 independently (single procedure JSON output)
5. Complete Phase 4: User Story 2 (Excel export)
6. **STOP and VALIDATE**: Test User Story 2 independently (Excel export)
7. Deploy/demo if ready (MVP: JSON + Excel outputs working)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Stories 1 & 2 (P1) → Test independently → Deploy/Demo (MVP)
3. Add User Story 3 (P2) → Test independently → Deploy/Demo
4. Each story adds value without breaking previous stories

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests MUST fail before implementing (TDD approach per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Constitution requires JUnit 5 tests - test tasks included for all evaluator changes
- Excel format helper must append "(in loop)" suffix correctly
- Loop depth tracking MUST use context-aware approach (not just keyword counting)
- Procedure names are case-sensitive: track "GET_DATA" and "get_data" as different
- All three evaluators (Oracle, Gauss, Hive) must be updated for consistency
- Batch endpoint has 3 locations where `ComplexityMetrics.builder()` is called - all must be updated
