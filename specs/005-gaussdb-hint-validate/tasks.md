# Implementation Tasks: GaussDB SQL Hint Validation

**Feature**: GaussDB SQL Hint Validation
**Branch**: `005-gaussdb-hint-validate`
**Generated**: 2026-01-16
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Data Model**: [data-model.md](./data-model.md)

## Implementation Strategy

**MVP Scope**: User Story 1 (Validate SQL Hints in Single SQL Statement) + User Story 3 (JSON Output)
- Phase 1: Setup and foundational components
- Phase 2: User Story 1 + User Story 3 (can be delivered together as MVP)
- Phase 3: User Story 2 (Stored Procedure validation)
- Phase 4: User Story 4 (Excel output - P2)
- Phase 5: Batch validation and polish

**Incremental Delivery**: Each user story phase is independently testable. The MVP (Phases 1-2) delivers single SQL validation with JSON output. Subsequent phases build on this foundation.

## Dependency Graph

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational) ───────┐
    │                         │
    ▼                         ▼
Phase 3 (US1+US3) ────────► Phase 4 (US4)
    │                         │
    ▼                         ▼
Phase 5 (US2) ────────────► Polish
```

**Critical Path**: Phase 1 → Phase 2 → Phase 3 → Phase 5 → Polish

## Task Summary

| Phase | Task Count | Description |
|-------|------------|-------------|
| Phase 1: Setup | 4 | Project initialization, model classes |
| Phase 2: Foundational | 5 | Core services, reference loader, utilities |
| Phase 3: User Story 1+3 | 8 | SQL validation + JSON output endpoints |
| Phase 4: User Story 4 | 4 | Excel export service and endpoint |
| Phase 5: User Story 2 | 5 | Stored procedure analysis |
| Phase 6: Batch + Polish | 4 | Batch validation and cross-cutting |
| **Total** | **30** | |

---

## Phase 1: Setup

**Goal**: Initialize project structure and create model classes

**Independent Test Criteria**: All model classes compile and pass basic validation tests

### Tasks

- [X] T001 Create HintValidationResult model in src/main/java/com/sdchat/ce/sp/complexity/model/HintValidationResult.java
- [X] T002 Create HintAnalysisSummary model in src/main/java/com/sdchat/ce/sp/complexity/model/HintAnalysisSummary.java
- [X] T003 Create StoredProcedureAnalysis model in src/main/java/com/sdchat/ce/sp/complexity/model/StoredProcedureAnalysis.java
- [X] T004 Create HintValidationResponse model in src/main/java/com/sdchat/ce/sp/complexity/model/HintValidationResponse.java

---

## Phase 2: Foundational

**Goal**: Create core infrastructure that all user stories depend on

**Independent Test Criteria**: HintReferenceLoader successfully loads gaussdb_sql_plan_hints.json; HintExtractor finds all hints in test SQL; HintSyntaxValidator validates basic syntax

### Tasks

- [X] T005 [P] Create ParseError model in src/main/java/com/sdchat/ce/sp/complexity/model/ParseError.java
- [X] T006 [P] Create StatementHintAnalysis model in src/main/java/com/sdchat/ce/sp/complexity/model/StatementHintAnalysis.java
- [X] T007 Create HintReferenceLoader in src/main/java/com/sdchat/ce/sp/complexity/parser/HintReferenceLoader.java
- [X] T008 Create HintExtractor in src/main/java/com/sdchat/ce/sp/complexity/evaluator/HintExtractor.java
- [X] T009 Create HintSyntaxValidator in src/main/java/com/sdchat/ce/sp/complexity/util/HintSyntaxValidator.java

---

## Phase 3: User Story 1 + User Story 3 (MVP)

**User Story 1 Goal**: Validate hints in single SQL statements and identify valid/invalid hints
**User Story 3 Goal**: Output results in JSON format for programmatic integration

**Independent Test Criteria**: 
- POST /validate/sql returns JSON with correct validation results
- Valid hints recognized: tablescan, indexscan, nestloop, hashjoin, mergejoin
- Invalid hints detected: unknown hint names, syntax errors
- Processing time under 100ms for simple queries

### Implementation Tasks

- [X] T010 [P] Create ValidateSqlRequest DTO in src/main/java/com/sdchat/ce/sp/complexity/model/ValidateSqlRequest.java
- [X] T011 [P] Create ValidateProcedureRequest DTO in src/main/java/com/sdchat/ce/sp/complexity/model/ValidateProcedureRequest.java
- [X] T012 [P] Create BatchValidateRequest DTO in src/main/java/com/sdchat/ce/sp/complexity/model/BatchValidateRequest.java
- [X] T013 [P] Create BatchValidationResponse DTO in src/main/java/com/sdchat/ce/sp/complexity/model/BatchValidationResponse.java
- [X] T014 [US1][US3] Create HintValidationService in src/main/java/com/sdchat/ce/sp/complexity/service/HintValidationService.java
- [X] T015 [US1][US3] Create HintValidationController in src/main/java/com/sdchat/ce/sp/complexity/controller/HintValidationController.java
- [X] T016 [US1][US3] Add POST /validate/sql endpoint to controller
- [X] T017 [US1][US3] Add GET /reference endpoint for hint reference info

### Test Tasks (Integration)

- [X] T018 [US1][US3] Create HintValidationControllerTest in src/test/java/com/sdchat/ce/sp/complexity/controller/HintValidationControllerTest.java
- [X] T019 [US1][US3] Create HintValidationServiceTest in src/test/java/com/sdchat/ce/sp/complexity/service/HintValidationServiceTest.java

---

## Phase 4: User Story 4

**User Story 4 Goal**: Output results in Excel format for human review

**Independent Test Criteria**:
- POST /validate/procedure?format=excel returns valid .xlsx file
- Excel contains Summary sheet with statistics
- Excel contains Hints sheet with detailed records
- Filters work correctly in spreadsheet software

### Implementation Tasks

- [X] T020 [P] Create HintReference DTO in src/main/java/com/sdchat/ce/sp/complexity/model/HintReference.java
- [X] T021 [P] Create HintParameter DTO in src/main/java/com/sdchat/ce/sp/complexity/model/HintParameter.java
- [X] T022 [US4] Create ExcelExportService in src/main/java/com/sdchat/ce/sp/complexity/service/ExcelExportService.java
- [X] T023 [US4] Add Excel export endpoint to HintValidationController

### Test Tasks (Integration)

- [X] T024 [US4] Create ExcelExportServiceTest in src/test/java/com/sdchat/ce/sp/complexity/service/ExcelExportServiceTest.java

---

## Phase 5: User Story 2

**User Story 2 Goal**: Validate hints across all SQL statements in a stored procedure

**Independent Test Criteria**:
- POST /validate/procedure returns storedProcedureAnalysis with all statements analyzed
- Nested statements in IF/ELSE, loops are detected
- Parse errors are collected without failing entire analysis
- Summary shows counts by category and validity

### Implementation Tasks

- [X] T025 [P] Create ValidateProcedureFileRequest DTO in src/main/java/com/sdchat/ce/sp/complexity/model/ValidateProcedureFileRequest.java
- [X] T026 [P] Create BatchItem DTO in src/main/java/com/sdchat/ce/sp/complexity/model/BatchItem.java
- [X] T027 [US2] Extend HintValidationService to support stored procedure parsing in src/main/java/com/sdchat/ce/sp/complexity/service/HintValidationService.java
- [X] T028 [US2] Add POST /validate/procedure endpoint to controller in src/main/java/com/sdchat/ce/sp/complexity/controller/HintValidationController.java
- [X] T029 [US2] Add POST /validate/batch endpoint to controller in src/main/java/com/sdchat/ce/sp/complexity/controller/HintValidationController.java

### Test Tasks (Integration)

- [X] T030 [US2] Create stored procedure validation integration test in src/test/java/com/sdchat/ce/sp/complexity/service/StoredProcedureValidationTest.java

---

## Phase 6: Batch + Polish

**Goal**: Complete batch validation and address cross-cutting concerns

**Polish Goals**:
- Consistent error responses across all endpoints
- Performance optimization for large procedures
- Documentation completeness
- Code quality verification

### Tasks

- [x] T031 [P] Verify all error responses follow ErrorResponse schema (openapi.yaml)
- [x] T032 [P] Add processing time tracking for all validation operations
- [x] T033 Add comprehensive error handling for edge cases (missing JSON, malformed SQL)
- [x] T034 Run full test suite: ./mvnw test
- [x] T035 Verify build: ./mvnw clean package

---

## Parallel Execution Examples

### Within Phase 1 (Setup)
- T001, T002, T003, T004 can run in parallel (independent model classes)

### Within Phase 2 (Foundational)
- T005, T006 can run in parallel with T007, T008, T009 (DTOs with core services)

### Within Phase 3 (US1+US3)
- T010, T011, T012, T013 can run in parallel (DTO creation)
- T014 (service) depends on T007-T009 from Phase 2
- T015 (controller) depends on T014 (service)
- T016, T017 depend on T015 (controller)

### Within Phase 4 (US4)
- T020, T021 can run in parallel (DTOs)
- T022 (Excel service) depends on T020, T021
- T023 depends on T022

---

## Dependencies Summary

| Task | Depends On | Blocks |
|------|------------|--------|
| T001-T004 | None | T007-T009 |
| T005-T006 | None | T014 |
| T007 | T001-T004 | T014 |
| T008 | T001-T004 | T014 |
| T009 | T001-T004 | T014 |
| T010-T013 | T005-T006, T007-T009 | T014 |
| T014 | T007-T009, T010-T013 | T015, T016, T017 |
| T015 | T014 | T016, T017 |
| T016 | T015 | None |
| T017 | T015 | None |
| T018, T019 | T016, T017 | None |
| T020, T021 | None | T022 |
| T022 | T020, T021 | T023 |
| T023 | T022 | T024 |
| T024 | T023 | None |
| T025, T026 | None | T027 |
| T027 | T025, T026, T014 | T028, T029 |
| T028 | T027 | None |
| T029 | T027 | None |
| T030 | T028, T029 | None |
| T031-T033 | All previous | T034 |
| T034 | T031-T033 | T035 |
| T035 | T034 | None |

---

## Verification Checklist

- [ ] All model classes use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- [ ] All service classes use @Slf4j for logging
- [ ] All REST endpoints follow openapi.yaml specification
- [ ] JUnit 5 tests use @SpringBootTest annotation
- [ ] Test method naming follows pattern: methodName_condition_expectedResult
- [ ] Error handling logs warnings for recoverable issues, errors for failures
- [ ] Processing time tracked and included in response
- [ ] gaussdb_sql_plan_hints.json loaded from classpath with graceful fallback
