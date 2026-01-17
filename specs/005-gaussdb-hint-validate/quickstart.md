# Quickstart: GaussDB SQL Hint Validation

This guide helps you quickly get started with the GaussDB SQL Hint Validation feature.

## Prerequisites

- Java 17 or later
- Maven or Gradle
- Access to the application (default: http://localhost:8080)

## Installation

```bash
# Clone the repository
git clone <repository-url>
cd SP-Complexity-Evaluator

# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

## API Endpoints

### Validate a Single SQL Statement

```bash
curl -X POST http://localhost:8080/api/v1/hint-validation/validate/sql \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT /*+ tablescan(t1) */ * FROM t1 WHERE id = 1",
    "includeDetails": true
  }'
```

**Response:**
```json
{
  "success": true,
  "format": "json",
  "processingTimeMs": 15,
  "analysis": {
    "totalHints": 1,
    "validCount": 1,
    "invalidCount": 0,
    "validHints": [
      {
        "hintText": "/*+ tablescan(t1) */",
        "validationStatus": "VALID",
        "category": "Scan",
        "lineNumber": 1,
        "charOffset": 16,
        "hintName": "tablescan",
        "parameters": "t1"
      }
    ],
    "invalidHints": [],
    "categoryBreakdown": {
      "Scan": 1
    },
    "errorTypeBreakdown": {}
  }
}
```

### Validate a Stored Procedure

```bash
curl -X POST http://localhost:8080/api/v1/hint-validation/validate/procedure \
  -H "Content-Type: application/json" \
  -d '{
    "procedure": "CREATE PROCEDURE get_data() BEGIN SELECT /*+ tablescan(t1) */ * FROM t1; SELECT /*+ nestloop(t1 t2) */ * FROM t1 JOIN t2 ON t1.id = t2.id; END",
    "procedureName": "get_data",
    "format": "json"
  }'
```

### Request Excel Output

```bash
curl -X POST http://localhost:8080/api/v1/hint-validation/validate/procedure \
  -H "Content-Type: application/json" \
  -d '{
    "procedure": "SELECT /*+ tablescan(t1) */ * FROM t1",
    "format": "excel"
  }' \
  --output hints-analysis.xlsx
```

### Batch Validation

```bash
curl -X POST http://localhost:8080/api/v1/hint-validation/validate/batch \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "type": "sql",
        "content": "SELECT /*+ tablescan(t1) */ * FROM t1",
        "name": "Query 1"
      },
      {
        "type": "procedure",
        "content": "CREATE PROCEDURE test() BEGIN SELECT /*+ invalid_hint */ * FROM t1; END",
        "name": "Procedure 1"
      }
    ],
    "format": "json"
  }'
```

### Get Available Hints Reference

```bash
curl http://localhost:8080/api/v1/hint-validation/reference
```

## Response Formats

### JSON Response

The JSON response includes:
- `success`: Whether analysis completed successfully
- `analysis`: Summary of hint validation results
- `storedProcedureAnalysis`: Detailed analysis for procedures
- `processingTimeMs`: Time taken for analysis

### Excel Output

Excel files contain:
- **Summary**: Overall statistics and breakdowns
- **Hints**: Detailed hint records with validation status
- **Statements**: For procedures, mapping statements to their hints

## Validation Status Values

| Status | Description |
|--------|-------------|
| `VALID` | Hint matches an entry in the reference JSON |
| `NOT_IN_REFERENCE` | Hint name not found in reference |
| `SYNTAX_ERROR` | Hint has malformed syntax |

## Error Types for Invalid Hints

| Error Type | Description |
|------------|-------------|
| `MISSING_PARAM` | Required parameter is missing |
| `UNBALANCED_PARENS` | Unbalanced parentheses in hint |
| `MALFORMED_COMMENT` | Invalid hint comment structure |
| `UNKNOWN_HINT` | Hint name not in reference |

## Example: Detecting Invalid Hints

**Input:**
```sql
SELECT /*+ invalid_hint(param) */ * FROM t1
```

**Response:**
```json
{
  "success": true,
  "format": "json",
  "processingTimeMs": 8,
  "analysis": {
    "totalHints": 1,
    "validCount": 0,
    "invalidCount": 1,
    "validHints": [],
    "invalidHints": [
      {
        "hintText": "/*+ invalid_hint(param) */",
        "validationStatus": "NOT_IN_REFERENCE",
        "lineNumber": 1,
        "charOffset": 8,
        "hintName": "invalid_hint",
        "parameters": "param",
        "errorType": "UNKNOWN_HINT",
        "errorMessage": "Hint 'invalid_hint' not found in reference. Valid hints include: tablescan, indexscan, nestloop, hashjoin, ..."
      }
    ],
    "categoryBreakdown": {},
    "errorTypeBreakdown": {
      "UNKNOWN_HINT": 1
    }
  }
}
```

## Common Issues

### Hint Not Recognized

Ensure the hint name is spelled correctly and matches the reference. Hint names are case-insensitive.

### Reference File Missing

The application requires `gaussdb_sql_plan_hints.json` in the classpath. Check application logs for loading errors.

### Syntax Errors

Hints must follow the format `/*+ hint_name [parameters] */`. Common issues:
- Missing closing `*/`
- Unbalanced parentheses
- Invalid parameter format

## Next Steps

- Explore the [full API specification](./contracts/openapi.yaml)
- Review the [data model](./data-model.md) for entity details
- Run tests: `./mvnw test`
