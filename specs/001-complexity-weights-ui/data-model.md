# Data Model: Complexity Weights UI Enhancement

## Core Entities

### WeightConfiguration

Represents the complete set of weight factors for complexity calculation.

**Fields**:
- `id`: Unique identifier for the configuration
- `name`: User-defined name for saved configurations
- `dialect`: Database dialect (ORACLE, GAUSS, HIVE)
- `isDefault`: Boolean indicating if this is the system default
- `createdAt`: Timestamp when configuration was created
- `sqlWeights`: Map of SQL factor names to weight values
- `procedureWeights`: Map of procedure factor names to weight values

**Weight Factors (SQL)**:
- `tableCount`: Weight for table count (default: 1.0/1.0/1.0)
- `joinCount`: Weight for join operations (default: 2.0/2.0/2.0)
- `whereConditionCount`: Weight for WHERE conditions (default: 1.5/1.0/1.5)
- `subqueryCount`: Weight for subqueries (default: 3.0/3.0/3.0)
- `aggregateFunctionCount`: Weight for aggregate functions (default: 1.0/1.5/1.0)
- `caseExpressionCount`: Weight for CASE expressions (default: 1.0/1.5/1.5)
- `setOperationCount`: Weight for set operations (default: 2.0/2.0/2.0)
- `groupByCount`: Weight for GROUP BY clauses (default: 1.5/1.5/1.5)
- `orderByCount`: Weight for ORDER BY clauses (default: 1.0/1.0/1.0)
- `lateralViewCount`: Weight for LATERAL VIEW (default: 0.0/0.0/2.0) - Hive only
- `distributeByCount`: Weight for DISTRIBUTE BY (default: 0.0/0.0/1.5) - Hive only
- `clusterByCount`: Weight for CLUSTER BY (default: 0.0/0.0/1.5) - Hive only
- `sortByCount`: Weight for SORT BY (default: 0.0/0.0/1.0) - Hive only
- `partitionByCount`: Weight for PARTITION BY (default: 0.0/0.0/1.5) - Hive only
- `windowFunctionCount`: Weight for window functions (default: 0.0/0.0/2.5) - Hive only

**Weight Factors (Procedure)**:
- `loopCount`: Weight for loops (default: 2.5/2.0/2.5)
- `nestedLoopLevel`: Weight per nested level (default: 1.5/3.0/1.5)
- `customFunctionCount`: Weight for custom functions (default: 2.0/1.5/2.0)
- `highWeightTableCount`: Weight for high-weight table references (default: 2.0/2.0/2.0)
- `nestedProcedureCount`: Weight for nested procedure calls (default: 3.0/3.0/3.0)
- `highWeightProcedureCount`: Weight for high-weight procedure calls (default: 2.5/2.5/2.5)
- `cursorCount`: Weight for cursor operations (default: 2.0/2.0/2.0)

### WeightTemplate

Represents user-saved weight configurations for reuse.

**Fields**:
- `id`: Unique identifier
- `name`: User-defined name (e.g., "Strict Evaluation", "Performance Focused")
- `weightConfigurationId`: Reference to WeightConfiguration
- `isShared`: Whether this template is shared across users
- `createdBy`: User identifier who created the template
- `usageCount`: Number of times this template has been used

### EvaluationSession

Represents a single evaluation session with specific weights.

**Fields**:
- `sessionId`: Unique session identifier
- `weightConfigurationId`: Reference to weights used
- `sqlInput`: Original SQL text
- `dialect`: Database dialect used
- `evaluationResults`: List of complexity results
- `createdAt`: Session start timestamp
- `completedAt`: Session completion timestamp

### EvaluationResult

Represents the result of a single SQL or procedure evaluation.

**Fields**:
- `id`: Unique result identifier
- `sessionId`: Reference to parent session
- `type`: SQL or PROCEDURE
- `source`: Original source text
- `overallScore`: Total complexity score
- `factorBreakdown`: Map of factor names to their contributions
- `weightConfigurationUsed`: Reference to weights that generated this result
- `evaluationTime`: Time taken for evaluation in milliseconds

## Validation Rules

### Weight Configuration Validation
- All weights must be non-negative numbers
- At least one weight must be positive (> 0)
- Maximum weight value: 100.0 (to prevent extreme values)
- All required factors must have values (no null weights)
- Hive-specific weights only apply when dialect is HIVE

### Template Validation
- Template names must be unique per user
- Template names cannot be empty or whitespace only
- Maximum template name length: 100 characters
- No prohibited characters in template names

### Session Validation
- Sessions expire after 24 hours of inactivity
- Maximum concurrent sessions per user: 5
- Maximum SQL input size: 1MB per evaluation

## Data Relationships

```
WeightConfiguration (1) -----> (N) EvaluationSession
WeightConfiguration (1) -----> (N) WeightTemplate
EvaluationSession (1) -----> (N) EvaluationResult
WeightTemplate (1) -----> (1) WeightConfiguration
```

## State Transitions

### Weight Configuration Lifecycle
1. **Default Load** → System loads default weights based on dialect
2. **User Modify** → Session-specific weights created from defaults
3. **Save Template** → User weights persisted as WeightTemplate
4. **Load Template** → Existing template applied to current session
5. **Reset** → Revert to system default weights

### Evaluation Session Lifecycle
1. **Create** → New session initialized with current weights
2. **Evaluate** → SQL processed with current weights
3. **Export** → Results exported with weight configuration
4. **Complete** → Session marked complete and archived
5. **Expire** → Session cleaned up after timeout