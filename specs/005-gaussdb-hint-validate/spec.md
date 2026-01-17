# Feature Specification: GaussDB SQL Hint Validation

**Feature Branch**: `005-gaussdb-hint-validate`
**Created**: 2026-01-16
**Status**: Draft
**Input**: User description: "@src/main/resources/gaussdb_sql_plan_hints.json 005号需求项，对于高斯数据库的输入，识别sql或存储过程中sql所使用的hint，如果在gaussdb_sql_plan_hints.json中能匹配上，则认为是合法的hint，记录下来；如果是在json文件中不存在或有hint语法错误的（可能会被高斯执行计划解析器忽略，但开发者如果不看执行计划的话，会错误地期望该hint能生效，从而产生偏差）。识别情况在json和excel中输出"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Validate SQL Hints in Single SQL Statement (Priority: P1)

As a database developer, I want to analyze a single SQL statement to identify all hints used, so that I can verify whether the hints are valid according to GaussDB documentation.

**Why this priority**: This is the fundamental capability that all other features depend on. Without validating hints in individual SQL statements, the broader analysis of stored procedures cannot function correctly.

**Independent Test**: Can be fully tested by providing a SQL statement with hints and verifying that each hint is correctly categorized as valid or invalid with appropriate details.

**Acceptance Scenarios**:

1. **Given** a SQL statement containing a valid hint like `/*+ tablescan(t1) */`, **When** the system analyzes it, **Then** the hint should be identified as VALID and recorded with its category (Scan) and position in the SQL.

2. **Given** a SQL statement containing an invalid hint not in the reference JSON (e.g., `/*+ invalid_hint(param) */`), **When** the system analyzes it, **Then** the hint should be identified as INVALID, NOT_IN_REFERENCE, and recorded with the raw hint text.

3. **Given** a SQL statement containing a malformed hint (syntax error, e.g., `/*+ tablescan(` without closing parenthesis), **When** the system analyzes it, **Then** the hint should be identified as INVALID, SYNTAX_ERROR, and recorded with the raw hint text and error description.

4. **Given** a SQL statement containing multiple hints with mixed validity, **When** the system analyzes it, **Then** each hint should be independently categorized and all results returned in a comprehensive summary.

---

### User Story 2 - Validate Hints in Stored Procedures (Priority: P1)

As a database developer, I want to analyze a complete stored procedure to identify all hints used across all SQL statements, so that I can audit the entire procedure for hint correctness.

**Why this priority**: Stored procedures contain multiple SQL statements, and developers need to validate all hints at once rather than manually checking each statement individually.

**Independent Test**: Can be fully tested by providing a stored procedure with multiple SQL statements containing various hints and verifying comprehensive hint analysis across all statements.

**Acceptance Scenarios**:

1. **Given** a stored procedure with SQL statements in different sections (DECLARE, BEGIN, IF/ELSE, loops), **When** the system analyzes it, **Then** all hints from all SQL statements should be identified and categorized.

2. **Given** a stored procedure where some SQL statements have valid hints and others have invalid hints, **When** the system analyzes it, **Then** a summary should show counts of valid and invalid hints with detailed breakdowns.

3. **Given** a stored procedure with nested subprocedures or functions, **When** the system analyzes it, **Then** hints in nested code should also be identified and included in the results.

---

### User Story 3 - Output Results in JSON Format (Priority: P1)

As a developer using the system programmatically, I want to receive hint analysis results in JSON format, so that I can integrate the validation results into other tools or automated workflows.

**Why this priority**: JSON is the standard format for machine-readable data interchange, enabling integration with CI/CD pipelines and other automation tools.

**Independent Test**: Can be fully tested by requesting JSON output and verifying the structure, fields, and data types match the specified schema.

**Acceptance Scenarios**:

1. **Given** a request for JSON output format, **When** the system completes analysis, **Then** the result should be a valid JSON object containing hint analysis summary with counts, detailed hint records, and metadata.

2. **Given** a request for JSON output, **When** the analysis includes valid and invalid hints, **Then** the JSON should separate hints into "valid_hints" and "invalid_hints" arrays with appropriate fields (hint_text, position, category, error_type, error_message).

3. **Given** a JSON output request for a stored procedure with multiple SQL statements, **Then** the result should include statement-level breakdowns showing which statements contain which hints.

---

### User Story 4 - Output Results in Excel Format (Priority: P2)

