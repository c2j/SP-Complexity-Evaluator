# Research: GaussDB Transaction and Subtransaction Statistics Optimization

**Feature**: 006-gaussdb-transaction-stats
**Date**: 2026-01-17
**Status**: Complete

## Unknowns Resolved

### 1. BEGIN (transaction) vs BEGIN (anonymous block) Distinction in GaussDB

**Question**: How to distinguish between BEGIN used for transactions and BEGIN used for anonymous blocks in GaussDB?

**Research Findings**:

Based on GaussDB documentation:
- **Transaction BEGIN**: `BEGIN [ WORK | TRANSACTION ] [ ISOLATION LEVEL ... | READ WRITE | READ ONLY ]`
- **Anonymous Block BEGIN**: `[DECLARE [declare_statements]] BEGIN execution_statements END;`

**Decision**: Pattern matching strategy for GaussDB

| Pattern | Classification |
|---------|----------------|
| `BEGIN;` (standalone) | Transaction (no WORK/TRANSACTION keyword) |
| `BEGIN WORK;` / `BEGIN TRANSACTION;` | Transaction (with optional keyword) |
| `BEGIN` followed by `ISOLATION LEVEL` or `READ WRITE/READ ONLY` | Transaction (transaction attributes) |
| `BEGIN;` followed by variable declarations (`DECLARE`) | Anonymous Block |
| `BEGIN;` followed by execution statements then `END;` | Anonymous Block |
| `BEGIN;` followed by `END /` | Anonymous Block (PL/SQL style) |

**Algorithm**:
1. After matching `BEGIN`, look ahead to next token
2. If next token is `;` or `WORK` or `TRANSACTION` or `ISOLATION` or `READ` → Transaction BEGIN
3. If next token is `DECLARE` or execution statement → Anonymous Block BEGIN
4. If `BEGIN` is followed by `END` (possibly with `/`) → Anonymous Block

**Rationale**: This approach follows GaussDB syntax rules and matches the documentation provided in the feature description.

### 2. Transaction Control Statement Patterns

**Question**: What are all the transaction control statement patterns to recognize?

**Research Findings**:

**Transaction Initiation**:
- `BEGIN [WORK | TRANSACTION]`
- `START TRANSACTION`

**Transaction Completion**:
- `COMMIT [WORK | TRANSACTION]`
- `END [WORK | TRANSACTION]`
- `ROLLBACK [WORK | TRANSACTION]`

**Savepoint Operations**:
- `SAVEPOINT savepoint_name`
- `ROLLBACK [WORK | TRANSACTION] TO [SAVEPOINT] savepoint_name`
- `RELEASE [SAVEPOINT] savepoint_name`

**Transaction Settings**:
- `SET TRANSACTION ISOLATION LEVEL ...`
- `SET LOCAL TRANSACTION [ISOLATION LEVEL ...]`

**Decision**: Implement regex patterns for each category

```java
// Transaction initiation patterns
private static final Pattern BEGIN_PATTERN = Pattern.compile(
    "\\bBEGIN\\s*(?:WORK|TRANSACTION)?\\s*(?:;|ISOLATION|READ|WORK|TRANSACTION|$)",
    Pattern.CASE_INSENSITIVE
);
private static final Pattern START_TRANSACTION_PATTERN = Pattern.compile(
    "\\bSTART\\s+TRANSACTION\\b",
    Pattern.CASE_INSENSITIVE
);

// Transaction completion patterns
private static final Pattern COMMIT_PATTERN = Pattern.compile(
    "\\bCOMMIT\\s*(?:WORK|TRANSACTION)?\\s*;?",
    Pattern.CASE_INSENSITIVE
);
private static final Pattern END_PATTERN = Pattern.compile(
    "\\bEND\\s*(?:WORK|TRANSACTION)?\\s*;?",
    Pattern.CASE_INSENSITIVE
);
private static final Pattern ROLLBACK_PATTERN = Pattern.compile(
    "\\bROLLBACK\\s*(?:WORK|TRANSACTION)?\\s*(?:TO\\s*(?:SAVEPOINT)?\\s*\\w+)?\\s*;?",
    Pattern.CASE_INSENSITIVE
);

// Savepoint patterns
private static final Pattern SAVEPOINT_PATTERN = Pattern.compile(
    "\\bSAVEPOINT\\s+(\\w+)\\s*;?",
    Pattern.CASE_INSENSITIVE
);
private static final Pattern RELEASE_SAVEPOINT_PATTERN = Pattern.compile(
    "\\bRELEASE\\s*(?:SAVEPOINT)?\\s+(\\w+)\\s*;?",
    Pattern.CASE_INSENSITIVE
);
```

