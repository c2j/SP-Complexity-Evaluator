# Implementation Plan: Procedure Call Details in Output Formats

**Branch**: `001-procedure-call-details` | **Date**: 2025-01-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-procedure-call-details/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for execution workflow.

## Summary

Add procedure call tracking and details to stored procedure complexity evaluation outputs. Feature requires:
- Detecting procedure calls across Oracle, Gauss, and Hive SQL dialects
- Tracking whether calls occur within loop constructs (FOR, WHILE, LOOP)
- Outputting `procedureCallCount` (integer) and `procedureCallDetails` (array) in JSON
- Adding "Procedure Call Count" and "Procedure Call Details" columns to Excel exports
- Maintaining consistency across all evaluation endpoints (single, batch, package)

**Technical approach**: Regex-based pattern matching for procedure call detection, context-aware depth tracking for loop detection, new `ProcedureCallMetric` model class, and updates to existing `ComplexityMetrics` and `ExcelExportUtil`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.6, Lombok 1.18.32, Apache POI 5.2.3
**Storage**: N/A (stateless REST API, no persistence)
**Testing**: JUnit 5 with @SpringBootTest
**Target Platform**: Linux server (Java application server)
**Project Type**: web (Spring Boot REST API)
**Performance Goals**: Support batch processing of ZIP files with hundreds of SQL files without degradation
**Constraints**: File uploads limited to 10MB, must support Oracle, Gauss, and Hive dialects, procedure call detection must not break existing error handling
**Scale/Scope**: All existing evaluation endpoints (single, batch, package, file upload)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Pre-Phase 0 Status**: ✅ All gates passed - Feature extends existing evaluators following interface-based pattern

**Post-Phase 1 Re-check**:
- [x] SQL Dialect Extensibility: Extends existing evaluator interfaces, no new dialects added
- [x] Test Coverage: Tests follow JUnit 5 + @SpringBootTest pattern (quickstart.md)
- [x] Error Resilience: Procedure call errors logged at DEBUG level, evaluation continues
- [x] REST API First: Extends existing endpoints with additive fields only
- [x] Logging: @Slf4j used with DEBUG logging for procedure call detection (quickstart.md)

**Final Status**: ✅ All gates confirmed - No violations, no justifications needed

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Current Spring Boot application structure
src/main/java/com/sdchat/ce/sp/complexity/
├── Application.java                    # Main Spring Boot application
├── config/                             # Configuration classes
├── controller/                         # REST controllers
├── evaluator/                          # Complexity evaluators (Oracle, Gauss, Hive)
│   ├── OracleComplexityEvaluator.java      # [MODIFY] Add procedure call detection
│   ├── GaussComplexityEvaluator.java       # [MODIFY] Add procedure call detection
│   └── HiveComplexityEvaluator.java        # [MODIFY] Add function call detection
├── model/                              # Data models (DTOs, entities)
│   ├── ComplexiyMetrics.java              # [MODIFY] Add procedureCallCount and procedureCallDetails fields
│   └── ProcedureCallMetric.java        # [CREATE] New model for procedure call details
├── parser/                             # SQL parsers
├── service/                             # Business logic services
└── util/                                # Utility classes
    └── ExcelExportUtil.java             # [MODIFY] Add "Procedure Call Count" and "Procedure Call Details" columns
```

**Structure Decision**: Single Spring Boot application with interface-based evaluator pattern. Feature extends existing evaluators without architectural changes.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
