# Feature Specification: Filter GaussDB Built-in Functions from Analysis Results

**Feature Branch**: `[001-filter-builtin-functions]`
**Created**: 2026-01-15
**Status**: Draft
**Input**: User description: "使用GaussDB内置函数清单过滤Table和Procedure分析结果"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey MUST be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Filter Built-in Functions from Procedure Analysis (Priority: P1)

As a database analyst, I want to analyze a GaussDB stored procedure's complexity without counting built-in database functions as user-defined complexity, so that I can accurately assess the actual code complexity written by developers.

**Why this priority**: This is the primary use case for the feature. Built-in functions are not user code and should not contribute to complexity metrics. This ensures analysis results reflect true code complexity.

**Independent Test**: Can be fully tested by submitting a stored procedure containing a mix of built-in functions (e.g., `gs_ai_stats_explain`, `hll_empty`, `hash_array`) and user-defined logic, then verifying that built-in functions are excluded from the function count while user-defined code is accurately counted.

**Acceptance Scenarios**:

1. **Given** a stored procedure containing both built-in functions (e.g., `gs_index_advise`, `hypopg_create_index`) and user-defined logic, **When** the complexity analysis is performed, **Then** the built-in functions from `gaussdb_functions.json` MUST be excluded from the function count and complexity calculation.

2. **Given** a stored procedure with 50 function calls where 20 are built-in GaussDB functions, **When** the analysis completes, **Then** the result SHOULD show 30 user-defined functions and indicate that 20 built-in functions were filtered.

3. **Given** a stored procedure containing only built-in functions from the whitelist, **When** the analysis is performed, **Then** the complexity metrics SHOULD reflect zero user-defined function calls with a clear indication that built-in functions were filtered.

---

### User Story 2 - Filter Built-in Functions from Table Analysis (Priority: P1)

As a database analyst, I want to analyze table structures without having built-in functions (used in computed columns or default values) distort the analysis, so that I can focus on the actual table complexity from user-defined elements.

**Why this priority**: Table analysis may encounter computed columns or expressions that reference built-in functions. These should not be counted as user-defined code complexity.

**Independent Test**: Can be fully tested by creating a table with computed columns using built-in functions (e.g., `hash_array` for partitioning, `buckettext` for distribution), then verifying that these built-in function references are filtered from the analysis results.

**Acceptance Scenarios**:

1. **Given** a table definition containing computed columns that use built-in functions like `hll_empty` or `bucketvarchar`, **When** the table analysis is performed, **Then** these built-in function references MUST be excluded from the analysis results.

2. **Given** a table with default values defined using functions from the built-in list (e.g., `now()`, `random()`), **When** the analysis runs, **Then** the default value expressions SHOULD NOT count built-in functions toward complexity metrics.

3. **Given** a table with expressions mixing built-in and user-defined functions, **When** the analysis completes, **Then** only the user-defined functions SHOULD be included in the function count.

---

### User Story 3 - Transparency Through Filtered Functions Report (Priority: P2)

As a database analyst, I want to see which functions were filtered from the analysis, so that I can understand what was excluded and verify the filtering logic is working correctly.

**Why this priority**: Users need visibility into the filtering process to trust the analysis results. This transparency helps users understand why certain functions weren't counted and allows them to identify potential issues.

**Independent Test**: Can be fully tested by analyzing code containing known built-in functions, then verifying the response includes a list of filtered functions with their categories for user review.

**Acceptance Scenarios**:

1. **Given** a procedure analysis request, **When** the analysis completes, **Then** the response MUST include a section listing all functions that were filtered as built-in functions, organized by category from the JSON file.

2. **Given** a table analysis request, **When** the analysis completes, **Then** the result SHOULD include a count and optional detailed list of built-in functions that were filtered from expressions.

3. **Given** a batch analysis request with multiple procedures/tables, **When** filtering is applied, **Then** each individual result SHOULD include its own filtered functions report for traceability.

---

### Edge Cases

- What happens when a function name partially matches a built-in function name? (e.g., `gs_index_advise_custom` vs `gs_index_advise`)
- How does the system handle functions with the same name in different categories (overloaded functions)?
- What happens when the built-in functions JSON file is missing or corrupted?
- How does the system handle functions with special characters or case sensitivity in their names?
- What happens when a user-defined function shadows a built-in function name?

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.

  NOTE: For SP-Complexity-Evaluator, requirements typically involve:
  - SQL dialect support (Oracle, Gauss, Hive, or new dialects)
  - Complexity evaluation features (metrics, scoring algorithms)
  - API functionality (REST endpoints, response formats)
  - Error handling (failed statement collection, exception tracking)
-->

### Functional Requirements

