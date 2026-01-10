# Data Model: GaussDB Subtransaction Metrics

**Feature**: [spec.md](spec.md)
**Date**: 2026-01-10

## Overview

This document defines the data models required for implementing subtransaction detection and quantification for GaussDB stored procedures. The design maintains separation of concerns by introducing a dedicated SubtransactionMetric model while preserving the existing ComplexityMetrics structure.

## Entities

### SubtransactionMetric

**Purpose**: Represents detailed information about a single subtransaction detected in a stored procedure.

**Package**: `com.sdchat.ce.sp.complexity.model`

**Fields**:

| Field | Type | Description | Validation Rules |
|--------|------|-------------|-----------------|
| `name` | String | Savepoint name for explicit subtransactions, generated name for implicit subtransactions | Non-empty string, max length 255 |
| `type` | Enum (EXPLICIT, IMPLICIT) | Type of subtransaction | Must be either EXPLICIT or IMPLICIT |
| `dmlStatements` | List<String> | Associated DML statements for implicit subtransactions | Can be empty list for explicit subtransactions |
| `savepointOperations` | List<String> | Operations (SAVEPOINT, ROLLBACK, RELEASE) for explicit subtransactions | Can be empty list for implicit subtransactions |
| `nestingLevel` | Integer | Nesting depth of this subtransaction within savepoint hierarchy | Non-negative integer, max value 10000 |
| `sourceProcedure` | String | Name of procedure containing this subtransaction | Non-empty string, format "package.procedure" or "procedure" |
| `sourceLine` | Integer | Line number in source code where subtransaction detected | Positive integer |

**Rationale**: Separate model allows for detailed subtransaction tracking and Excel export without complicating the existing ComplexityMetrics model. Using an enum for type ensures type safety and better IDE support.

### ComplexityMetrics (Updated)

**Purpose**: Extended to include subtransaction metrics while preserving all existing complexity fields.

**Package**: `com.sdchat.ce.sp.complexity.model`

**New Fields Added**:

| Field | Type | Description |
|--------|------|-------------|
| `subtransactionCount` | Integer | Total number of subtransactions detected (explicit + implicit) |
| `subtransactionDetails` | String | JSON-formatted array of SubtransactionMetric objects for detailed Excel export |
| `maxSubtransactionNestingLevel` | Integer | Maximum nesting depth of subtransactions in this procedure |

**Existing Fields Preserved**: All existing fields (overallScore, tableCount, joinCount, whereConditionCount, subqueryCount, aggregateFunctionCount, caseExpressionCount, setOperationCount, groupByCount, orderByCount, queryDepth, loopCount, maxLoopNestingLevel, customFunctionCount, customFunctionList, highWeightTableCount, highWeightTableList, nestedProcedureCount, nestedProcedureList, highWeightProcedureCount, highWeightProcedureList, cursorCount, cursorList, cursorOperationCount, maxCursorNestingLevel, procedureName, lineCount, fileName, additionalMetrics, dmlStatements, dynamicSqlCount, paramBindingCount, nestedDynamicSqlCount, transactionControlCount, transactionNestingLevel, usesAutonomousTransactions, javaStoredProcedureCount, javaTypeConversionCount, unionCount, unionAllCount, totalUnionCount, unionDepth, withClauseCount, nestedWithCount, lateralViewCount, distributeByCount, clusterByCount, sortByCount, partitionByCount, windowFunctionCount, lengthComplexityMultiplier, unionNestingMultiplier, isLongStatement, isVeryLongStatement, hasLargeLineCount, hasVeryLargeLineCount, characterCount, packageMetrics) - All preserved unchanged

**Rationale**: Adding fields directly to existing model maintains backward compatibility with existing evaluators (Oracle, Hive) and test infrastructure. The subtransactionDetails field as JSON string provides flexible serialization for Excel export without requiring nested model structure.

### State Tracking

**SubtransactionContext** (Internal tracking class - not persisted to model)

