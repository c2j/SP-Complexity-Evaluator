# Stored Procedure Complexity Evaluator

A service for evaluating the complexity of SQL statements and stored procedures, with initial support for Oracle and extensibility for other database dialects.

## Overview

This service analyzes SQL statements and stored procedures to determine their complexity based on various metrics:

- Number of tables involved
- Number of joins
- Number of conditions in WHERE clauses
- Number of subqueries
- Number of aggregate functions
- Number of CASE expressions
- Number of set operations (UNION, INTERSECT, MINUS)
- Depth of nested queries
- And more...

The service calculates an overall complexity score that can be used to identify complex queries that might need optimization or refactoring.

## Features

- Analyze individual SQL statements
- Analyze entire stored procedures
- Support for Oracle SQL dialect (extensible to other dialects)
- RESTful API for integration with other systems
- Detailed complexity metrics

## Technology Stack

- Java 17
- Spring Boot 3.4.4
- JSqlParser for SQL parsing
- JUnit 5 for testing

## API Endpoints

### Evaluate SQL Statement

```
POST /api/complexity/sql
```

Request body:
```json
{
  "sql": "SELECT * FROM employees WHERE department_id = 10",
  "dialect": "Oracle"
}
```

Response:
```json
{
  "overallScore": 3.5,
  "tableCount": 1,
  "joinCount": 0,
  "whereConditionCount": 1,
  "subqueryCount": 0,
  "aggregateFunctionCount": 0,
  "caseExpressionCount": 0,
  "setOperationCount": 0,
  "queryDepth": 1
}
```

### Evaluate Stored Procedure

```
POST /api/complexity/stored-procedure
```

Request body:
```json
{
  "sourceCode": "CREATE OR REPLACE PROCEDURE get_employee_details(...) AS BEGIN ... END;",
  "name": "get_employee_details",
  "schema": "HR",
  "dialect": "Oracle"
}
```

Response:
```json
{
  "overallScore": 15.8,
  "tableCount": 3,
  "joinCount": 2,
  "whereConditionCount": 3,
  "subqueryCount": 1,
  "aggregateFunctionCount": 2,
  "caseExpressionCount": 1,
  "setOperationCount": 0,
  "queryDepth": 2,
  "additionalMetrics": {
    "statementCount": 3
  }
}
```

## Building and Running

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build

```bash
./mvnw clean package
```

### Run

```bash
./mvnw spring-boot:run
```

The service will be available at http://localhost:8080

## Testing

```bash
./mvnw test
```

## Extending to Other Database Dialects

To add support for a new database dialect:

1. Create a new implementation of `SqlParser` for the dialect
2. Create a new implementation of `StoredProcedureParser` for the dialect
3. Create a new implementation of `ComplexityEvaluator` for the dialect
4. Register the new implementations in `ComplexityEvaluationServiceImpl`

## License

This project is licensed under the MIT License - see the LICENSE file for details.