**Rationale**: Pattern-based approach is consistent with existing code style in GaussSqlParser (e.g., CURSOR_PATTERN).

### 3. Complexity Score Calculation

**Question**: How to calculate transaction complexity score from counts?

**Research Findings**:

**Factors to consider**:
- Transaction count (main weight)
- Savepoint count (subtransaction weight)
- Nesting depth (multiplier)
- Unbalanced transactions (penalty)

**Decision**: Formula-based scoring

```
ComplexityScore = (TransactionCount * 10) + (SavepointCount * 5) + (NestingDepth * 3) + (UnbalancedPenalty)

Where:
- TransactionCount: number of BEGIN/START TRANSACTION statements
- SavepointCount: number of SAVEPOINT statements
- NestingDepth: maximum savepoint nesting level
- UnbalancedPenalty: 5 if BEGIN != COMMIT/ROLLBACK, else 0
```

**Score Ranges**:
- 0-20: Low complexity
- 21-50: Medium complexity  
- 51-100: High complexity
- 100+: Very high complexity

**Rationale**: Simple formula that provides meaningful differentiation while being easy to understand and explain.

## Implementation Patterns

### Pattern 1: Parser Enhancement

Extend existing `GaussSqlParser` with transaction detection methods:

```java
public class GaussSqlParser {
    private int transactionCount;
    private int savepointCount;
    private int commitCount;
    private int rollbackCount;
    private List<SavepointInfo> savepoints;
    private int maxNestingDepth;
    
    public void parseTransactionStatements(String sql) {
        // Pattern matching for all transaction control statements
    }
    
    public TransactionMetrics buildTransactionMetrics() {
        return TransactionMetrics.builder()
            .transactionCount(transactionCount)
            .savepointCount(savepointCount)
            .commitCount(commitCount)
            .rollbackCount(rollbackCount)
            .maxNestingDepth(maxNestingDepth)
            .complexityScore(calculateScore())
            .build();
    }
}
```

### Pattern 2: Model Classes

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetrics {
    private int transactionCount;
    private int savepointCount;
    private int commitCount;
    private int rollbackCount;
    private int maxNestingDepth;
    private double complexityScore;
    private boolean hasUnbalancedTransactions;
    private List<SavepointInfo> savepointDetails;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavepointInfo {
    private String name;
    private int lineNumber;
    private int nestingLevel;
}
```

### Pattern 3: Integration with ComplexityEvaluator

Modify `GaussComplexityEvaluator` to include transaction metrics:

```java
@Component
@Slf4j
public class GaussComplexityEvaluator implements ComplexityEvaluator {
    
    @Override
    public ComplexityMetrics evaluate(StoredProcedure procedure) throws Exception {
        ComplexityMetrics metrics = evaluateBasicMetrics(procedure);
        
        // Add transaction metrics
        TransactionMetrics transactionMetrics = evaluateTransactionMetrics(procedure);
        metrics.setTransactionMetrics(transactionMetrics);
        
        return metrics;
    }
}
```

## Alternatives Considered

### Alternative 1: Abstract Syntax Tree (AST) Parsing

- **Pros**: More accurate, handles complex cases better
- **Cons**: Requires significant parser changes, more complex implementation
- **Rejected Because**: Existing codebase uses pattern-based parsing; AST would require breaking changes

### Alternative 2: Recursive Aggregation Across Procedures

- **Pros**: Complete view of all transactions in call chain
- **Cons**: Circular dependency handling complexity, performance concerns
- **Rejected Because**: User clarified only current procedure should be counted (no recursive aggregation)

### Alternative 3: Separate TransactionComplexityEvaluator

- **Pros**: Clean separation of concerns
- **Cons**: Duplicates existing evaluator infrastructure
- **Rejected Because**: Violates simplicity; transaction metrics should be part of overall complexity

## Best Practices

1. **Performance**: Pre-compile all regex patterns as static final fields
2. **Thread Safety**: TransactionMetrics objects are immutable after creation
3. **Error Handling**: Continue parsing even if one statement fails to parse
4. **Logging**: Use DEBUG level for detailed parsing steps
5. **Testing**: Cover all edge cases including comments, strings, and nested blocks

## References

- GaussDB SQL Syntax Documentation (provided in feature description)
- Existing GaussSqlParser implementation patterns
- Existing GaussComplexityEvaluator structure
