# Feature Specification: Procedure Call Details in Output Formats

**Feature Branch**: `001-procedure-call-details`
**Created**: 2025-01-13
**Status**: Draft
**Input**: User description: "在输出的json和excel格式中，增加对存储过程内调用其他存储过程的统计信息和明细信息，类似Table Count和Table List，增加输出 Procedure Call Count 和 Procedure Call Details, 其中Details信息应该包括所调用的存储过程名称、次数和是否在循环内调用"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Procedure Call Information in Single Stored Procedure Evaluation (Priority: P1)

A user evaluates the complexity of a single stored procedure that contains calls to other stored procedures. After evaluation completes, the user views the results in JSON format and sees two new fields: `procedureCallCount` (total number of procedure calls) and `procedureCallDetails` (list of detailed information for each called procedure).

**Why this priority**: This is the core functionality of the feature. Users need to see procedure call information when evaluating individual stored procedures. Without this capability, the feature provides no value.

**Independent Test**: Can be tested by evaluating a single stored procedure with known procedure calls and verifying that the JSON response contains the new fields with correct values.

**Acceptance Scenarios**:

1. **Given** a stored procedure that calls 3 other stored procedures (2 calls outside loops, 1 call inside a loop), **When** the user evaluates this stored procedure and views the JSON response, **Then** the response must show `procedureCallCount` as 3 and `procedureCallDetails` must contain 3 entries, each with the procedure name, call count, and a boolean flag indicating whether it was called inside a loop.
2. **Given** a stored procedure that makes multiple calls to the same procedure (e.g., calls "get_user_data" 5 times), **When** the user evaluates this stored procedure, **Then** `procedureCallDetails` must contain a single entry for "get_user_data" with count=5.
3. **Given** a stored procedure that calls the same procedure both inside and outside loops (e.g., 2 calls outside loop, 3 calls inside loop), **When** the user evaluates this stored procedure, **Then** `procedureCallDetails` must show one entry for that procedure with count=5 and the loop flag set to indicate that at least one call occurred inside a loop.

---

### User Story 2 - Export Procedure Call Information to Excel (Priority: P1)

A user evaluates multiple stored procedures via batch upload and requests the results in Excel format. The user downloads the Excel file and finds that procedure call information is included in dedicated columns, following the same pattern as the existing Table Count and Table List columns.

**Why this priority**: Excel export is a critical output format for this application. Users who prefer Excel analysis must have access to procedure call information in the same format as other metrics.

**Independent Test**: Can be tested by performing a batch evaluation with multiple stored procedures containing procedure calls, exporting to Excel, and verifying that the Excel contains the new columns with correct data.

**Acceptance Scenarios**:

1. **Given** a batch evaluation of 5 stored procedures, some with procedure calls and some without, **When** the user exports the results to Excel, **Then** the Excel must contain two new columns: "Procedure Call Count" and "Procedure Call Details".
2. **Given** stored procedures in the batch with varying numbers of procedure calls, **When** the user views the "Procedure Call Count" column, **Then** each row must show the correct total count of procedure calls for that procedure (or 0 if none).
3. **Given** a stored procedure with multiple called procedures, **When** the user views the "Procedure Call Details" column, **Then** the cell must contain a readable summary of all called procedures with their counts and loop status (e.g., "PROC_A:3, PROC_B:2(in loop)").

---

### User Story 3 - Batch JSON Export Includes Procedure Call Information (Priority: P2)

A user evaluates multiple stored procedures via batch upload and requests the results in JSON format. The user receives a JSON array where each element contains procedure call information for the corresponding stored procedure.

**Why this priority**: Batch JSON evaluation is already supported, and users expect consistency between single and batch evaluation outputs. This ensures that the feature works across all evaluation modes.

**Independent Test**: Can be tested by performing a batch evaluation, requesting JSON format, and verifying that each procedure in the response array includes the new procedure call fields.

**Acceptance Scenarios**:

1. **Given** a batch evaluation of multiple stored procedures, **When** the user requests JSON format, **Then** each procedure object in the response array must include `procedureCallCount` and `procedureCallDetails` fields.
2. **Given** a stored procedure in the batch with 0 procedure calls, **When** the user views its entry in the JSON response, **Then** `procedureCallCount` must be 0 and `procedureCallDetails` must be an empty array.

---

### Edge Cases

