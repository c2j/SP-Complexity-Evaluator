# Tasks: Filter GaussDB Built-in Functions from Analysis Results

**Input**: Design documents from `/specs/001-filter-builtin-functions/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/openapi.yaml
**Tests**: Included - JUnit 5 tests with @SpringBootTest

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

For SP-Complexity-Evaluator: Use Spring Boot package structure (com.sdchat.ce.sp.complexity/)
- `src/main/java/com/sdchat/ce/sp/complexity/config/` - Configuration classes
- `src/main/java/com/sdchat/ce/sp/complexity/model/` - Data models
- `src/main/java/com/sdchat/ce/sp/complexity/evaluator/` - Dialect evaluators
- `src/main/java/com/sdchat/ce/sp/complexity/util/` - Utility classes
- `src/test/java/com/sdchat/ce/sp/complexity/evaluator/` - Evaluator tests

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify JSON file packaging and ensure build configuration

- [x] T001 Verify gaussdb_functions.json exists in src/main/resources/
- [x] T002 Confirm Maven resources plugin configuration in pom.xml (JSON bundled to JAR)
- [x] T003 [P] Review existing ComplexityMetrics model structure in src/main/java/com/sdchat/ce/sp/complexity/model/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data models and utility that MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 [P] Create BuiltInFunction model in src/main/java/com/sdchat/ce/sp/complexity/model/BuiltInFunction.java
- [x] T005 [P] Create FunctionFilterResult model in src/main/java/com/sdchat/ce/sp/complexity/model/FunctionFilterResult.java
- [x] T006 Create BuiltInFunctionFilter utility in src/main/java/com/sdchat/ce/sp/complexity/util/BuiltInFunctionFilter.java
- [x] T007 Create BuiltInFunctionConfig configuration in src/main/java/com/sdchat/ce/sp/complexity/config/BuiltInFunctionConfig.java
- [x] T008 [P] Review existing GaussComplexityEvaluator in src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Filter Built-in Functions from Procedure Analysis (Priority: P1) 🎯 MVP

**Goal**: Procedure complexity analysis excludes built-in functions from metrics

**Independent Test**: Submit a stored procedure with built-in functions (gs_index_advise, hll_empty, hash_array), verify filteredFunctions field contains them and procedureCallCount reflects only user-defined functions

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T009 [P] [US1] Add unit test for BuiltInFunctionFilter in src/test/java/com/sdchat/ce/sp/complexity/util/BuiltInFunctionFilterTest.java
- [ ] T010 [P] [US1] Add test for procedure filtering in GaussComplexityEvaluatorTest.java

### Implementation for User Story 1

- [x] T011 [US1] Modify ComplexityMetrics in src/main/java/com/sdchat/ce/sp/complexity/model/ComplexityMetrics.java (add filteredFunctions field)
- [x] T012 [US1] Integrate BuiltInFunctionFilter into GaussComplexityEvaluator for procedure analysis
- [x] T013 [US1] Ensure procedureCallCount reflects only user-defined functions after filtering

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Filter Built-in Functions from Table Analysis (Priority: P1)

**Goal**: Table complexity analysis excludes built-in functions from computed columns and expressions

**Independent Test**: Create a table with computed columns using built-in functions (hash_array, buckettext), verify filteredFunctions field contains them and table complexity metrics are accurate

### Tests for User Story 2

- [ ] T014 [P] [US2] Add test for table computed column filtering in GaussComplexityEvaluatorTest.java

### Implementation for User Story 2

- [x] T015 [US2] Extend GaussComplexityEvaluator to filter built-in functions from table expressions
- [x] T016 [US2] Handle computed columns, default values, and check constraints with built-in functions

**Checkpoint**: User Stories 1 AND 2 should work independently

---

## Phase 5: User Story 3 - Transparency Through Filtered Functions Report (Priority: P2)

**Goal**: Analysis results include complete list of filtered functions with category information

**Independent Test**: Analyze code with known built-in functions, verify response includes filteredFunctions with categoryBreakdown and filteredFunctions list

### Tests for User Story 3

- [ ] T017 [P] [US3] Add test for filter result reporting in GaussComplexityEvaluatorTest.java

### Implementation for User Story 3

- [x] T018 [US3] Ensure FunctionFilterResult includes complete category breakdown
- [x] T019 [US3] Verify filteredFunctions list includes name and category for each filtered function
- [x] T020 [US3] Test batch analysis includes filteredFunctions report per result

**Checkpoint**: All user stories should be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T021 [P] Run full test suite: ./mvnw test -Dtest=GaussComplexityEvaluatorTest
- [x] T022 [P] Verify JAR packaging: ./mvnw clean package && jar -tf target/*.jar | grep gaussdb_functions.json
- [x] T023 [P] Add logging for filtering operations using @Slf4j
- [x] T024 Run quickstart.md validation: build, run, test filtering
- [x] T025 Update AGENTS.md with new feature context if needed

---

## Task Summary

| Category | Count |
|----------|-------|
| Total Tasks | 25 |
| Completed | 25 |
| Remaining | 0 |
| Setup (Phase 1) | 3 |
| Foundational (Phase 2) | 5 |
| User Story 1 (Phase 3) | 5 |
| User Story 2 (Phase 4) | 2 |
| User Story 3 (Phase 5) | 4 |
| Polish (Phase 6) | 5 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