**Purpose**: Tracks current state of subtransaction detection during procedure evaluation.

**Fields**:
- `activeSavepoints`: Stack tracking currently open SAVEPOINT names
- `implicitDmlCount`: Count of DML statements without explicit SAVEPOINT markers
- `loopMultiplier`: Configuration parameter for loop iteration count (default: 1)
- `calledProcedures`: Set of procedure names called during evaluation
- `maxNestingLevel`: Maximum nesting depth detected

**Rationale**: Internal context object maintains state during evaluation without persisting to ComplexityMetrics. Used for calculating nesting levels and aggregating subtransactions from nested procedure calls.

### LoopMultiplierConfig

**Purpose**: Configuration parameter for loop iteration count when exact loop counts cannot be parsed.

**Fields**:
- `multiplier`: Integer - Number of loop iterations to apply to subtransaction counts (default: 1)
- `isAutoDetect`: Boolean - Whether system should attempt automatic loop count detection (future enhancement)

**Rationale**: Loop multiplier is provided as a configuration parameter (API request or configuration file) as exact loop counting is out of scope per feature constraints.

## Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                                                     │
│              ComplexityMetrics                           │
│                                                     │
│  ┌─────────────────────┬───────────────────────────┐  │
│  │                     │                          │  │
│  │  New fields:       │                          │  │
│  │  • subtransactionCount                          │  │
│  │  • subtransactionDetails                        │  │
│  │  • maxSubtransactionNestingLevel               │  │
│  │                     │                          │  │
│  └─────────────────────┴                          │  │
│                                                     │
│                                                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                                               │  │
│  │              SubtransactionMetric                 │  │
│  │                                               │  │
│  │  Fields: name, type, dmlStatements,      │  │
│  │            savepointOperations, nestingLevel,     │  │
│  │            sourceProcedure, sourceLine            │  │
│  │                                               │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                     │
└─────────────────────────────────────────────────────────────┘
```

**Cardinality**:
- One ComplexityMetrics can have zero or more SubtransactionMetrics (via subtransactionDetails)
- Each SubtransactionMetric is independent
- ComplexityMetrics has zero or one SubtransactionContext (internal tracking)

## State Transitions

### SubtransactionDetection Workflow

1. **Parse Phase**:
   - Scan source code for SAVEPOINT statements (explicit subtransactions)
   - Scan for DML statements without SAVEPOINT markers (implicit subtransactions)
   - Scan for EXCEPTION blocks (implicit subtransactions via EXCEPTION handling)
   - Track savepoint nesting levels using stack

2. **Nesting Calculation**:
   - Each SAVEPOINT pushes to stack
   - Each ROLLBACK TO or RELEASE pops from stack
   - Nesting level = current stack size at detection time

3. **Loop Multiplier Application**:
   - Implicit subtransaction count is multiplied by loopMultiplier configuration
   - Only applies to implicit DML-based subtransactions
   - Explicit SAVEPOINT subtransactions are not affected by loop multiplier

4. **Nested Procedure Aggregation**:
   - When procedure A calls procedure B:
     - Parse procedure B for subtransactions
     - Add B's subtransaction count to A's total
     - Recursively process if B calls other procedures
   - Mark aggregated subtransactions with sourceProcedure = "B"

5. **Error Handling**:
   - Malformed SAVEPOINT syntax is logged with WARN level
   - Evaluation continues even if subtransaction parsing fails
   - Failed subtransaction detections are included in failedStatements list

## Excel Export Mapping

### New Columns

| Excel Column | Model Field | Data Type | Format |
|-------------|-------------|------------|--------|
| Subtransaction Count | ComplexityMetrics.subtransactionCount | Integer | Right-aligned |
| Subtransaction Details | ComplexityMetrics.subtransactionDetails | String | JSON array string |
| Max Subtransaction Nesting Level | ComplexityMetrics.maxSubtransactionNestingLevel | Integer | Right-aligned |

### Example Subtransaction Details JSON

```json
[
  {
    "name": "sp_update_data",
    "type": "EXPLICIT",
    "dmlStatements": [],
    "savepointOperations": ["SAVEPOINT", "RELEASE"],
    "nestingLevel": 1,
    "sourceProcedure": "pkg_main.process_order",
    "sourceLine": 25
  },
  {
    "name": "implicit_dml_1",
    "type": "IMPLICIT",
    "dmlStatements": ["INSERT INTO log VALUES (1)"],
    "savepointOperations": [],
    "nestingLevel": 0,
    "sourceProcedure": "pkg_main.process_order",
    "sourceLine": 35
  },
  {
    "name": "implicit_dml_2",
    "type": "IMPLICIT",
    "dmlStatements": ["DELETE FROM temp WHERE id > 100"],
    "savepointOperations": [],
    "nestingLevel": 0,
    "sourceProcedure": "pkg_main.process_order",
    "sourceLine": 42
  }
]
```

## Validation Rules

### SubtransactionMetric Validation

- `name`: Required for all subtransactions
- `type`: Required, must match enum values (EXPLICIT, IMPLICIT)
- `nestingLevel`: Required, must be non-negative, recommend limit to 10,000
- `sourceProcedure`: Required for traceability
- `sourceLine`: Required for source code reference

### ComplexityMetrics Validation

- `subtransactionCount`: Must be >= 0, equals size of subtransactionDetails array
- `subtransactionDetails`: Valid JSON array of SubtransactionMetric objects, can be empty string for no subtransactions
- `maxSubtransactionNestingLevel`: Must be >= 0, equals maximum nestingLevel across all SubtransactionMetrics

### Edge Case Handling

1. **Same-named SAVEPOINT** (GaussDB behavior):
   - Both old and new savepoints exist
   - Only latest is used for ROLLBACK TO or RELEASE
   - Detection: Track last occurrence line number for each name

2. **Unsupported Contexts**:
   - SAVEPOINT in TRIGGER: Detect but flag as unsupported (FR-012)
   - SAVEPOINT in EXECUTE: Detect but flag as unsupported (FR-012)
   - SAVEPOINT in CURSOR: Detect but flag as unsupported (FR-012)
   - SAVEPOINT in PL/JAVA, PL/PYTHON: Detect but flag as unsupported (FR-012)

3. **EXCEPTION Block Subtransactions**:
   - One implicit subtransaction per EXCEPTION block
   - EXCEPTION blocks in EXCEPTION blocks do not create additional implicit subtransactions (handled correctly)

4. **Loop Multiplier**:
   - Applies to implicit DML subtransactions only
   - Explicit SAVEPOINT subtransactions are NOT multiplied
   - Default value is 1 (no multiplication)

5. **Nesting Overflow**:
   - If nesting level > 10,000: Log WARN with message "Subtransaction nesting level exceeds 10,000 recommended threshold for performance"
   - Continue evaluation (do not fail)

6. **Empty Procedure**:
   - No DML statements and no SAVEPOINT statements: subtransactionCount = 0
   - Empty subtransactionDetails JSON string: "[]"

## Implementation Notes

### Lombok Annotations

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtransactionMetric {
    // fields
}
```

### Enum Definition

```java
public enum SubtransactionType {
    EXPLICIT,
    IMPLICIT
}
```

### JSON Serialization

Use Jackson ObjectMapper for subtransactionDetails serialization:

```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(subtransactionList);
```

### Integration Points

1. **ComplexityMetrics.java**: Add subtransactionCount, subtransactionDetails, maxSubtransactionNestingLevel fields
2. **GaussComplexityEvaluator.java**: Add subtransaction detection logic in evaluateStoredProcedure method
3. **ExcelExportUtil.java**: Add three new columns for subtransaction metrics
4. **GaussComplexityEvaluatorTest.java**: Add tests for explicit, implicit, and nested subtransactions
