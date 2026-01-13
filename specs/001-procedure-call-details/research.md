# Research: Procedure Call Detection for SP-Complexity-Evaluator

**Feature**: Add procedure call tracking to complexity evaluation outputs
**Date**: 2025-01-13
**Phase**: Phase 0 - Outline & Research

## Research Tasks

### Task 1: Oracle Procedure Call Detection Patterns

**Objective**: Identify patterns for detecting direct procedure calls in Oracle stored procedures.

**Findings**:
- **Direct invocation pattern**: `procedure_name(param_list)` or `schema.procedure_name(param_list)`
- **EXECUTE pattern**: `EXECUTE procedure_name` or `EXEC schema.procedure_name`
- **Call statements**: `CALL procedure_name` (less common in Oracle)
- **Context matters**: Must distinguish between procedure definition (`CREATE PROCEDURE`) and procedure invocation
- **Parentheses optional**: Some invocations use `EXECUTE IMMEDIATE 'procedure_name'` - should be excluded per assumptions

**Decision**: Use regex patterns to match:
1. `\bEXEC(?:UTE)?\s+(\w+(?:\.\w+)?)\s*\(` - EXECUTE or EXEC statements
2. `\b\w+(?:\.\w+)?\s*\((?!\s*SELECT\s|\s*INSERT\s|\s*UPDATE\s|\s*DELETE\s)` - Procedure name followed by opening paren (not followed by DML keywords)
3. Must be outside of CREATE/ALTER/DROP statement contexts

**Rationale**: Covers the most common Oracle procedure call patterns while avoiding false positives from DML statements and procedure definitions.

---

### Task 2: Gauss Procedure Call Detection Patterns

**Objective**: Identify patterns for detecting direct procedure calls in Gauss stored procedures.

**Findings**:
- Gauss SQL is similar to Oracle but with some PostgreSQL extensions
- **Direct invocation**: Same as Oracle pattern
- **Call statement**: `CALL procedure_name` is more common in Gauss
- **Anonymous blocks**: May call procedures within `BEGIN...END` blocks
- **Dynamic SQL**: `EXECUTE IMMEDIATE` should be excluded per assumptions

**Decision**: Extend Oracle patterns to include Gauss-specific CALL pattern:
1. All Oracle patterns apply
2. Add pattern: `\bCALL\s+(\w+(?:\.\w+)?)\s*\(` - CALL statements

**Rationale**: Gauss follows Oracle syntax but has additional CALL syntax support. Reusing Oracle patterns ensures consistency.

---

### Task 3: Hive Procedure Call Detection Patterns

**Objective**: Identify patterns for detecting direct procedure calls in Hive stored procedures.

**Findings**:
- Hive primarily uses SQL-style scripting, not traditional stored procedures
- **No true procedures**: Hive uses UDFs (User Defined Functions) and UDTFs (User Defined Table Functions)
- **Function invocation**: `function_name(param_list)` - similar to procedure calls
- **Script execution**: Can use `source script_file` or `add jar` statements
- **Transformations**: TRANSFORM statements may invoke external scripts

**Decision**: Since Hive doesn't have traditional procedures, adapt to function calls:
1. Pattern: `\b\w+(?:\.\w+)?\s*\(` - function invocation
2. Must exclude built-in Hive functions (SUM, COUNT, AVG, etc.)
3. Track these as "procedure calls" in complexity context

**Rationale**: While Hive doesn't have procedures, function calls represent similar complexity. Treating them as procedure calls maintains consistency across dialects.

---

### Task 4: Loop Detection Patterns

**Objective**: Identify patterns for detecting loop constructs in all SQL dialects.

**Findings**:
- **FOR loops**: `FOR loop_variable IN (SELECT ...)` or `FOR i IN 1..10`
- **WHILE loops**: `WHILE condition LOOP` or `WHILE (condition) DO`
- **LOOP construct**: `LOOP ... END LOOP` (unconditional loop)
- **Nested loops**: Must track nesting depth for complexity
- **Context-sensitive**: LOOP keyword can appear in other contexts (e.g., "exit loop")

**Decision**: Use context-aware parsing:
1. Track current loop depth when iterating through statements
2. Increment depth on: `FOR`, `WHILE`, `LOOP` keywords at statement start
3. Decrement depth on: `END LOOP`, `END FOR`, `END WHILE`, or matching `END` statements
4. Any procedure call with depth > 0 is marked as "in loop"

