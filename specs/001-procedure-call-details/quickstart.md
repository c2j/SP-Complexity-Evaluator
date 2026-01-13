# Quickstart Guide: Procedure Call Details Feature

**Feature**: Add procedure call tracking to complexity evaluation
**Date**: 2025-01-13
**Phase**: Phase 1 - Design & Contracts

## Overview

This feature adds procedure call detection and reporting to the SP-Complexity-Evaluator. It extends existing evaluators (Oracle, Gauss, Hive) with new metrics that track:
- How many procedures/functions are called within evaluated stored procedure
- Which specific procedures/functions are called, how many times, and whether any call occurs in a loop
- Outputting this information in JSON responses and Excel exports

## Quick Reference

### Files to Create

| File | Purpose | Template |
|------|---------|-----------|
| `ProcedureCallMetric.java` | Model for individual procedure call details | See data-model.md |
| (Modify) `ComplexityMetrics.java` | Add new fields to existing model | See data-model.md |

### Files to Modify

| File | Purpose | Key Changes |
|------|---------|--------------|
| `OracleComplexityEvaluator.java` | Add procedure call detection logic | New regex patterns, loop tracking |
| `GaussComplexityEvaluator.java` | Add procedure call detection logic | Same as Oracle |
| `HiveComplexityEvaluator.java` | Add function call detection logic | Adapted for Hive UDFs |
| `ExcelExportUtil.java` | Add new columns to Excel export | Columns 12-13 |
| `ComplexityEvaluationController.java` | Ensure batch exports copy new fields | Similar to subtransaction fix |

### Files to Create for Testing

| File | Purpose |
|------|---------|
| `ProcedureCallTest.java` | Test procedure call detection across dialects |

## Implementation Approach

### Step 1: Create Data Model

Create `ProcedureCallMetric.java` in `src/main/java/com/sdchat/ce/sp/complexity/model/`:

```java
package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureCallMetric {
    private String procedureName;
    private int callCount;
    private boolean calledInLoop;
}
```

Update `ComplexityMetrics.java` to add:
```java
private int procedureCallCount;
private List<ProcedureCallMetric> procedureCallDetails;
```

### Step 2: Add Detection Logic to Evaluators

For each evaluator (Oracle, Gauss, Hive), add:

1. **Regex patterns** for procedure/function calls (see research.md for patterns)
2. **Loop depth tracking** - maintain a counter that increments on FOR/WHILE/LOOP keywords
3. **Procedure call map** - track `(procedureName, calledInLoop)` pairs
4. **Aggregation** - combine multiple calls to same procedure into single entry

Example pseudo-code:
```java
int loopDepth = 0;
Map<String, Boolean> procedureCalls = new HashMap<>();

for (SqlStatement statement : statements) {
    if (isLoopStart(statement)) {
        loopDepth++;
    }
    if (isLoopEnd(statement)) {
        loopDepth--;
    }
    
    if (isProcedureCall(statement)) {
        String procName = extractProcedureName(statement);
        procedureCalls.merge(procName, loopDepth > 0, Boolean::logicalOr);
    }
}

// Convert map to ProcedureCallMetric list
List<ProcedureCallMetric> details = procedureCalls.entrySet().stream()
    .map(entry -> ProcedureCallMetric.builder()
        .procedureName(entry.getKey())
        .callCount(countCalls(entry.getKey()))  // Count total calls
        .calledInLoop(entry.getValue())
        .build())
    .sorted(Comparator.comparing(ProcedureCallMetric::getProcedureName))
    .collect(Collectors.toList());
```

### Step 3: Update Excel Export

In `ExcelExportUtil.java`:

1. Add "Procedure Call Count" to `columns[]` array (after "High Weight Procedure List")
2. Add "Procedure Call Details" to `columns[]` array (after "Procedure Call Count")
3. In data row creation loop, add:
```java
row.createCell(colIndex++).setCellValue(metrics.getProcedureCallCount());
row.createCell(colIndex++).setCellValue(formatProcedureCallDetails(metrics.getProcedureCallDetails()));
```

