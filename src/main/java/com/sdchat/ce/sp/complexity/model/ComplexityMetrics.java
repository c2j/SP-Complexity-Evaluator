package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the complexity metrics for a SQL statement or stored procedure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityMetrics {

    /**
     * The overall complexity score (higher means more complex)
     */
    private double overallScore;

    /**
     * The number of tables involved
     */
    private int tableCount;

    /**
     * The list of tables involved
     */
    private List<String> tableList;

    /**
     * The number of joins
     */
    private int joinCount;

    /**
     * The number of conditions in WHERE clauses
     */
    private int whereConditionCount;

    /**
     * The number of subqueries
     */
    private int subqueryCount;

    /**
     * The number of aggregate functions
     */
    private int aggregateFunctionCount;

    /**
     * The number of CASE expressions
     */
    private int caseExpressionCount;

    /**
     * The number of UNION, INTERSECT, MINUS operations
     */
    private int setOperationCount;

    /**
     * The depth of nested queries
     */
    private int queryDepth;

    /**
     * The number of loops (FOR, WHILE, LOOP)
     */
    private int loopCount;

    /**
     * The maximum nesting level of loops
     */
    private int maxLoopNestingLevel;

    /**
     * The number of custom function calls
     */
    private int customFunctionCount;

    /**
     * The list of custom functions used
     */
    private List<String> customFunctionList;

    /**
     * The number of high-weight tables referenced
     */
    private int highWeightTableCount;

    /**
     * The list of high-weight tables referenced
     */
    private List<String> highWeightTableList;

    /**
     * The number of nested stored procedure calls
     */
    private int nestedProcedureCount;

    /**
     * The list of nested stored procedures called
     */
    private List<String> nestedProcedureList;

    /**
     * The number of high-weight stored procedure calls
     */
    private int highWeightProcedureCount;

    /**
     * The list of high-weight stored procedures called
     */
    private List<String> highWeightProcedureList;

    /**
     * The name of the stored procedure (null for SQL statements)
     */
    private String procedureName;

    /**
     * The number of lines in the source code
     */
    private int lineCount;

    /**
     * Additional metrics specific to the statement type or database
     */
    private Map<String, Object> additionalMetrics;
}