**Rationale**: Simple keyword counting isn't sufficient. Context-aware tracking accurately identifies when procedure calls occur within loop constructs.

---

### Task 5: ProcedureCallMetric Data Model Design

**Objective**: Define data structure for storing procedure call information.

**Findings**:
- Must support: procedure name, call count, in-loop flag
- Similar to existing: `nestedProcedureCount` and `nestedProcedureList` in ComplexityMetrics
- Need aggregation: Multiple calls to same procedure should be single entry
- Serialization: Must work with both JSON (API) and Excel (export)

**Decision**: Create new model class with:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureCallMetric {
    private String procedureName;           // Fully qualified name
    private int callCount;                   // Total number of calls
    private boolean calledInLoop;              // True if any call inside loop
}
```

Add to `ComplexityMetrics`:
```java
private int procedureCallCount;                      // Total count
private List<ProcedureCallMetric> procedureCallDetails;  // Detailed breakdown
```

**Rationale**: Follows existing patterns in the codebase (e.g., similar to nested procedure tracking). Lombok builder pattern matches project standards.

---

### Task 6: Excel Column Ordering and Formatting

**Objective**: Determine optimal placement of new columns in Excel export.

**Findings**:
- **Current columns**: Package Name, Procedure Name, Line Count, Overall Score, Loop Count, Max Loop Nesting Level, Custom Function Count, Custom Function List, High Weight Table Count, High Weight Table List, High Weight Procedure Count, High Weight Procedure List, Subquery Count, Table Count, Table List, Has Exceptions, Failed Statements, Subtransaction Count, Max Subtransaction Nesting Level, Subtransaction Details
- **Logical grouping**: Related metrics should be adjacent (e.g., Table Count and Table List)
- **Placement**: Procedure Call metrics should be near related "procedure call" metrics (High Weight Procedure Count, High Weight Procedure List)

**Decision**: Insert new columns between "High Weight Procedure List" and "Subquery Count":
1. After column 11 (High Weight Procedure List)
2. Insert: "Procedure Call Count" (column 12)
3. Insert: "Procedure Call Details" (column 13)

**Excel Summary Format**: "PROC_A:3, PROC_B:2(in loop)" - comma-separated with "(in loop)" suffix.

**Rationale**: Groups procedure-related metrics together and maintains logical flow from procedure calls to other complexity metrics.

---

## Resolved Clarifications

All items from Technical Context section have been resolved through research. No additional clarifications needed.

## Alternatives Considered

### Pattern Matching Approach
| Option | Approach | Rejected Because |
|--------|-----------|------------------|
| A | Full SQL parser (ANTLR/JavaCC) | Overkill for this feature, requires maintaining grammar for multiple dialects |
| B | Regex-based pattern matching | **CHOSEN** - Sufficient for detection, lightweight, easier to maintain |
| C | Token-based scanning | More complex than regex, no significant advantage |

### Loop Detection Strategy
| Option | Approach | Rejected Because |
|--------|-----------|------------------|
| A | Keyword counting alone | False positives (LOOP in strings, comments) |
| B | Full AST parsing | Over-engineering for simple depth tracking |
| C | Context-aware depth tracking | **CHOSEN** - Accurate and maintainable |

### Data Storage Strategy
| Option | Approach | Rejected Because |
|--------|-----------|------------------|
| A | Separate database table | Unnecessary overhead, not persisted across requests |
| B | Additional Metrics class | Doesn't follow existing pattern in ComplexityMetrics |
| C | Direct fields in ComplexityMetrics | **CHOSEN** - Consistent with existing metrics, simpler aggregation |

## Technical Decisions Summary

1. **Pattern Matching**: Regex-based approach across all dialects
2. **Loop Detection**: Context-aware depth tracking (not just keyword counting)
3. **Data Model**: New `ProcedureCallMetric` class with fields in `ComplexityMetrics`
4. **Dialect Support**: Oracle and Gauss share patterns, Hive adapted to function calls
5. **Excel Placement**: Inserted after high-weight procedure columns, before subquery metrics
6. **Excel Format**: Comma-separated summary with "(in loop)" suffix