- **FR-001**: System MUST load the built-in functions list from `gaussdb_functions.json` at application startup, parsing all 1316 functions across the 44 documented categories.

- **FR-001a**: The `gaussdb_functions.json` file MUST be bundled within the application JAR file during the build/packaging process to ensure it is always available at runtime.

- **FR-002**: System MUST support case-insensitive matching of function names when filtering, ensuring that `GS_INDEX_ADVISE`, `gs_index_advise`, and `Gs_Index_Advise` are all recognized as the same built-in function.

- **FR-003**: System MUST provide an exact match mechanism for function name filtering, ensuring that only complete function name matches are filtered (e.g., `gs_index_advise` filters the built-in function but not `gs_index_advise_custom`).

- **FR-004**: System MUST filter built-in functions from Procedure complexity analysis before calculating complexity metrics, ensuring they do not contribute to function counts, cyclomatic complexity, or other relevant metrics.

- **FR-005**: System MUST filter built-in functions from Table analysis when evaluating computed columns, default values, check constraints, and other expressions within table definitions.

- **FR-006**: System MUST include category information when reporting filtered functions, indicating which category from `gaussdb_functions.json` each filtered function belongs to (e.g., "AI特性函数", "HashFunc函数", "JSON/JSONB函数和操作符").

- **FR-007**: System MUST fall back gracefully when the built-in functions JSON file cannot be loaded, logging a warning and continuing analysis without filtering.

- **FR-008**: System MUST support loading built-in function lists for different database dialects, allowing the filtering logic to be dialect-specific.

### Error Handling Requirements

- **FR-EH-001**: When the built-in functions JSON file is missing or invalid, the system MUST log a warning at startup and continue without filtering, ensuring the application remains operational.

- **FR-EH-002**: When a function name cannot be matched due to parsing errors, the system MUST log a debug message and continue processing without failing the overall analysis.

- **FR-EH-003**: All filtering operations MUST be logged with appropriate detail level for debugging purposes, including function names that were filtered and their categories.

### Key Entities *(include if feature involves data)*

- **BuiltInFunction**: Represents a single built-in function from the JSON file with attributes for name, parameters, description, return type, and category. This entity is used for matching and filtering decisions.

- **FunctionFilterResult**: Represents the outcome of filtering operations, containing lists of filtered functions organized by category, counts of filtered vs. retained functions, and metadata for reporting.

- **AnalysisResult**: Extended to include a `filteredFunctions` field that contains the report of which built-in functions were excluded from the analysis, providing transparency to users.

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: 100% of built-in functions listed in `gaussdb_functions.json` are correctly identified and filtered from analysis results when present in user code.

- **SC-002**: No user-defined functions are incorrectly filtered as built-in functions, maintaining 0% false positive rate in function classification.

- **SC-003**: Analysis results include a complete list of filtered functions with category information, allowing users to verify the filtering transparency.

- **SC-004**: The filtering process adds negligible overhead to analysis time, with no measurable increase in response latency for single procedure/table analysis.

- **SC-005**: The system handles missing or corrupted built-in functions JSON file gracefully, continuing to provide analysis results with a warning log.

### Assumptions

- The `gaussdb_functions.json` file contains the authoritative list of built-in functions for GaussDB version 2.23.07.210.

- Function name matching should be case-insensitive to match common SQL practices.

- Only exact function name matches should be filtered (not substring matches).

- The filtering should apply to all analysis contexts where functions might appear: procedure bodies, table expressions, computed columns, default values, and constraint expressions.

- The JSON file structure (functions array with category, name, parameters, description, return_type, example fields) remains consistent.

### Dependencies

- The `gaussdb_functions.json` file MUST be present in the resources directory and bundled into the JAR during packaging.

- Depends on the existing SQL parsing infrastructure to extract function calls from code.

- Relies on the current Table and Procedure analysis components to integrate the filtering logic.

## Clarifications

### Session 2026-01-16

- Q: Should the `gaussdb_functions.json` file be bundled into the JAR during packaging? → A: Yes, the JSON file must be packaged into the JAR file to ensure it is available at runtime.

## Notes

<!--
  Optional: Add any additional notes, context, or references that may help during planning and implementation.
-->

- The `gaussdb_functions.json` file contains 1316 functions across 44 categories including AI特性函数, HLL函数和操作符, HashFunc函数, JSON/JSONB函数和操作符, and others.

- Functions are organized by category in the JSON, which should be preserved in the filtering report for user visibility.

- Some functions like `hll_add_agg` and `hll_empty` have multiple overloaded versions with different parameters - all versions should be filtered.

- Functions marked as "内部调用函数" (internal functions) in the descriptions are still built-in functions and should be filtered.
