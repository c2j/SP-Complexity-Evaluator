# Data Model: GaussDB Transaction and Subtransaction Statistics

**Feature**: 006-gaussdb-transaction-stats
**Date**: 2026-01-17
**Status**: Complete

## Overview

This document describes the data model for transaction and subtransaction statistics in GaussDB stored procedures. The model extends existing `ComplexityMetrics` and adds new entities for comprehensive transaction analysis.

## Entity Relationship

```
ComplexityMetrics (existing)
    ├── transactionControlCount (int)
    ├── transactionNestingLevel (int)
    ├── usesAutonomousTransactions (boolean)
    ├── subtransactionCount (Integer)
    ├── subtransactionDetails (String - JSON)
    ├── maxSubtransactionNestingLevel (Integer)
    └── additionalMetrics (Map<String, Object>)
            │
            └── TransactionMetrics (NEW - via additionalMetrics)
                    ├── transactionCount
                    ├── savepointCount
                    ├── commitCount
                    ├── rollbackCount
                    ├── maxNestingDepth
                    ├── complexityScore
                    ├── hasUnbalancedTransactions
                    └── savepointDetails (List<SavepointInfo>)
```

## Data Transfer Objects

### TransactionMetrics

Comprehensive transaction complexity metrics for a stored procedure.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `transactionCount` | `int` | Number of BEGIN/START TRANSACTION statements | >= 0 |
| `savepointCount` | `int` | Number of SAVEPOINT statements | >= 0 |
| `commitCount` | `int` | Number of COMMIT statements | >= 0 |
| `rollbackCount` | `int` | Number of ROLLBACK statements | >= 0 |
| `maxNestingDepth` | `int` | Maximum savepoint nesting level | >= 0 |
| `complexityScore` | `double` | Derived complexity score | > 0 |
| `hasUnbalancedTransactions` | `boolean` | BEGIN != COMMIT/ROLLBACK | - |
| `savepointDetails` | `List<SavepointInfo>` | Detailed savepoint information | nullable |

### SavepointInfo

Detailed metadata for each savepoint.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `name` | `String` | Savepoint name | non-null, non-empty |
| `lineNumber` | `int` | Line number in source code | > 0 |
| `nestingLevel` | `int` | Nesting level when savepoint was created | >= 0 |
| `hasRollback` | `boolean` | Has corresponding ROLLBACK TO | - |

## Existing Models (Extended)

### ComplexityMetrics (Existing)

Extended with new fields for transaction statistics.

**Transaction-related fields (existing, to be enhanced)**:

| Field | Type | Description | New/Existing |
|-------|------|-------------|--------------|
| `transactionControlCount` | `int` | Count of COMMIT, ROLLBACK, SAVEPOINT | Existing |
| `transactionNestingLevel` | `int` | Maximum transaction nesting | Existing |
| `usesAutonomousTransactions` | `boolean` | Whether autonomous transactions used | Existing |
| `subtransactionCount` | `Integer` | Number of subtransactions | Existing |
| `subtransactionDetails` | `String` | JSON array of SubtransactionMetric | Existing |
| `maxSubtransactionNestingLevel` | `Integer` | Maximum subtransaction nesting | Existing |

### SubtransactionMetric (Existing)

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Subtransaction name |
| `type` | `SubtransactionType` | EXPLICIT or IMPLICIT |
| `dmlStatements` | `List<String>` | DML statements in subtransaction |
| `savepointOperations` | `List<String>` | Savepoint operations |
| `nestingLevel` | `Integer` | Nesting level |
| `sourceProcedure` | `String` | Procedure name |
| `sourceLine` | `Integer` | Line number |

### SubtransactionContext (Existing)

| Field | Type | Description |
|-------|------|-------------|
| `maxNestingLevel` | `int` | Maximum nesting level |
| `activeSavepoints` | `Stack<String>` | Stack of active savepoints |
| `implicitDmlCount` | `int` | Count of implicit DML in exceptions |

## Validation Rules

1. **Transaction Counting**:
   - `transactionCount` must equal `commitCount + rollbackCount` for balanced transactions
   - `hasUnbalancedTransactions` is true when they don't match

2. **Nesting Depth**:
   - `maxNestingDepth` must be >= 0
   - For no savepoints, `maxNestingDepth` = 0

3. **Complexity Score**:
   - Formula: `(transactionCount * 10) + (savepointCount * 5) + (maxNestingDepth * 3) + (unbalancedPenalty)`
   - `unbalancedPenalty` = 5 if `hasUnbalancedTransactions`, else 0

## State Transitions

Not applicable - transaction metrics are computed statically from source code, not stateful.

## Integration with ComplexityMetrics

Transaction metrics are stored in `ComplexityMetrics` via:

1. **Direct fields**: Existing transaction fields in `ComplexityMetrics`
2. **Additional metrics**: `TransactionMetrics` stored in `additionalMetrics` map
3. **JSON serialization**: `subtransactionDetails` contains serialized `SubtransactionMetric` list

```java
// Integration example
ComplexityMetrics metrics = ComplexityMetrics.builder()
    .transactionControlCount(5)
    .transactionNestingLevel(2)
    .subtransactionCount(3)
    .maxSubtransactionNestingLevel(2)
    .additionalMetrics(Map.of("transactionMetrics", transactionMetrics))
    .build();
```

## Serialization

### JSON Response Format

```json
{
  "overallScore": 150.0,
  "transactionControlCount": 5,
  "transactionNestingLevel": 2,
  "subtransactionCount": 3,
  "maxSubtransactionNestingLevel": 2,
  "subtransactionDetails": "[{\"name\":\"SP1\",\"type\":\"EXPLICIT\",\"nestingLevel\":1}]",
  "transactionMetrics": {
    "transactionCount": 2,
    "savepointCount": 3,
    "commitCount": 1,
    "rollbackCount": 1,
    "maxNestingDepth": 2,
    "complexityScore": 46.0,
    "hasUnbalancedTransactions": false,
    "savepointDetails": [
      {
        "name": "SP1",
        "lineNumber": 15,
        "nestingLevel": 1,
        "hasRollback": true
      }
    ]
  }
}
```

### Excel Export

Transaction metrics included in Excel export via `ComplexityMetrics` fields.

## Considerations

### Performance

- All regex patterns pre-compiled as static final fields
- Line number calculation cached during parsing
- Savepoint stack operations use standard Java Stack

### Thread Safety

- `TransactionMetrics` objects are immutable after creation
- Parsing methods are stateless (use local variables only)

### Error Handling

- Partial results returned if parsing fails
- Invalid savepoint references logged but don't fail evaluation
- Unbalanced transactions flagged in results
