# Feature Specification: GaussDB Transaction and Subtransaction Statistics Optimization

**Feature Branch**: `006-gaussdb-transaction-stats`
**Created**: 2026-01-17
**Status**: Draft
**Input**: User description: "现在开启006号需求项，进行事务和子事务统计的优化。根据GaussDB的文档，存储过程中事务、子事务或嵌套事务数量需要进一步优化，请参考如下说明对当前subtransaction的统计算法进行检查并优化"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Accurate Transaction Counting (Priority: P1)

As a database developer, I want the system to correctly count all transaction control statements (BEGIN, START TRANSACTION, COMMIT, ROLLBACK) in stored procedures, so that I can understand the transaction complexity of my code.

**Why this priority**: Transaction counting is the foundation of the entire feature. Without accurate transaction counts, all subsequent metrics and complexity calculations will be incorrect. This directly impacts the reliability of the complexity evaluation system.

**Independent Test**: Can be fully tested by submitting a stored procedure containing various transaction control statements and verifying the counts match expected values based on GaussDB transaction semantics.

**Acceptance Scenarios**:

1. **Given** a stored procedure with multiple BEGIN statements, **When** the system evaluates it, **Then** it MUST return the correct count of main transactions
2. **Given** a stored procedure with START TRANSACTION statements, **When** the system evaluates it, **Then** it MUST count them as transactions alongside BEGIN statements
3. **Given** a stored procedure with COMMIT and ROLLBACK statements, **When** the system evaluates it, **Then** it MUST correctly associate them with their respective transactions
4. **Given** a stored procedure without explicit transaction control, **When** the system evaluates it, **Then** it MUST report zero transactions

---

### User Story 2 - Accurate Subtransaction and Savepoint Counting (Priority: P1)

As a database developer, I want the system to correctly count all savepoint operations (SAVEPOINT, ROLLBACK TO SAVEPOINT, RELEASE SAVEPOINT) in stored procedures, so that I can understand the nested transaction complexity of my code.

**Why this priority**: Savepoints represent subtransactions within a main transaction. Incorrectly counting savepoints leads to misleading complexity metrics, especially for stored procedures that use savepoints for error handling and partial rollbacks.

**Independent Test**: Can be fully tested by submitting a stored procedure with various savepoint patterns and verifying the subtransaction counts match expected values.

**Acceptance Scenarios**:

1. **Given** a stored procedure with SAVEPOINT statements, **When** the system evaluates it, **Then** it MUST count each SAVEPOINT as a subtransaction
2. **Given** a stored procedure with ROLLBACK TO SAVEPOINT statements, **When** the system evaluates it, **Then** it MUST recognize them as subtransaction control operations
3. **Given** a stored procedure with RELEASE SAVEPOINT statements, **When** the system evaluates it, **Then** it MUST recognize them as subtransaction completion operations
4. **Given** a stored procedure with multiple savepoints of the same name, **When** the system evaluates it, **Then** it MUST handle them according to GaussDB semantics (only the most recent is active)

---

### User Story 3 - Nested Transaction Structure Analysis (Priority: P2)

As a database developer, I want the system to understand the hierarchical structure of nested transactions and savepoints within a single stored procedure, so that I can identify overly complex transaction patterns in my code.

**Why this priority**: Nested transactions increase complexity and can lead to performance issues. Understanding the nesting depth helps developers identify potential problems before they impact production systems.

**Scope Note**: This story covers nesting within a single stored procedure (e.g., savepoints inside transactions). Cross-procedure calls are evaluated independently - Procedure A's statistics count only its own transactions, not those in called Procedure B.

**Independent Test**: Can be fully tested by submitting stored procedures with various levels of nesting and verifying the system correctly identifies the hierarchy and depth.

**Acceptance Scenarios**:

1. **Given** a stored procedure with savepoints inside transactions, **When** the system evaluates it, **Then** it MUST correctly identify the parent-child relationship between transactions and savepoints
2. **Given** a stored procedure with multiple levels of savepoints, **When** the system evaluates it, **Then** it MUST calculate the maximum nesting depth
3. **Given** a stored procedure with transactions that contain other transactions (if supported), **When** the system evaluates it, **Then** it MUST handle the nested structure according to GaussDB semantics
4. **Given** a stored procedure with transactions and savepoints, **When** the system evaluates it, **Then** it MUST provide separate counts for main transactions and subtransactions

---

### User Story 4 - Transaction Complexity Metrics (Priority: P2)

