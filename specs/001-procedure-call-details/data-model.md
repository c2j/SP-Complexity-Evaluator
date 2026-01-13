# Data Model: Procedure Call Details

**Feature**: Add procedure call tracking to complexity evaluation
**Date**: 2025-01-13
**Phase**: Phase 1 - Design & Contracts

## New Entities

### ProcedureCallMetric

**Purpose**: Represents detailed information about a single procedure (or function) that is called within the evaluated stored procedure.

**Fields**:

| Field Name | Type | Description | Validation Rules |
|-------------|------|-------------|-----------------|
| procedureName | String | Fully qualified name of called procedure (e.g., "GET_DATA" or "hr.get_employee") | Required, non-empty, case-sensitive |
| callCount | int | Total number of times this procedure is called within the evaluated procedure | Required, >= 0 |
| calledInLoop | boolean | True if at least one call to this procedure occurs within a loop construct | Required |

**Lombok Annotations**:
```java
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

**Notes**:
- Multiple calls to the same procedure should be aggregated into a single entry
- The `calledInLoop` flag should be true if ANY call to the procedure occurs in a loop
- Procedure names are case-sensitive (e.g., "GET_DATA" and "get_data" are different procedures)
- For Hive, this tracks function calls (UDFs, UDTFs) as "procedures" for consistency

## Modified Entities

### ComplexityMetrics

**Purpose**: Extended to include procedure call statistics.

**New Fields**:

| Field Name | Type | Description | Validation Rules |
|-------------|------|-------------|-----------------|
| procedureCallCount | int | Total count of all procedure calls in the evaluated stored procedure | Required, >= 0 |
| procedureCallDetails | List<ProcedureCallMetric> | Detailed breakdown of each called procedure with counts and loop status | Required, can be empty list |

**Lombok Annotations** (add to existing class):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityMetrics {
    // ... existing fields ...

    private int procedureCallCount;
    private List<ProcedureCallMetric> procedureCallDetails;

    // ... rest of existing fields ...
}
```

**Builder Pattern**: The existing builder pattern will continue to work. New fields can be built like:
```java
ComplexityMetrics.builder()
    // ... existing fields ...
    .procedureCallCount(5)
    .procedureCallDetails(Arrays.asList(
        ProcedureCallMetric.builder()
            .procedureName("GET_DATA")
            .callCount(3)
            .calledInLoop(false)
            .build(),
        ProcedureCallMetric.builder()
            .procedureName("UPDATE_EMPLOYEE")
            .callCount(2)
            .calledInLoop(true)
            .build()
    ))
    .build();
```

**Default Values**:
- `procedureCallCount`: Default to 0 if no procedure calls detected
- `procedureCallDetails`: Default to empty ArrayList if no procedure calls detected

## JSON Serialization

The new fields will be automatically serialized by Jackson (Spring Boot default JSON serializer). Example output:

```json
{
  "overallScore": 75.5,
  "tableCount": 3,
  "procedureCallCount": 5,
  "procedureCallDetails": [
    {
      "procedureName": "GET_DATA",
      "callCount": 3,
      "calledInLoop": false
    },
    {
      "procedureName": "UPDATE_EMPLOYEE",
      "callCount": 2,
      "calledInLoop": true
    }
  ],
  "tableList": ["EMPLOYEES", "DEPARTMENTS"],
  "loopCount": 2
}
```

## Relationships

- **ComplexityMetrics** contains **List<ProcedureCallMetric>** via `procedureCallDetails` field
- **ProcedureCallMetric** is independent and does not reference other entities
- No database persistence required (stateless API)

## State Transitions

Not applicable - entities are stateless DTOs used for API responses.

## Migration Notes

No migration required. The new fields are additive (adding to existing model class). Existing `ComplexityMetrics` instances will continue to work with default values (0 for count, empty list for details).

## Excel Export Mapping

The `ExcelExportUtil` will map these fields to Excel columns:

| Excel Column | ComplexityMetrics Field | Format |
|--------------|------------------------|--------|
| Procedure Call Count | procedureCallCount | Integer value |
| Procedure Call Details | procedureCallDetails | String summary (comma-separated: "PROC_A:3, PROC_B:2(in loop)") |

See `ExcelExportUtil.java` modifications in plan for implementation details.