- What happens when a stored procedure calls itself recursively? The system should count recursive calls normally and track them like any other procedure call.
- How does the system handle procedures that don't call any other procedures? The system must return `procedureCallCount: 0` and `procedureCallDetails: []` (empty array).
- What if procedure call parsing fails for a particular statement? The system must continue evaluation and include partial results, with failed statements tracked in existing error handling mechanisms.
- How does the system distinguish between direct and indirect procedure calls (procedures called by called procedures)? The system tracks only direct calls within the evaluated stored procedure.
- What happens when the same procedure is called multiple times with the same name but different schemas (e.g., `hr.get_data` vs `finance.get_data`)? These should be tracked as separate entries due to different fully-qualified names.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST detect and count all stored procedure calls within the evaluated stored procedure source code for all supported SQL dialects (Oracle, Gauss, Hive).
- **FR-002**: The system MUST determine whether each procedure call occurs inside a loop construct (FOR, WHILE, LOOP) and record this information.
- **FR-003**: For each called procedure, the system MUST track: the procedure name, the total call count, and whether any call occurred within a loop.
- **FR-004**: When the same procedure is called multiple times, the system MUST aggregate these into a single entry with the total call count.
- **FR-005**: The system MUST include a `procedureCallCount` field (integer) in the JSON output for all stored procedure evaluation results.
- **FR-006**: The system MUST include a `procedureCallDetails` field (array) in the JSON output for all stored procedure evaluation results, where each array element contains: the procedure name (string), call count (integer), and called in loop flag (boolean).
- **FR-007**: The system MUST include a "Procedure Call Count" column in the Excel export output.
- **FR-008**: The system MUST include a "Procedure Call Details" column in the Excel export output.
- **FR-009**: The Excel "Procedure Call Details" column MUST display a human-readable summary of all called procedures, showing the procedure name, count, and loop status (e.g., "PROC_A:3, PROC_B:2(in loop)").
- **FR-010**: For stored procedures with no procedure calls, the system MUST return `procedureCallCount: 0`, `procedureCallDetails: []`, and the Excel columns must show 0 and an empty string respectively.

### Error Handling Requirements

- **FR-EH-001**: Procedure call detection MUST continue even if individual statements fail to parse.
- **FR-EH-002**: Procedure call information MUST be calculated and included in results even when other complexity metrics encounter errors.
- **FR-EH-003**: Any errors during procedure call analysis MUST be logged at the DEBUG level and must not prevent the overall evaluation from completing.

### Key Entities

- **ProcedureCallMetric**: Represents information about a single procedure that is called within the evaluated stored procedure. Contains: the procedure name (string, fully qualified with schema if applicable), the call count (integer, total number of times this procedure is called), and the called in loop flag (boolean, true if any call to this procedure occurs inside a loop construct).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of stored procedure evaluations include `procedureCallCount` and `procedureCallDetails` fields in the JSON response.
- **SC-002**: 100% of Excel exports contain "Procedure Call Count" and "Procedure Call Details" columns with correct data.
- **SC-003**: Procedure call detection accuracy: The system correctly identifies 95%+ of explicit procedure calls in test cases across all supported dialects (Oracle, Gauss, Hive).
- **SC-004**: Loop detection accuracy: The system correctly identifies 95%+ of procedure calls that occur within loop constructs (FOR, WHILE, LOOP) in test cases.
- **SC-005**: Users can verify the procedure call information by inspecting the output within 5 seconds of evaluation completion.

## Assumptions

- The feature applies to all existing evaluation endpoints: single stored procedure evaluation (`/api/complexity/stored-procedure`), file upload evaluation (`/api/complexity/stored-procedure/upload`), batch evaluation (`/api/complexity/batch/upload`), and package evaluation (`/api/complexity/package-body/upload`).
- Procedure calls are identified by matching patterns for direct procedure invocation (e.g., `procedure_name`, `schema.procedure_name`) but not dynamic SQL or execute immediate statements.
- Loop constructs are identified by keywords: FOR, WHILE, LOOP (case-insensitive) for all dialects.
- The Excel summary format will show procedures separated by commas, with "(in loop)" appended to procedures that are called within loops.
- Procedure name tracking is case-sensitive (e.g., `GET_DATA` and `get_data` are treated as different procedures).
- Recursive procedure calls are counted and tracked like any other procedure call (no special handling needed).
