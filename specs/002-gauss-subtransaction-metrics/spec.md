# Feature Specification: GaussDB Subtransaction Metrics

**Feature Branch**: `002-gauss-subtransaction-metrics`
**Created**: 2026-01-10
**Status**: Draft
**Input**: User description: "开启002号需求，对GaussDB存储过程中的子事务（subtransaction）进行识别并做量化。需在输出excel中保留其明细以备核查。不要破坏当前已识别的其他复杂度评估项。识别子事务包括两类：一类是隐式的，存储过程中A若涉及Insert、Delete、Update的语句即计作有事务，如果存储过程A中又调用了其他的存储过程B，那B中的事务被视作子事务，也算到A的子事务数量中；如果A中有循环，则子事务的数量要乘以循环的次数（通常难以识别具体数量，统一通过设置的参数来作为假设）。另一类是显式的，规则见下面的说明：[GaussDB subtransaction knowledge document]"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Explicit Subtransaction Detection (Priority: P1)

Data analysts and developers need to identify and quantify explicit subtransactions in GaussDB stored procedures. Explicit subtransactions are defined by SAVEPOINT statements that mark subtransaction boundaries within stored procedures. The system must detect SAVEPOINT, ROLLBACK TO SAVEPOINT, and RELEASE SAVEPOINT statements to track subtransaction lifecycle.

**Why this priority**: Explicit subtransactions are clearly defined by SQL syntax and provide foundation for complexity evaluation. They are most common and easily identifiable subtransaction type in GaussDB code.

**Independent Test**: Can be fully tested by evaluating GaussDB stored procedures containing SAVEPOINT statements and verifying that subtransaction count, names, and operations are correctly extracted from source code.

**Acceptance Scenarios**:

1. **Given** a stored procedure with `SAVEPOINT sp1; INSERT INTO table VALUES (1); RELEASE SAVEPOINT sp1;`, **When** evaluating complexity, **Then** subtransaction count is 1 and includes sp1 in the subtransaction list.
2. **Given** a stored procedure with multiple savepoints `SAVEPOINT sp1; SAVEPOINT sp2; ROLLBACK TO SAVEPOINT sp1;`, **When** evaluating complexity, **Then** subtransaction count is 2 and operations are tracked for both sp1 and sp2.
3. **Given** a stored procedure without any SAVEPOINT statements, **When** evaluating complexity, **Then** subtransaction count is 0 and subtransaction list is empty.

---

### User Story 2 - Implicit Subtransaction Detection (Priority: P2)

Data analysts need to identify implicit subtransactions created by DML statements (INSERT, DELETE, UPDATE) and EXCEPTION blocks. These subtransactions do not have explicit SAVEPOINT markers but still represent transaction boundaries within stored procedures. The system must count these statements and EXCEPTION blocks as implicit subtransactions.

**Why this priority**: Implicit subtransactions are common in production code and represent significant complexity that should be measured. Detection requires parsing for DML keywords and EXCEPTION block structures.

**Independent Test**: Can be fully tested by evaluating GaussDB stored procedures with DML statements and EXCEPTION blocks without explicit SAVEPOINT statements, verifying correct subtransaction counting.

**Acceptance Scenarios**:

1. **Given** a stored procedure with `INSERT INTO table VALUES (1); DELETE FROM table WHERE id = 1; UPDATE table SET value = 2;`, **When** evaluating complexity, **Then** implicit subtransaction count is 3 (one per DML statement).
2. **Given** a stored procedure with `BEGIN ... EXCEPTION WHEN OTHERS THEN NULL; END;`, **When** evaluating complexity, **Then** implicit subtransaction count includes the EXCEPTION block (1 implicit subtransaction).
3. **Given** a stored procedure with DML statements inside a loop, **When** evaluating complexity with loop multiplier parameter set to 5, **Then** implicit subtransaction count is (DML count × 5).

---

### User Story 3 - Nested Procedure Subtransaction Aggregation (Priority: P3)

Data analysts need to see aggregated subtransaction counts when stored procedures call other stored procedures. If procedure A calls procedure B, B's transactions should be counted as A's subtransactions to reflect total transaction complexity of the calling procedure.

**Why this priority**: Nested procedure calls are common in complex database schemas. Aggregating subtransactions provides a more accurate representation of total transaction complexity for code review and optimization purposes.

**Independent Test**: Can be fully tested by evaluating a parent procedure that calls a child procedure, verifying that parent's subtransaction count includes both its own and child's subtransactions.

**Acceptance Scenarios**:

1. **Given** procedure A with no DML statements that calls procedure B containing 3 DML statements, **When** evaluating procedure A, **Then** subtransaction count is 3 (aggregated from B).
2. **Given** procedure A with 2 DML statements that calls procedure B containing 2 DML statements, **When** evaluating procedure A, **Then** subtransaction count is 4 (2 from A + 2 from B).
3. **Given** procedure A that calls procedure B which calls procedure C (nested calls), **When** evaluating procedure A, **Then** subtransaction count includes transactions from A, B, and C (full aggregation).

