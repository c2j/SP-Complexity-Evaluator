# Research: GaussDB Subtransaction Metrics

**Feature**: [spec.md](spec.md)
**Date**: 2026-01-10

## Summary

No research tasks required. Feature specification is complete with all requirements clearly defined and no [NEEDS CLARIFICATION] markers. Technical context uses existing Java 17 + Spring Boot stack. All dependencies (Spring Boot, Lombok, JUnit, Apache POI) are already established in the project.

## Research Tasks

### Task 1: Research Subtransaction Detection Patterns

**Status**: ✅ Complete - No research needed

**Rationale**: Feature specification provides comprehensive GaussDB subtransaction knowledge document including:
- Explicit subtransaction syntax (SAVEPOINT, ROLLBACK TO, RELEASE)
- Implicit subtransaction patterns (DML statements, EXCEPTION blocks)
- Nested procedure subtransaction aggregation rules
- Same-named SAVEPOINT handling behavior
- Unsupported contexts (TRIGGER, EXECUTE, CURSOR, PL/JAVA, PL/PYTHON)

### Task 2: Research Loop Multiplier Configuration

**Status**: ✅ Complete - No research needed

**Rationale**: Specification clearly defines loop multiplier as a configuration parameter provided via API or configuration file. Exact loop counting is explicitly out of scope.

### Task 3: Research Excel Export Integration

**Status**: ✅ Complete - No research needed

**Rationale**: Specification requires adding subtransaction columns to existing Excel export format. Apache POI 5.2.3 is already in project and Excel export utility (ExcelExportUtil) exists.

## Dependencies

No additional dependencies required. All technologies are already in the project:
- Java 17
- Spring Boot 3.2.6
- Lombok 1.18.32
- JUnit 5
- Apache POI 5.2.3

## Best Practices Considered

1. **Dialect Isolation**: Subtransaction detection applies ONLY to GaussDB dialect - Oracle and Hive evaluators remain unchanged
2. **Backward Compatibility**: Excel export must preserve existing columns while adding new subtransaction columns
3. **Error Handling**: Subtransaction detection errors must not break evaluation (follow Error Resilience principle)
4. **Performance**: Subtransaction depth must be limited (< 10,000) to prevent performance degradation

## Decisions

### Decision 1: Subtransaction Model Approach

**Choice**: Create separate SubtransactionMetric model instead of embedding in ComplexityMetrics

**Rationale**:
- Separation of concerns - subtransaction details are separate from complexity score
- Existing ComplexityMetrics model is already large (~327 lines)
- SubtransactionMetric can be serialized as JSON string in Excel export
- Cleaner code organization and maintenance

**Alternatives Considered**:
- Add fields directly to ComplexityMetrics - Rejected due to model size
- Use nested inner class in ComplexityMetrics - Rejected to avoid complexity in existing evaluator

### Decision 2: Subtransaction Type Enumeration

**Choice**: Use Java enum (EXPLICIT, IMPLICIT) instead of string type

**Rationale**:
- Type safety at compile time
- Better IDE support for code completion
- Clearer semantic meaning

**Alternatives Considered**:
- String type with constants - Rejected for type safety
- Integer type (0=implicit, 1=explicit) - Rejected for readability

## Open Questions

None - all requirements are clear and implementation approach is defined.

## Conclusion

Research phase complete. Proceeding to Phase 1: Design & Contracts.
