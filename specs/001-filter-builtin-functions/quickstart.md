# Quickstart: Filter Built-in Functions from Analysis Results

## Overview

This feature adds the ability to filter GaussDB built-in functions from Table and Procedure complexity analysis results, ensuring analysis reflects only user-defined code complexity.

## Prerequisites

- Java 17 or higher
- Maven Wrapper (`./mvnw`)
- GaussDB stored procedures or table definitions to analyze

## Build & Run

```bash
# Build the project (bundles gaussdb_functions.json into JAR)
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Application starts on port 8080
```

## Usage

### Analyze a Stored Procedure

```bash
curl -X POST http://localhost:8080/api/v1/evaluate/gauss \
  -H "Content-Type: application/json" \
  -d '{"sql": "CREATE PROCEDURE example AS BEGIN gs_index_advise(); user_func(); END;"}'
```

### Response with Filtered Functions

```json
{
  "overallScore": 75.0,
  "tableCount": 0,
  "joinCount": 0,
  "procedureCallCount": 2,
  "filteredFunctions": {
    "filteredCount": 1,
    "retainedCount": 1,
    "categoryBreakdown": {
      "AI特性函数": 1
    },
    "filteredFunctions": [
      {
        "name": "gs_index_advise",
        "category": "AI特性函数"
      }
    ]
  }
}
```

### Analyze a Table

```bash
curl -X POST http://localhost:8080/api/v1/evaluate/gauss/table \
  -H "Content-Type: application/json" \
  -d '{"sql": "CREATE TABLE t1 (id INT, data VARCHAR, computed AS hash_array(id));}'
```

## Verification

```bash
# Run tests
./mvnw test -Dtest=GaussComplexityEvaluatorTest

# Verify filtering behavior
# Check that built-in functions are excluded from procedureCallCount
# Verify filteredFunctions field contains expected built-in functions
```

## Troubleshooting

- **JSON not found**: Ensure `gaussdb_functions.json` is in `src/main/resources/`
- **Functions not filtering**: Verify case-insensitive matching is working
- **Missing category info**: Check JSON file has valid category for each function
