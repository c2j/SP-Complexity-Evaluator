# API Contract: Procedure Call Details Extension

**Feature**: Add procedure call tracking to complexity evaluation responses
**Date**: 2025-01-13
**Phase**: Phase 1 - Design & Contracts

## Modified Endpoints

This feature extends existing endpoints without creating new APIs. The following endpoints will include additional fields in their responses:

1. **POST** `/api/complexity/stored-procedure` - Single stored procedure evaluation
2. **POST** `/api/complexity/stored-procedure/upload` - File upload evaluation
3. **POST** `/api/complexity/batch/upload` - Batch ZIP evaluation
4. **POST** `/api/complexity/package-body/upload` - Package body evaluation

## Updated Response Schema: ComplexityMetrics

The `ComplexityMetrics` object is extended with two new fields.

### Added Fields

| Field | Type | Description | Required | Default |
|-------|------|-------------|----------|---------|
| procedureCallCount | integer | Total number of procedure calls in the evaluated stored procedure | Yes | 0 |
| procedureCallDetails | Array<ProcedureCallMetric> | Detailed breakdown of each called procedure with counts and loop status | Yes | [] |

### ProcedureCallMetric Object

| Field | Type | Description | Required | Valid Values |
|-------|------|-------------|----------|--------------|
| procedureName | string | Fully qualified name of called procedure or function | Yes | Non-empty string |
| callCount | integer | Total number of times this procedure is called | Yes | >= 0 |
| calledInLoop | boolean | True if at least one call occurs within a loop construct | Yes | true, false |

## Example Response (JSON)

### Single Procedure Evaluation

```json
{
  "overallScore": 75.5,
  "tableCount": 3,
  "tableList": ["EMPLOYEES", "DEPARTMENTS"],
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
  "loopCount": 2,
  "maxLoopNestingLevel": 1,
  "customFunctionCount": 0,
  "customFunctionList": [],
  "highWeightTableCount": 0,
  "highWeightTableList": [],
  "nestedProcedureCount": 0,
  "nestedProcedureList": [],
  "highWeightProcedureCount": 0,
  "highWeightProcedureList": [],
  "procedureName": "process_employee_data",
  "lineCount": 45,
  "fileName": "process_employee_data.sql",
  "additionalMetrics": null,
  "failedStatements": [],
  "hasExceptions": false
}
```

### Batch Evaluation Response

```json
[
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
    "procedureName": "process_employee_data",
    "lineCount": 45,
    "fileName": "proc1.sql"
  },
  {
    "overallScore": 42.3,
    "tableCount": 1,
    "procedureCallCount": 0,
    "procedureCallDetails": [],
    "procedureName": "simple_query",
    "lineCount": 12,
    "fileName": "proc2.sql"
  }
]
```

### Empty Procedure Call Details

```json
{
  "overallScore": 35.0,
  "tableCount": 2,
  "procedureCallCount": 0,
  "procedureCallDetails": [],
  "procedureName": "no_calls_procedure",
  "lineCount": 20
}
```

## Excel Export Format

When `responseFormat=excel`, the output includes two new columns:

### Column Placement

| Column Index | Column Name | Field | Format |
|-------------|-------------|--------|--------|
| 12 | Procedure Call Count | procedureCallCount | Integer |
| 13 | Procedure Call Details | procedureCallDetails | String (comma-separated summary) |

### Excel Example

| Package Name | Procedure Name | ... | High Weight Procedure List | **Procedure Call Count** | **Procedure Call Details** | Subquery Count |
|-------------|----------------|-----|--------------------------|------------------------|------------------------|----------------|
| emp_pkg | process_employee_data | ... | | **5** | GET_DATA:3, UPDATE_EMPLOYEE:2(in loop) | 2 |
| emp_pkg | simple_query | ... | | **0** | | 0 |

### Excel Summary Format Rules

1. Procedures separated by commas: `PROC_A:3, PROC_B:2`
2. Append "(in loop)" to procedures where `calledInLoop=true`: `PROC_A:3(in loop)`
3. Empty when `procedureCallCount=0`

Examples:
- Single procedure not in loop: `GET_DATA:3`
- Multiple procedures, some in loop: `GET_DATA:3, UPDATE_EMPLOYEE:2(in loop)`
- No procedures: (empty cell)

## Validation Rules

- `procedureCallCount` must equal the sum of all `callCount` values in `procedureCallDetails`
- `procedureCallCount` must be 0 if `procedureCallDetails` is empty
- `procedureCallDetails` array must be sorted by procedure name (alphabetical) for consistent output
- Procedure names are case-sensitive: "GET_DATA" and "get_data" are different procedures
- Same procedure called multiple times must be aggregated into a single entry with total count

## Error Handling

Errors in procedure call detection must not break the evaluation process:
- Continue evaluation even if individual statements fail to parse
- Return partial results if some statements contain parse errors
- Log errors at DEBUG level
- Include failed statements in `failedStatements` array

## Backward Compatibility

The new fields are **additive** - they do not modify or remove existing fields.
Existing API clients will continue to work without changes (they can ignore the new fields).
Clients expecting only existing fields will continue to function correctly.