As a database developer or team lead, I want the system to provide meaningful transaction complexity metrics, so that I can make informed decisions about code quality and refactoring priorities.

**Why this priority**: Raw counts alone are not sufficient. Developers need derived metrics that indicate whether the transaction structure is too complex and requires attention.

**Independent Test**: Can be fully tested by comparing the generated complexity scores against manually calculated expected values for a set of representative stored procedures.

**Acceptance Scenarios**:

1. **Given** a stored procedure with simple transaction structure (one transaction, no savepoints), **When** the system evaluates it, **Then** it MUST assign a low transaction complexity score
2. **Given** a stored procedure with complex transaction structure (multiple transactions, many savepoints, deep nesting), **When** the system evaluates it, **Then** it MUST assign a proportionally higher complexity score
3. **Given** a stored procedure evaluated multiple times, **When** the transaction counting algorithm changes, **Then** it MUST be able to compare results and identify discrepancies
4. **Given** two stored procedures with different transaction patterns but similar raw counts, **When** the system evaluates them, **Then** it MUST distinguish based on structural complexity

---

### User Story 5 - Integration with Existing Evaluation (Priority: P3)

As a user of the complexity evaluation system, I want the transaction and subtransaction metrics to be seamlessly integrated with existing complexity metrics, so that I get a complete picture of stored procedure complexity without additional effort.

**Why this priority**: This is a quality-of-life improvement that ensures the new metrics fit into the existing workflow rather than requiring users to learn new interfaces or processes.

**Independent Test**: Can be fully tested by verifying that transaction metrics appear in the standard evaluation output alongside other complexity indicators.

**Acceptance Scenarios**:

1. **Given** a stored procedure evaluation request, **When** the system completes evaluation, **Then** it MUST include transaction and subtransaction metrics in the standard response format
2. **Given** an evaluation that previously returned complexity metrics, **When** transaction metrics are added, **Then** existing fields MUST remain unchanged to maintain backward compatibility
3. **Given** a batch evaluation request, **When** processing multiple stored procedures, **Then** each procedure MUST include transaction metrics in its individual result
4. **Given** a user exporting results to JSON or Excel, **When** the export includes complexity data, **Then** transaction metrics MUST be included in the export

---

### Edge Cases

- What happens when a stored procedure has unbalanced transactions (more BEGIN than COMMIT)?
- How does system handle comments and strings that contain transaction keywords (e.g., a string containing "BEGIN")?
- What happens when transaction control statements appear inside stored procedure definitions within the procedure being evaluated?
- How does system handle case sensitivity in transaction keywords (BEGIN vs begin)?
- What happens when savepoint operations reference undefined savepoints?
- How does system handle extremely deeply nested transactions (performance and stack overflow concerns)?
- What happens when transaction control statements appear in dynamic SQL or EXECUTE statements?
- How does system handle transaction isolation level settings (SET TRANSACTION, SET LOCAL TRANSACTION)?
- What happens when there are multiple commit/rollback statements in sequence?
- How does system distinguish between transaction BEGIN and anonymous block BEGIN in GaussDB?
- What happens when stored procedures have mutual or circular dependencies (A calls B, B calls A)?
- How does system handle procedure calls with dynamic names (e.g., EXECUTE 'proc_' || suffix)?
- What happens when the called procedure does not exist in the provided code?

## Requirements *(mandatory)*

<!--
  NOTE: For SP-Complexity-Evaluator, requirements typically involve:
  - SQL dialect support (Oracle, Gauss, Hive, or new dialects)
  - Complexity evaluation features (metrics, scoring algorithms)
  - API functionality (REST endpoints, response formats)
  - Error handling (failed statement collection, exception tracking)
-->

### Functional Requirements

