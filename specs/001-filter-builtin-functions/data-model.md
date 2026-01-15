# Data Model: Filter Built-in Functions from Analysis Results

## Overview

This document describes the data models required for filtering GaussDB built-in functions from complexity analysis results.

## Entity Definitions

### BuiltInFunction

Represents a single built-in function from the `gaussdb_functions.json` file.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Function name (e.g., "gs_index_advise") |
| `category` | String | Yes | Category from JSON (e.g., "AI特性函数") |
| `parameters` | String | No | Parameter signature |
| `description` | String | No | Function description |
| `returnType` | String | No | Return type information |

**Constraints**:
- Name must be unique within the function list
- Case-insensitive matching for filtering (stored lowercase internally)
- All 1316 functions from 44 categories supported

### FunctionFilterResult

Represents the outcome of filtering operations for a single analysis result.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `filteredFunctions` | List<BuiltInFunction> | Yes | List of functions that were filtered |
| `filteredCount` | int | Yes | Total number of functions filtered |
| `retainedCount` | int | Yes | Number of user-defined functions retained |
| `categoryBreakdown` | Map<String, Integer> | Yes | Count of filtered functions by category |

**Constraints**:
- `filteredCount` = sum of all values in `categoryBreakdown`
- `filteredFunctions` may be empty if no functions were filtered
- Categories match those defined in `gaussdb_functions.json`

### ComplexityMetrics (Extended)

Extends the existing ComplexityMetrics model with filtering information.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `overallScore` | double | Yes | Calculated complexity score |
| `tableCount` | int | Yes | Number of tables detected |
| `joinCount` | int | Yes | Number of join operations |
| `procedureCallCount` | int | Yes | Number of procedure/function calls |
| `filteredFunctions` | FunctionFilterResult | No | Functions excluded from analysis |

**Constraints**:
- `filteredFunctions` is null when no filtering was performed
- All existing fields remain unchanged for backward compatibility

## Validation Rules

1. **BuiltInFunction.name**: Must be a valid SQL identifier (alphanumeric + underscore)
2. **FunctionFilterResult**: `filteredCount` + `retainedCount` = total functions analyzed
3. **Category names**: Must exist in the 44 predefined categories

## State Transitions

N/A - These are data transfer objects, not stateful entities.
