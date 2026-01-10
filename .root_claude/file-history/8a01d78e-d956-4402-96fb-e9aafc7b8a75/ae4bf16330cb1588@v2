---

description: "Task list template for feature implementation"
---

# Tasks: Complexity Weights UI Enhancement

**Input**: Design documents from `/specs/001-complexity-weights-ui/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  - Do NOT keep these sample tasks in generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize Java 17 project with Spring Boot 3.4.4 dependencies
- [ ] T003 [P] Configure linting and formatting tools for Java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Setup existing complexity evaluation models with weight configuration support
- [ ] T005 [P] Implement weight configuration management service
- [ ] T006 [P] Setup API routing and middleware structure for weight endpoints
- [ ] T007 Create base weight configuration models that all stories depend on
- [ ] T008 Configure error handling and validation infrastructure for weight inputs
- [ ] T009 Setup session management for weight configurations

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - View Complexity Formulas and Weights (Priority: P1) 🎯 MVP

**Goal**: Display complexity evaluation formulas and current weight factors to users

**Independent Test**: Can access web interface and verify that all complexity factors, their current weights, and calculation formula are displayed correctly for each database dialect

### Tests for User Story 1 (OPTIONAL - only if tests requested) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Contract test for GET /api/weights/formulas in tests/contract/test_weights_api.java
- [ ] T011 [P] [US1] Integration test for formula display in tests/integration/test_formula_display.java
- [ ] T012 [P] [US1] Unit test for weight configuration retrieval in tests/unit/test_weight_config.java

### Implementation for User Story 1

- [ ] T013 [P] [US1] Create WeightConfiguration model in src/main/java/com/evaluator/model/WeightConfiguration.java
- [ ] T014 [P] [US1] Create SqlWeights and ProcedureWeights models in src/main/java/com/evaluator/model/SqlWeights.java
- [ ] T015 [P] [US1] Implement WeightService in src/main/java/com/evaluator/service/WeightService.java (depends on T013, T014)
- [ ] T016 [US1] Implement GET /api/weights/formulas endpoint in src/main/java/com/evaluator/controller/WeightController.java
- [ ] T017 [US1] Create formula display template section in src/main/resources/templates/fragments/formula-display.html
- [ ] T018 [US1] Add weight configuration display to main evaluation template in src/main/resources/templates/index.html
- [ ] T019 [US1] Add validation and error handling for formula display
- [ ] T020 [US1] Add logging for weight formula access operations

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Modify Calculation Weights (Priority: P1) 🎯 MVP

**Goal**: Allow users to interactively modify weight factors and see real-time complexity score updates

**Independent Test**: Can modify weights, run evaluations with original and modified weights, and verify that results change according to weight adjustments

### Tests for User Story 2 (OPTIONAL - only if tests requested) ⚠️

- [ ] T021 [P] [US2] Contract test for PUT /api/weights in tests/contract/test_weight_update.java
- [ ] T022 [P] [US2] Integration test for weight modification workflow in tests/integration/test_weight_modification.java
- [ ] T023 [P] [US2] Unit test for weight validation in tests/unit/test_weight_validation.java

### Implementation for User Story 2

- [ ] T024 [P] [US2] Create WeightUpdateRequest model in src/main/java/com/evaluator/model/WeightUpdateRequest.java
- [ ] T025 [P] [US2] Implement weight validation logic in WeightService (depends on T015)
- [ ] T026 [US2] Implement PUT /api/weights endpoint in WeightController (depends on T015)
- [ ] T027 [P] [US2] Create weight modification UI components in src/main/resources/templates/fragments/weight-controls.html
- [ ] T028 [US2] Add JavaScript for real-time weight updates in src/main/resources/static/js/weight-manager.js
- [ ] T029 [US2] Implement weight reset functionality in WeightService
- [ ] T030 [US2] Add weight modification form to main evaluation template in index.html
- [ ] T031 [US2] Integrate weight modifications with existing evaluation endpoints
- [ ] T032 [US2] Add session management for weight configurations

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Export Weight Information to Excel (Priority: P2)

**Goal**: Include weight configuration information in Excel exports for documentation and team consistency

**Independent Test**: Can perform evaluations with custom weights, export to Excel, and verify that weight information appears correctly in the output file

### Tests for User Story 3 (OPTIONAL - only if tests requested) ⚠️

- [ ] T033 [P] [US3] Contract test for Excel export with weights in tests/contract/test_excel_export.java
- [ ] T034 [P] [US3] Integration test for weight information in Excel export in tests/integration/test_excel_weights.java

### Implementation for User Story 3

- [ ] T035 [P] [US3] Extend ExcelExportService to include weight configuration worksheet
- [ ] T036 [US3] Create WeightConfigurationExcelWriter in src/main/java/com/evaluator/excel/WeightConfigurationExcelWriter.java
- [ ] T037 [US3] Modify existing evaluation endpoints to pass weight configuration to export service
- [ ] T038 [US3] Add weight information to batch evaluation Excel exports
- [ ] T039 [US3] Test Excel export with various weight configurations

---

## Phase 6: User Story 4 - Compare Results with Different Weights (Priority: P3)

**Goal**: Provide comparison functionality to analyze how different weight configurations impact complexity scores

**Independent Test**: Can run same SQL with different weight configurations and verify that comparison view correctly shows the differences

### Tests for User Story 4 (OPTIONAL - only if tests requested) ⚠️

- [ ] T040 [P] [US4] Contract test for weight template management in tests/contract/test_weight_templates.java
- [ ] T041 [P] [US4] Integration test for comparison functionality in tests/integration/test_weight_comparison.java

### Implementation for User Story 4

- [ ] T042 [P] [US4] Create WeightTemplate model in src/main/java/com/evaluator/model/WeightTemplate.java
- [ ] T043 [P] [US4] Implement weight template CRUD operations in WeightService
- [ ] T044 [P] [US4] Implement GET /api/weights/templates endpoint in WeightController
- [ ] T045 [P] [US4] Implement POST /api/weights/templates endpoint in WeightController
- [ ] T046 [P] [US4] Implement GET /api/weights/templates/{id} endpoint in WeightController
- [ ] T047 [P] [US4] Implement DELETE /api/weights/templates/{id} endpoint in WeightController
- [ ] T048 [US4] Create comparison UI components in templates/fragments/comparison-display.html
- [ ] T049 [US4] Add JavaScript for comparison functionality in static/js/comparison-manager.js
- [ ] T050 [US4] Implement comparison logic in service layer

**Checkpoint**: All user stories should now be independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T051 [P] Documentation updates in docs/
- [ ] T052 Code cleanup and refactoring for weight management
- [ ] T053 Performance optimization across all stories (weight updates <200ms)
- [ ] T054 [P] Additional unit tests for weight management in tests/unit/
- [ ] T055 Security hardening for weight input validation
- [ ] T056 Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Depends on US1 for base models
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Depends on US1 and US2 for evaluation integration
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - Depends on US1, US2, US3 for full workflow

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before controllers
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for GET /api/weights/formulas in tests/contract/test_weights_api.java"
Task: "Integration test for formula display in tests/integration/test_formula_display.java"
Task: "Unit test for weight configuration retrieval in tests/unit/test_weight_config.java"

# Launch all models for User Story 1 together:
Task: "Create WeightConfiguration model in src/main/java/com/evaluator/model/WeightConfiguration.java"
Task: "Create SqlWeights and ProcedureWeights models in src/main/java/com/evaluator/model/SqlWeights.java"
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
5. Add User Story 4 → Test independently → Deploy/Demo
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3 & 4 (sequential)
3. Stories complete and integrate independently
4. Regular integration testing to ensure cross-story compatibility

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence