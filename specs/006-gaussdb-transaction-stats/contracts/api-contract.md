# API Contract: Transaction Metrics

**Feature**: 006-gaussdb-transaction-stats
**Date**: 2026-01-17

## Overview

This document describes the API contract for transaction metrics. The transaction metrics are returned as part of the existing `ComplexityMetrics` response - no new endpoints are required.

## Response Schema

### ComplexityMetrics (Extended with Transaction Fields)

| Field | Type | Description |
|-------|------|-------------|
| `overallScore` | number | Overall complexity score |
| `transactionControlCount` | integer | Number of transaction control statements (COMMIT, ROLLBACK, SAVEPOINT) |
| `transactionNestingLevel` | integer | Maximum transaction nesting level |
| `usesAutonomousTransactions` | boolean | Whether autonomous transactions are used |
| `subtransactionCount` | integer (nullable) | Number of subtransactions (SAVEPOINT/ROLLBACK pairs) |
| `subtransactionDetails` | string (nullable) | JSON array of SubtransactionMetric objects |
| `maxSubtransactionNestingLevel` | integer (nullable) | Maximum nesting level of subtransactions |
| `transactionMetrics` | TransactionMetrics | Comprehensive transaction metrics object |

### TransactionMetrics Object

| Field | Type | Description |
|-------|------|-------------|
| `transactionCount` | integer | Number of BEGIN/START TRANSACTION statements |
| `savepointCount` | integer | Number of SAVEPOINT statements |
| `commitCount` | integer | Number of COMMIT statements |
| `rollbackCount` | integer | Number of ROLLBACK statements |
| `maxNestingDepth` | integer | Maximum savepoint nesting depth |
| `complexityScore` | number | Derived transaction complexity score |
| `hasUnbalancedTransactions` | boolean | Whether BEGIN count equals COMMIT/ROLLBACK count |
| `savepointDetails` | SavepointInfo[] | Detailed information for each savepoint |

### SavepointInfo Object

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Savepoint name |
| `lineNumber` | integer | Line number in source code |
| `nestingLevel` | integer | Nesting level when savepoint was created |
| `hasRollback` | boolean | Whether this savepoint has a corresponding ROLLBACK TO |

## Example Responses

### Single Stored Procedure Evaluation

```json
{
  "overallScore": 150.0,
  "transactionControlCount": 5,
  "transactionNestingLevel": 2,
  "usesAutonomousTransactions": false,
  "subtransactionCount": 3,
  "maxSubtransactionNestingLevel": 2,
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
      },
      {
        "name": "SP2",
        "lineNumber": 25,
        "nestingLevel": 2,
        "hasRollback": false
      }
    ]
  }
}
```

## Complexity Score Interpretation

| Score Range | Complexity Level | Description |
|-------------|------------------|-------------|
| 0-20 | Low | Simple transaction structure, few or no savepoints |
| 21-50 | Medium | Moderate complexity with some savepoints |
| 51-100 | High | Complex nested transactions with many savepoints |
| 100+ | Very High | Very complex transaction patterns, potential refactoring candidate |

## Error Response

Same as existing error responses - no changes required.

```json
{
  "error": "Evaluation failed",
  "message": "Failed to parse stored procedure",
  "details": "Syntax error at line 42"
}
```
