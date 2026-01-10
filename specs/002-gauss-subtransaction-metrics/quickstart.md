# Quickstart: GaussDB Subtransaction Metrics

**Purpose**: Quick start guide for implementing subtransaction detection in GaussDB stored procedures.
**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

## Prerequisites

1. **Environment Setup**
   ```bash
   # Ensure Java 17 is installed
   java -version

   # Navigate to project root
   cd /path/to/SP-Complexity-Evaluator

   # Verify Maven wrapper exists
   ./mvnw --version
   ```

2. **Run Existing Tests** (verify baseline)
   ```bash
   # Run all tests to ensure existing functionality works
   ./mvnw test

   # Run Gauss evaluator tests specifically
   ./mvnw test -Dtest=GaussComplexityEvaluatorTest
   ```

3. **Review Existing Code**
   - Read `GaussComplexityEvaluator.java` in `src/main/java/com/sdchat/ce/sp/complexity/evaluator/`
   - Understand existing complexity evaluation logic
   - Review how existing metrics are calculated and scored

## Implementation Steps

### Phase 1: Create Data Models

1. **Create SubtransactionMetric Model**
   ```bash
   # Create new model file
   touch src/main/java/com/sdchat/ce/sp/complexity/model/SubtransactionMetric.java
   ```

   **Implementation Guide**: Follow data-model.md for detailed structure including:
   - Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
   - SubtransactionType enum (EXPLICIT, IMPLICIT)
   - All validation rules and field types

2. **Update ComplexityMetrics Model**
   - Add three new fields to existing `ComplexityMetrics.java`:
     - `subtransactionCount` (Integer)
     - `subtransactionDetails` (String, JSON array)
     - `maxSubtransactionNestingLevel` (Integer)
   - Preserve all existing fields (no changes to Oracle/Hive metrics)

### Phase 2: Implement Subtransaction Detection

1. **Add Detection Patterns to GaussComplexityEvaluator**
   - Add constants for SAVEPOINT patterns (see data-model.md)
   - Implement explicit subtransaction detection method
   - Implement implicit subtransaction detection method (DML, EXCEPTION blocks)
   - Track savepoint nesting using stack-based approach

2. **Implement Nested Procedure Aggregation**
   - Add recursive logic to aggregate subtransactions from called procedures
   - Mark aggregated subtransactions with sourceProcedure name
   - Use LoopMultiplierConfig to multiply implicit DML counts

3. **Update Excel Export**
   - Add three new columns to `ExcelExportUtil.java`:
     - "Subtransaction Count" (Integer)
     - "Subtransaction Details" (String, JSON)
     - "Max Subtransaction Nesting Level" (Integer)

### Phase 3: Write Tests

1. **Test Explicit Subtransactions**
   - Create test: `evaluateStoredProcedure_explicitSubtransactions_withSavepoints`
   - Verify SAVEPOINT detection and tracking
   - Verify ROLLBACK TO and RELEASE operations
   - Verify subtransaction count accuracy

2. **Test Implicit Subtransactions**
   - Create test: `evaluateStoredProcedure_implicitSubtransactions_fromDmlStatements`
   - Verify DML statement counting
   - Verify EXCEPTION block detection
   - Verify loop multiplier application

3. **Test Nested Procedure Aggregation**
   - Create test: `evaluateStoredProcedure_aggregatesSubtransactions_fromCalledProcedures`
   - Verify parent includes child's subtransactions
   - Verify sourceProcedure is set correctly
   - Verify deep nesting (A calls B calls C)

4. **Test Edge Cases**
   - Same-named SAVEPOINT handling
   - Unsupported contexts (TRIGGER, EXECUTE, CURSOR)
   - Nesting overflow (>10,000)
   - Empty procedures (no DML, no SAVEPOINT)

## Testing Commands

```bash
# Run only subtransaction-related tests
./mvnw test -Dtest=GaussComplexityEvaluatorTest#evaluateStoredProcedure_explicitSubtransactions*

# Run all Gauss evaluator tests
./mvnw test -Dtest=GaussComplexityEvaluatorTest

# Run full test suite
./mvnw test
```

## Configuration Parameters

### Loop Multiplier

When exact loop count cannot be determined, use configuration:

```java
// In your test setup or API call:
evaluator.setLoopMultiplier(5); // Apply 5x multiplier to implicit DML subtransactions
```

### API Request Example

```json
{
  "sourceCode": "CREATE PROCEDURE test_proc AS\nBEGIN\n  SAVEPOINT sp1;\n  INSERT INTO table VALUES (1);\n  RELEASE SAVEPOINT sp1;\nEND;",
  "name": "test_proc",
  "schema": "test_schema",
  "dialect": "Gauss",
  "loopMultiplier": 5
}
```

## Excel Export Format

After implementation, Excel export will include new columns:

| Package Name | Procedure Name | Subtransaction Count | Subtransaction Details | Max Subtransaction Nesting Level | ...existing columns... |
|-------------|---------------|---------------------|------------------------|--------------------|---------------------|
| test_pkg   | test_proc     | 3             | [{"name":"sp1","type":"EXPLICIT",...}] | 1                    | ... |

## Debugging Tips

1. **Enable DEBUG Logging**
   ```properties
   # In src/main/resources/application.properties:
   logging.level.com.sdchat.ce.sp.complexity.evaluator.GaussComplexityEvaluator=DEBUG
   ```

2. **Check Subtransaction Details JSON**
   - SubtransactionDetails field is a JSON array string
   - Use online JSON formatter to verify structure
   - Example: `[{"name":"sp1","type":"EXPLICIT","dmlStatements":[],"savepointOperations":["SAVEPOINT","RELEASE"],"nestingLevel":1,"sourceProcedure":"proc.main","sourceLine":10}]`

3. **Monitor Performance Impact**
   - Compare evaluation time with and without subtransaction detection
   - Should be < 10% degradation as per success criteria SC-006

## Validation Checklist

Before committing code, verify:

- [ ] SubtransactionMetric model has Lombok annotations
- [ ] SubtransactionType enum values are correct (EXPLICIT, IMPLICIT)
- [ ] ComplexityMetrics has three new fields added
- [ ] ExcelExportUtil has three new columns
- [ ] GaussComplexityEvaluator tests pass
- [ ] Existing Oracle/Hive evaluator tests still pass
- [ ] No regressions in existing metrics (tables, joins, loops, etc.)

## Next Steps

After implementation:

1. Run `/speckit.tasks` to generate detailed task breakdown
2. Implement tasks in order (data models → detection logic → tests)
3. Run full test suite: `./mvnw test`
4. Verify Excel export includes new subtransaction columns
5. Update AGENTS.md with any new patterns or conventions

## Troubleshooting

**Issue**: Subtransaction count is incorrect

**Possible Causes**:
- DML statements counted but have explicit SAVEPOINT nearby
- EXCEPTION blocks not detected correctly
- Loop multiplier not applied correctly
- Nested procedure subtransactions not aggregated

**Debug Steps**:
1. Set DEBUG logging for GaussComplexityEvaluator
2. Add breakpoint in subtransaction detection method
3. Check DML detection regex patterns
4. Verify loop detection logic
5. Check nested procedure call parsing

**Expected Output**:
```
DEBUG: Found explicit SAVEPOINT: sp1 at line 10
DEBUG: DML statements without SAVEPOINT: INSERT at line 15, UPDATE at line 20
DEBUG: EXCEPTION block detected at line 25, implicit subtransaction created
DEBUG: Nested procedure call detected: pkg_child.process_child
DEBUG: Subtransaction count: 3 (1 explicit + 2 implicit × loop multiplier 1)
```