---

### Edge Cases

- What happens when SAVEPOINT statements appear in unsupported contexts (TRIGGER, EXECUTE, CURSOR)?
- How does system handle same-named SAVEPOINT statements (GaussDB retains old savepoints)?
- What happens when subtransaction nesting exceeds 10,000 (performance warning threshold)?
- How does system handle EXCEPTION blocks during subtransaction cleanup (node failure scenarios)?
- What happens when plstmt_implicit_savepoint configuration is enabled vs disabled?
- How does system distinguish between implicit DML transactions and explicit SAVEPOINT transactions?
- What happens when COPY FROM operations cause exceptions (cannot rollback to savepoint)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST detect explicit subtransactions by identifying SAVEPOINT statements in GaussDB stored procedure source code.
- **FR-002**: System MUST identify ROLLBACK TO SAVEPOINT statements and associate them with corresponding SAVEPOINT.
- **FR-003**: System MUST identify RELEASE SAVEPOINT statements and mark subtransaction completion.
- **FR-004**: System MUST detect implicit subtransactions by counting DML statements (INSERT, DELETE, UPDATE) that do not have explicit SAVEPOINT markers.
- **FR-005**: System MUST detect implicit subtransactions in EXCEPTION blocks within BEGIN...EXCEPTION...END structures.
- **FR-006**: System MUST aggregate subtransactions from called procedures when procedure A calls procedure B.
- **FR-007**: System MUST apply loop multiplier to subtransaction counts based on configuration parameter when loops are detected.
- **FR-008**: System MUST export subtransaction details in Excel format including: subtransaction name, type (explicit/implicit), associated DML statements, savepoint operations.
- **FR-009**: System MUST preserve all existing complexity evaluation metrics (tables, joins, loops, cursors, nested procedures, etc.) without any degradation.
- **FR-010**: System MUST track savepoint nesting levels and identify when nesting exceeds 10,000 (generate warning).
- **FR-011**: System MUST handle same-named SAVEPOINT statements per GaussDB behavior (retains old, uses latest for rollback/release).
- **FR-012**: System MUST detect unsupported SAVEPOINT contexts (TRIGGER, EXECUTE, CURSOR, PL/JAVA, PL/PYTHON) and exclude or flag appropriately.

### Error Handling Requirements

- **FR-EH-001**: Subtransaction detection MUST NOT cause evaluation to fail - continue evaluation even if subtransaction parsing encounters errors.
- **FR-EH-002**: Failed subtransaction detections MUST be logged but not block overall complexity evaluation.
- **FR-EH-003**: Exception block subtransaction cleanup failures MUST be handled gracefully and logged with WARN level.

### Key Entities

- **SubtransactionMetric**: Represents a single subtransaction with attributes: name, type (explicit/implicit), dmlStatements, savepointOperations, nestingLevel, sourceProcedure, sourceLine.
- **SubtransactionContext**: Tracks current state of subtransaction detection including: activeSavepoints, implicitDmlCount, loopMultiplier, calledProcedures.
- **LoopMultiplierConfig**: Configuration parameter for loop iteration count when exact loop count cannot be determined (default: 1, meaning no multiplier).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Subtransaction counts are accurate within 5% for test procedures with known subtransaction counts.
- **SC-002**: Excel export contains subtransaction details for 100% of evaluated procedures.
- **SC-003**: Existing complexity metrics (tables, joins, loops, cursors) remain unchanged for all existing test procedures.
- **SC-004**: Nested procedure subtransactions are correctly aggregated in 100% of test cases with procedure calls.
- **SC-005**: Loop multiplier configuration correctly scales subtransaction counts when loops are detected.
- **SC-006**: Evaluation performance degradation is less than 10% when subtransaction detection is enabled (compared to baseline without detection).

### Assumptions

- Loop multiplier parameter will be provided via API request or configuration file when exact loop counts cannot be parsed.
- Subtransaction depth is limited to reasonable nesting levels (< 10,000 for performance).
- Same-named SAVEPOINT behavior follows GaussDB specification (retains old, uses latest for rollback/release).
- Subtransactions are calculated as additional complexity weight factor to existing scoring algorithm.
- Excel export will use existing Apache POI infrastructure with new columns added.

## Constraints and Dependencies

### Constraints

- Subtransaction detection MUST NOT modify existing SQL parsing logic for other dialects (Oracle, Hive).
- Only applies to GaussDB dialect evaluation; other dialects maintain current behavior.
- Excel export format MUST remain backward compatible with existing Excel consumers.
- Loop multiplier is an estimation parameter; exact loop counting is out of scope for this feature.

### Dependencies

- Existing GaussComplexityEvaluator implementation for integration point.
- Existing ComplexityMetrics model for adding subtransaction fields.
- Existing Excel export infrastructure for new columns.
- Existing test infrastructure for GaussDB stored procedures.
