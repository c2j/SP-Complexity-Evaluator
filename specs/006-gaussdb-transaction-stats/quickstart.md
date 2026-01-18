# Quickstart: Transaction Metrics for GaussDB

**Feature**: 006-gaussdb-transaction-stats
**Date**: 2026-01-17

## Overview

This quickstart guide explains how to use the enhanced transaction metrics feature for GaussDB stored procedures. The feature provides comprehensive analysis of transaction control statements, savepoints, and nested transaction complexity.

## What is Included

Transaction metrics analysis provides:

1. **Transaction Counting**: Counts BEGIN, START TRANSACTION, COMMIT, ROLLBACK statements
2. **Savepoint Tracking**: Identifies SAVEPOINT, ROLLBACK TO SAVEPOINT, RELEASE SAVEPOINT operations
3. **Nesting Analysis**: Calculates maximum savepoint nesting depth
4. **Complexity Scoring**: Derives a transaction complexity score based on structure
5. **Balance Detection**: Identifies unbalanced transactions (BEGIN != COMMIT/ROLLBACK)

## Usage

### API Usage

Transaction metrics are automatically included when evaluating GaussDB stored procedures via the existing API endpoints.

#### Single Procedure Evaluation

```bash
curl -X POST "http://localhost:8080/api/v1/evaluate/procedure" \
  -H "Content-Type: application/json" \
  -d '{"dialect": "gauss", "sourceCode": "CREATE PROCEDURE proc() BEGIN ... END;"}'
```

#### Batch Evaluation

```bash
curl -X POST "http://localhost:8080/api/v1/evaluate/batch" \
  -H "Content-Type: application/json" \
  -d '{"dialect": "gauss", "files": ["procedure1.sql", "procedure2.sql"]}'
```

### Response Structure

The response includes `transactionMetrics` object with detailed transaction analysis:

```json
{
  "overallScore": 150.0,
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

## Complexity Score Interpretation

| Score | Level | Recommendation |
|-------|-------|----------------|
| 0-20 | Low | Simple transaction structure, good |
| 21-50 | Medium | Moderate complexity, review if score > 30 |
| 51-100 | High | Complex, consider refactoring |
| 100+ | Very High | Very complex, strongly recommend refactoring |

## Examples

### Simple Transaction (Score: 10-20)

```sql
CREATE PROCEDURE simple_proc()
BEGIN
    INSERT INTO table1 VALUES (1);
    COMMIT;
END;
```

Metrics: transactionCount=1, savepointCount=0, complexityScore=10

### Transaction with Savepoints (Score: 25-50)

```sql
CREATE PROCEDURE savepoint_proc()
BEGIN
    SAVEPOINT sp1;
    INSERT INTO table1 VALUES (1);
    ROLLBACK TO SAVEPOINT sp1;
    INSERT INTO table2 VALUES (2);
    COMMIT;
END;
```

Metrics: transactionCount=1, savepointCount=1, maxNestingDepth=1, complexityScore=25

### Nested Savepoints (Score: 50-80)

```sql
CREATE PROCEDURE nested_proc()
BEGIN
    SAVEPOINT sp_outer;
    INSERT INTO table1 VALUES (1);
    SAVEPOINT sp_inner;
    INSERT INTO table2 VALUES (2);
    ROLLBACK TO SAVEPOINT sp_inner;
    COMMIT;
END;
```

Metrics: transactionCount=1, savepointCount=2, maxNestingDepth=2, complexityScore=35

### Complex Nested Transactions (Score: 80+)

```sql
CREATE PROCEDURE complex_proc()
BEGIN
    BEGIN;
    SAVEPOINT sp1;
    INSERT INTO table1 VALUES (1);
    SAVEPOINT sp2;
    INSERT INTO table2 VALUES (2);
    ROLLBACK TO SAVEPOINT sp1;
    COMMIT;
    BEGIN;
    INSERT INTO table3 VALUES (3);
    COMMIT;
END;
```

Metrics: transactionCount=2, savepointCount=2, maxNestingDepth=2, complexityScore=65

## Best Practices

1. **Keep Transactions Short**: Minimize the number of statements within a transaction
2. **Limit Savepoints**: Each savepoint adds complexity; use sparingly
3. **Avoid Deep Nesting**: Nesting beyond 2-3 levels indicates potential refactoring need
4. **Balance Transactions**: Ensure BEGIN matches COMMIT/ROLLBACK
5. **Use for Code Review**: Transaction complexity score can guide code review priorities

## Performance Considerations

- Transaction analysis adds minimal overhead (~5-10% additional processing time)
- For batch evaluation of many files, performance is linear with file count
- Complexity score calculation is O(n) where n is number of lines in procedure

## Troubleshooting

### Missing Transaction Metrics

If transaction metrics don't appear in response:
- Verify dialect is set to "gauss"
- Check that source code contains transaction control statements
- Review logs for parsing warnings

### High Complexity Score

If complexity score is unexpectedly high:
- Check for implicit transactions in exception handlers
- Verify no nested anonymous blocks are being counted
- Review savepoint details for unusual patterns

## Integration with Existing Metrics

Transaction metrics integrate seamlessly with existing complexity metrics:

- `transactionControlCount` maps to existing field
- `transactionNestingLevel` maps to existing field
- `subtransactionCount` extends existing subtransaction tracking
- New `transactionMetrics` object provides comprehensive view

## Next Steps

- See [data-model.md](data-model.md) for detailed data structures
- See [contracts/api-contract.md](contracts/api-contract.md) for API details
- Review existing tests in `GaussComplexityEvaluatorTest.java`