As a database developer or auditor, I want to receive hint analysis results in Excel format, so that I can easily review, filter, sort, and share the analysis with team members.

**Why this priority**: Excel provides human-friendly visualization with filtering and sorting capabilities, useful for team reviews and documentation.

**Independent Test**: Can be fully tested by requesting Excel output and verifying the file is readable with correct column headers and data in appropriate sheets.

**Acceptance Scenarios**:

1. **Given** a request for Excel output, **When** the system completes analysis, **Then** the result should be an .xlsx file containing a "Summary" sheet with overall statistics and a "Hints" sheet with detailed hint records.

2. **Given** Excel output for stored procedure analysis, **When** the file is opened, **Then** it should contain a "Statements" sheet mapping SQL statement identifiers to their hints.

3. **Given** Excel output, **When** filters are applied, **Then** users should be able to filter by hint validity, category, or error type.

---

### Edge Cases

- What happens when the hint reference JSON file (gaussdb_sql_plan_hints.json) is missing or corrupted? → System should log a warning and provide empty/incomplete validation results with clear error message.

- How does system handle SQL comments that look like hints but are not in hint comment format (`/*+ ... */`)? → Regular comments should be ignored; only hint-style comments should be analyzed.

- How does system handle nested hint comments (hints within hints)? → Inner hints should be parsed as separate hints; syntax should be validated at each level.

- What happens when hint parameters reference non-existent tables or indexes? → This is considered a semantic error beyond syntax validation; hints should be flagged as potentially ineffective (not syntax error).

- How does system handle case sensitivity in hint names? → Hint matching should be case-insensitive per GaussDB convention (JSON uses lowercase, examples use lowercase).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST parse SQL statements and stored procedures to extract all hint comments in the format `/*+ hint_name [parameters] */`.

- **FR-002**: System MUST load and use the gaussdb_sql_plan_hints.json file as the authoritative reference for valid hint names, categories, syntax patterns, and parameter specifications.

- **FR-003**: System MUST validate each extracted hint against the reference JSON to determine validity (VALID, NOT_IN_REFERENCE, SYNTAX_ERROR).

- **FR-004**: System MUST record for each hint: hint text, position (line number, character offset), validation status, category, and for invalid hints, error type and description.

- **FR-005**: System MUST provide JSON output format with structured results containing summary statistics and detailed hint records.

- **FR-006**: System MUST provide Excel output format (.xlsx) with Summary and Hints sheets containing analysis results.

- **FR-007**: System MUST handle batch analysis of multiple SQL statements or stored procedures in a single request.

### Error Handling Requirements

- **FR-EH-001**: If the hint reference JSON file is missing or invalid, the system MUST log a warning and continue operation with reduced functionality (no hint validation possible).

- **FR-EH-002**: If a SQL statement cannot be parsed, the system MUST skip that statement and record it as a parse error without failing the entire analysis.

- **FR-EH-003**: If hint extraction fails for a specific hint comment (malformed comment structure), the system MUST record the raw comment text as an unrecognized pattern.

### Key Entities

- **HintValidationResult**: Represents the result of validating a single hint, containing hint_text, validation_status, category, position, and error_details.

- **HintAnalysisSummary**: Aggregates validation results, containing counts of valid/invalid hints, breakdown by category, breakdown by error type.

- **StoredProcedureAnalysis**: Contains analysis of a complete stored procedure, including multiple SQL statement analyses and overall summary.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: System correctly identifies 100% of valid hints that match entries in the reference JSON file.

- **SC-002**: System correctly identifies 100% of invalid hints that do not match any entry in the reference JSON file.

- **SC-003**: System correctly detects syntax errors in hint formatting (malformed comment structure, missing required parameters, unbalanced parentheses).

- **SC-004**: JSON output is machine-readable and can be parsed by standard JSON libraries without errors.

- **SC-005**: Excel output is readable by standard spreadsheet software (Microsoft Excel, LibreOffice Calc) with proper formatting.

- **SC-006**: Analysis of a single stored procedure completes within reasonable time (under 10 seconds for procedures up to 1000 lines of code).

---

**Assumptions**:
- The gaussdb_sql_plan_hints.json file will continue to be maintained as the source of truth for valid hints.
- Hint names are case-insensitive per GaussDB convention.
- The system only validates hint syntax and existence in the reference; it does not validate hint effectiveness or semantic correctness.
