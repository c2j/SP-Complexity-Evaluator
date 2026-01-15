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

- [ ] T001 Verify gaussdb_functions.json exists in src/main/resources/
- [ ] T002 Confirm Maven resources plugin configuration in pom.xml (JSON bundled to JAR)
- [ ] T003 [P] Review existing ComplexityMetrics model structure in src/main/java/com/sdchat/ce/sp/complexity/model/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data models and utility that MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 [P] Create BuiltInFunction model in src/main/java/com/sdchat/ce/sp/complexity/model/BuiltInFunction.java
- [ ] T005 [P] Create FunctionFilterResult model in src/main/java/com/sdchat/ce/sp/complexity/model/FunctionFilterResult.java
- [ ] T006 Create BuiltInFunctionFilter utility in src/main/java/com/sdchat/ce/sp/complexity/util/BuiltInFunctionFilter.java
- [ ] T007 Create BuiltInFunctionConfig configuration in src/main/java/com/sdchat/ce/sp/complexity/config/BuiltInFunctionConfig.java
- [ ] T008 [P] Review existing GaussComplexityEvaluator in src/main/java/com/sdchat/ce/sp/complexity/evaluator/GaussComplexityEvaluator.java

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

- [ ] T011 [US1] Modify ComplexityMetrics in src/main/java/com/sdchat/ce/sp/complexity/model/ComplexityMetrics.java (add filteredFunctions field)
- [ ] T012 [US1] Integrate BuiltInFunctionFilter into GaussComplexityEvaluator for procedure analysis
- [ ] T013 [US1] Ensure procedureCallCount reflects only user-defined functions after filtering

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Filter Built-in Functions from Table Analysis (Priority: P1)

**Goal**: Table complexity analysis excludes built-in functions from computed columns and expressions

**Independent Test**: Create a table with computed columns using built-in functions (hash_array, buckettext), verify filteredFunctions field contains them and table complexity metrics are accurate

### Tests for User Story 2

- [ ] T014 [P] [US2] Add test for table computed column filtering in GaussComplexityEvaluatorTest.java

### Implementation for User Story 2

- [ ] T015 [US2] Extend GaussComplexityEvaluator to filter built-in functions from table expressions
- [ ] T016 [US2] Handle computed columns, default values, and check constraints with built-in functions

**Checkpoint**: User Stories 1 AND 2 should work independently

---

## Phase 5: User Story 3 - Transparency Through Filtered Functions Report (Priority: P2)

**Goal**: Analysis results include complete list of filtered functions with category information

**Independent Test**: Analyze code with known built-in functions, verify response includes filteredFunctions with categoryBreakdown and filteredFunctions list

### Tests for User Story 3

- [ ] T017 [P] [US3] Add test for filter result reporting in GaussComplexityEvaluatorTest.java

### Implementation for User Story 3

- [ ] T018 [US3] Ensure FunctionFilterResult includes complete category breakdown
- [ ] T019 [US3] Verify filteredFunctions list includes name and category for each filtered function
- [ ] T020 [US3] Test batch analysis includes filteredFunctions report per result

**Checkpoint**: All user stories should be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T021 [P] Run full test suite: ./mvnw test -Dtest=GaussComplexityEvaluatorTest
- [ ] T022 [P] Verify JAR packaging: ./mvnw clean package && jar -tf target/*.jar | grep gaussdb_functions.json
- [ ] T023 [P] Add logging for filtering operations using @Slf4j
- [ ] T024 Run quickstart.md validation: build, run, test filtering
- [ ] T025 Update AGENTS.md with new feature context if needed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories - **MVP**
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) SHOULD be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- Phase 1 tasks T001-T003 can run in parallel
- Phase 2 tasks T004-T005, T008 can run in parallel
- Once Foundational phase completes, all user stories can start in parallel
- Tests for each user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T009: "Add unit test for BuiltInFunctionFilter"
Task T010: "Add test for procedure filtering in GaussComplexityEvaluatorTest"

# Launch model creation together:
Task T004: "Create BuiltInFunction model"
Task T005: "Create FunctionFilterResult model"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Task Summary

| Category | Count |
|----------|-------|
| Total Tasks | 25 |
| Setup (Phase 1) | 3 |
| Foundational (Phase 2) | 5 |
| User Story 1 (Phase 3) | 5 |
| User Story 2 (Phase 4) | 2 |
| User Story 3 (Phase 5) | 4 |
| Polish (Phase 6) | 5 |
| Parallelizable [P] tasks | 14 |
| Story-specific tasks | 11 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