Add helper method:
```java
private static String formatProcedureCallDetails(List<ProcedureCallMetric> details) {
    if (details == null || details.isEmpty()) {
        return "";
    }
    return details.stream()
        .map(d -> d.getProcedureName() + ":" + d.getCallCount() +
                 (d.getCalledInLoop() ? "(in loop)" : ""))
        .collect(Collectors.joining(", "));
}
```

### Step 4: Ensure Batch Exports Copy Fields

In `ComplexityEvaluationController.java` batch endpoint, ensure the 3 locations where `ComplexityMetrics.builder()` is called also include:
```java
.procedureCallCount(metrics.getProcedureCallCount())
.procedureCallDetails(metrics.getProcedureCallDetails())
```

Same pattern as the subtransaction fields that were recently fixed.

## Testing Strategy

### Unit Tests

For each dialect evaluator:
```java
@Test
void evaluateStoredProcedure_WithProcedureCalls() {
    String sql = """
        CREATE OR REPLACE PROCEDURE test_proc AS
        BEGIN
            GET_DATA(1);
            FOR i IN 1..5 LOOP
                GET_DATA(i);
            END LOOP;
        END;
        """;
    
    ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(sql, "test_proc", "HR", "Oracle");
    
    assertEquals(6, metrics.getProcedureCallCount());
    assertEquals(1, metrics.getProcedureCallDetails().size());
    assertEquals("GET_DATA", metrics.getProcedureCallDetails().get(0).getProcedureName());
    assertEquals(6, metrics.getProcedureCallDetails().get(0).getCallCount());
    assertTrue(metrics.getProcedureCallDetails().get(0).getCalledInLoop());
}
```

### Integration Tests

Test full flow:
1. Upload ZIP file with procedure calls
2. Evaluate and receive JSON response
3. Verify `procedureCallCount` and `procedureCallDetails` fields
4. Export to Excel
5. Verify columns 12-13 contain correct data

## Common Pitfalls

- **Loop tracking**: Must decrement depth on END LOOP, not just END keyword
- **Procedure names**: Fully-qualified names (schema.procedure) and unqualified are different
- **Regex false positives**: Avoid matching procedure in CREATE PROCEDURE or ALTER PROCEDURE statements
- **Empty results**: Must return 0 and [], not null
- **Excel formatting**: Ensure "(in loop)" suffix is appended correctly

## Debug Tips

- Add DEBUG logging: `log.debug("Found procedure call: {} at depth {}", procName, loopDepth)`
- Test regex patterns with simple test strings before integrating
- Verify loop depth tracking by adding log statements before/after loop detection
- Check Excel output by opening generated file after running tests

## Constitution Compliance

- ✅ Uses existing evaluator interfaces (no new patterns needed)
- ✅ JUnit 5 with @SpringBootTest
- ✅ Error handling: procedure call errors logged at DEBUG level
- ✅ REST API: extends existing endpoints with new fields only
- ✅ Logging: use @Slf4j, DEBUG level for evaluation steps

## Build & Test Commands

```bash
# Compile
./mvnw clean compile

# Run all tests
./mvnw test

# Run specific dialect tests
./mvnw test -Dtest="*Oracle*"
./mvnw test -Dtest="*Gauss*"
./mvnw test -Dtest="*Hive*"

# Run application
./mvnw spring-boot:run
```

## Success Criteria Checklist

- [ ] `ProcedureCallMetric` class created with Lombok annotations
- [ ] `ComplexityMetrics` extended with new fields
- [ ] Oracle evaluator detects procedure calls with 95%+ accuracy
- [ ] Gauss evaluator detects procedure calls with 95%+ accuracy
- [ ] Hive evaluator detects function calls with 95%+ accuracy
- [ ] All evaluators correctly identify calls inside loops
- [ ] JSON responses include `procedureCallCount` and `procedureCallDetails`
- [ ] Excel exports have "Procedure Call Count" and "Procedure Call Details" columns
- [ ] Batch evaluation exports copy new fields correctly
- [ ] All tests pass: `./mvnw test`
- [ ] No regression in existing functionality
