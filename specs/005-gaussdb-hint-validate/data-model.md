# Data Model: GaussDB SQL Hint Validation

## Entities

### HintValidationResult

Represents the validation result for a single hint.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| hintText | String | Yes | The raw hint text including `/*+ ... */` |
| validationStatus | String | Yes | VALID, NOT_IN_REFERENCE, or SYNTAX_ERROR |
| category | String | No | Hint category from reference (Scan, Join Method, etc.) |
| lineNumber | Integer | Yes | Line number where hint appears in source |
| charOffset | Integer | Yes | Character offset within the line |
| hintName | String | Yes | The hint name (e.g., "tablescan") |
| parameters | String | No | The parameters provided to the hint |
| errorType | String | No | Error classification (MISSING_PARAM, INVALID_PARAM, etc.) |
| errorMessage | String | No | Human-readable error description |

### HintAnalysisSummary

Aggregates validation results across all analyzed hints.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| totalHints | Integer | Yes | Total number of hints found |
| validCount | Integer | Yes | Count of valid hints |
| invalidCount | Integer | Yes | Count of invalid hints |
| validHints | List\<HintValidationResult\> | Yes | Array of valid hint results |
| invalidHints | List\<HintValidationResult\> | Yes | Array of invalid hint results |
| categoryBreakdown | Map\<String, Integer\> | No | Count by hint category |
| errorTypeBreakdown | Map\<String, Integer\> | No | Count by error type |

### StatementHintAnalysis

Analysis results for a single SQL statement within a stored procedure.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| statementId | String | Yes | Unique identifier for the statement |
| statementText | String | Yes | The SQL statement text |
| lineNumber | Integer | Yes | Starting line number of statement |
| hints | List\<HintValidationResult\> | Yes | All hints found in this statement |
| hintSummary | HintAnalysisSummary | Yes | Summary for this statement's hints |

### StoredProcedureAnalysis

Complete analysis of a stored procedure.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| procedureName | String | No | Name of the stored procedure (if available) |
| totalStatements | Integer | Yes | Number of SQL statements analyzed |
| totalHints | Integer | Yes | Total hints found across all statements |
| statementAnalyses | List\<StatementHintAnalysis\> | Yes | Per-statement analysis results |
| overallSummary | HintAnalysisSummary | Yes | Aggregated summary for all hints |
| parseErrors | List\<ParseError\> | Yes | Statements that could not be parsed |

### ParseError

Records a statement that failed to parse.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| statementId | String | Yes | Identifier for the failed statement |
| lineNumber | Integer | Yes | Line number where error occurred |
| errorMessage | String | Yes | Description of parse failure |
| partialText | String | No | Partial text that was parsed before failure |

### HintValidationResponse

API response wrapper for hint validation results.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| success | Boolean | Yes | Whether analysis completed successfully |
| format | String | Yes | Output format (JSON or EXCEL) |
| analysis | HintAnalysisSummary | Conditionally | Analysis results (for JSON) |
| storedProcedureAnalysis | StoredProcedureAnalysis | Conditionally | Full procedure analysis |
| downloadUrl | String | Conditionally | URL for Excel file download |
| errorMessage | String | No | Error message if analysis failed |
| processingTimeMs | Long | Yes | Time taken for analysis in milliseconds |

### HintReference

Internal representation of hint definition from JSON reference.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| hint | String | Yes | Hint name (e.g., "tablescan") |
| category | String | Yes | Hint category |
| description | String | Yes | Description in Chinese |
| syntax | String | Yes | Syntax pattern |
| parameters | List\<HintParameter\> | Yes | Parameter definitions |
| example | String | Yes | Usage example |
| notes | String | No | Additional notes |

### HintParameter

Parameter definition for a hint.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Parameter name |
| description | String | Yes | Parameter description |
| required | Boolean | Yes | Whether parameter is required |

## Validation Rules

1. **Hint Name Matching**: Case-insensitive matching against reference JSON
2. **Syntax Validation**: Check for balanced parentheses and required parameters
3. **Parameter Validation**: Verify required parameters are present (basic check)
4. **Position Tracking**: Record line number and character offset for each hint

## State Transitions

N/A - This feature does not involve stateful entities with transitions.