- **FR-001**: System MUST correctly identify and count all transaction initiation statements: BEGIN [WORK | TRANSACTION], START TRANSACTION
- **FR-002**: System MUST correctly identify and count all transaction completion statements: COMMIT [WORK | TRANSACTION], END [WORK | TRANSACTION], ROLLBACK [WORK | TRANSACTION]
- **FR-003**: System MUST correctly identify and count all savepoint statements: SAVEPOINT, ROLLBACK TO SAVEPOINT, RELEASE SAVEPOINT
- **FR-004**: System MUST distinguish between transaction-level BEGIN and anonymous block BEGIN according to GaussDB syntax rules
- **FR-005**: System MUST track the hierarchical relationship between main transactions and savepoints
- **FR-006**: System MUST calculate transaction nesting depth based on the structure of savepoints within transactions
- **FR-007**: System MUST report separate counts for: main transactions, subtransactions (savepoints), commits, rollbacks
- **FR-008**: System MUST provide a transaction complexity score derived from transaction count, savepoint count, and nesting depth
- **FR-009**: System MUST handle case-insensitive matching of transaction keywords (BEGIN, begin, Begin all treated as transaction start)
- **FR-010**: System MUST ignore transaction keywords that appear within string literals or comments
- **FR-011**: System MUST handle multiple savepoints with the same name according to GaussDB semantics (most recent takes precedence)
- **FR-012**: System MUST handle transaction isolation level settings (SET TRANSACTION, SET LOCAL TRANSACTION) appropriately
- **FR-013**: System MUST scope transaction statistics to the individual stored procedure being evaluated (do not recursively aggregate statistics from called procedures)
- **FR-014**: System MUST handle detection of stored procedure call statements (CALL, EXECUTE, or equivalent) without including called procedures' transaction counts in the current procedure's statistics

### Error Handling Requirements

- **FR-EH-001**: Evaluation process MUST continue even when transaction parsing encounters ambiguous syntax
- **FR-EH-002**: Unbalanced transaction control statements (unmatched BEGIN/COMMIT) MUST be flagged in the evaluation results
- **FR-EH-003**: Invalid savepoint references (ROLLBACK TO undefined_savepoint) MUST be flagged in the evaluation results
- **FR-EH-004**: All parsing exceptions MUST be logged with appropriate detail level without failing the entire evaluation
- **FR-EH-005**: When transaction counting cannot be completed due to parsing errors, the system MUST report partial results with an indication of the issue

### Key Entities

- **TransactionMetrics**: Represents the transaction-related complexity metrics for a stored procedure, including counts of transactions, savepoints, commits, rollbacks, and nesting depth
- **TransactionStructure**: Represents the hierarchical structure of transactions and savepoints within a stored procedure, capturing parent-child relationships
- **SavepointInfo**: Represents individual savepoint metadata including name, line number, and associated transaction context

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Transaction counting accuracy reaches 100% for standard transaction patterns (single transactions, simple savepoints)
- **SC-002**: Subtransaction counting accuracy reaches 100% for savepoint patterns (SAVEPOINT, ROLLBACK TO SAVEPOINT, RELEASE SAVEPOINT)
- **SC-003**: Transaction complexity metrics correctly distinguish between simple (low complexity) and complex (high complexity) transaction patterns
- **SC-004**: Transaction and subtransaction metrics are included in all evaluation results alongside existing complexity metrics
- **SC-005**: Evaluation of stored procedures with transaction complexity completes within the same time bounds as evaluations without transaction metrics (no performance degradation)
- **SC-006**: System correctly handles at least 95% of edge cases (unusual transaction patterns, nested structures, etc.) without requiring manual intervention
- **SC-007**: Transaction complexity score correlates with manual expert assessment of transaction complexity (correlation coefficient > 0.8)

## Assumptions

- The system already has SQL parsing capabilities that can be extended to recognize transaction control statements
- GaussDB transaction semantics follow the standard patterns described in the provided documentation
- The evaluation system can be extended to include new metrics without breaking existing functionality
- Stored procedures are well-formed PL/SQL or similar procedural SQL code
- The complexity evaluation is primarily used for static analysis (code review, quality assessment) rather than runtime behavior
- Transaction statistics are scoped to the individual stored procedure being evaluated (not recursively aggregated from called procedures)
- When Procedure A calls Procedure B, A's transaction statistics count only transactions within A itself, not those in B

## Clarifications

### Session 2026-01-17

- Q: When Procedure A calls Procedure B, should A's transaction statistics include B's transaction statistics (recursive aggregation)? → A: Only count transactions within the current procedure (no recursive aggregation)

## Dependencies

- Existing SQL parsing infrastructure (tokenization, statement recognition)
- Existing complexity metrics calculation framework
- REST API endpoints for evaluation (to include new metrics in responses)
- Export functionality (JSON, Excel) to include new metrics

## Out of Scope

- Runtime transaction monitoring or dynamic analysis of executing stored procedures
- Validation of transaction correctness (e.g., whether a transaction will succeed)
- Performance benchmarking of transactions at runtime
- Integration with database transaction logs or monitoring systems
- Automatic suggestion of transaction structure improvements (may be a future enhancement)
