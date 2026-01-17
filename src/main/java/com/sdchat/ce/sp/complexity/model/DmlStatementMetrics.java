package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a DML SQL statement with its complexity metrics.
 * This combines the SQL statement information with its complexity metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmlStatementMetrics {
    
    /**
     * The type of SQL statement (INSERT, UPDATE, DELETE, MERGE)
     */
    private String type;
    
    /**
     * The SQL statement text
     */
    private String sql;
    
    /**
     * The database dialect (Oracle, MySQL, PostgreSQL, etc.)
     */
    private String dialect;
    
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
     * The number of GROUP BY clauses
     */
    private int groupByCount;
    
    /**
     * The number of ORDER BY clauses
     */
    private int orderByCount;
    
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
     * The number of cursor declarations
     */
    private int cursorCount;
    
    /**
     * The list of cursor names
     */
    private List<String> cursorList;
    
    /**
     * The number of cursor operations (OPEN, FETCH, CLOSE)
     */
    private int cursorOperationCount;
    
    /**
     * The maximum nesting level of cursors
     */
    private int maxCursorNestingLevel;
    
    /**
     * The number of lines in the source code
     */
    private int lineCount;
    
    /**
     * Additional metrics specific to the statement type or database
     */
    private Map<String, Object> additionalMetrics;

    /**
     * Total number of SQL hints found in this statement
     */
    private int hintCount;

    /**
     * List of SQL hint texts found in this statement
     */
    private List<String> hintList;

    /**
     * Number of invalid hints in this statement
     */
    private int invalidHintCount;

    /**
     * List of invalid hint details
     */
    private List<InvalidHintDetail> invalidHintList;

    /**
     * Create a DmlStatementMetrics from a SqlStatement and its ComplexityMetrics
     * 
     * @param statement The SQL statement
     * @param metrics The complexity metrics for the statement
     * @return A new DmlStatementMetrics object
     */
    public static DmlStatementMetrics from(SqlStatement statement, ComplexityMetrics metrics) {
        return DmlStatementMetrics.builder()
                .type(statement.getType())
                .sql(statement.getSql())
                .dialect(statement.getDialect())
                .overallScore(metrics.getOverallScore())
                .tableCount(metrics.getTableCount())
                .tableList(metrics.getTableList())
                .joinCount(metrics.getJoinCount())
                .whereConditionCount(metrics.getWhereConditionCount())
                .subqueryCount(metrics.getSubqueryCount())
                .aggregateFunctionCount(metrics.getAggregateFunctionCount())
                .caseExpressionCount(metrics.getCaseExpressionCount())
                .setOperationCount(metrics.getSetOperationCount())
                .groupByCount(metrics.getGroupByCount())
                .orderByCount(metrics.getOrderByCount())
                .queryDepth(metrics.getQueryDepth())
                .loopCount(metrics.getLoopCount())
                .maxLoopNestingLevel(metrics.getMaxLoopNestingLevel())
                .customFunctionCount(metrics.getCustomFunctionCount())
                .customFunctionList(metrics.getCustomFunctionList())
                .highWeightTableCount(metrics.getHighWeightTableCount())
                .highWeightTableList(metrics.getHighWeightTableList())
                .nestedProcedureCount(metrics.getNestedProcedureCount())
                .nestedProcedureList(metrics.getNestedProcedureList())
                .highWeightProcedureCount(metrics.getHighWeightProcedureCount())
                .highWeightProcedureList(metrics.getHighWeightProcedureList())
                .cursorCount(metrics.getCursorCount())
                .cursorList(metrics.getCursorList())
                .cursorOperationCount(metrics.getCursorOperationCount())
                .maxCursorNestingLevel(metrics.getMaxCursorNestingLevel())
                .lineCount(metrics.getLineCount())
                .additionalMetrics(metrics.getAdditionalMetrics())
                .hintCount(metrics.getHintCount())
                .hintList(metrics.getHintList())
                .invalidHintCount(metrics.getInvalidHintCount())
                .invalidHintList(metrics.getInvalidHintList())
                .build();
    }
}
