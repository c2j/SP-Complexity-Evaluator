package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * List of SQL statements that failed during parsing or evaluation
     */
    private List<String> failedStatements;

    /**
     * Flag indicating whether there were any exceptions during processing
     */
    private boolean hasExceptions;

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
     * The name of the stored procedure (null for SQL statements)
     */
    private String procedureName;

    /**
     * The number of lines in the source code
     */
    private int lineCount;

    /**
     * The name of the file containing the SQL statement or stored procedure
     */
    private String fileName;

    /**
     * Additional metrics specific to the statement type or database
     */
    private Map<String, Object> additionalMetrics;

    /**
     * The list of DML SQL statements (INSERT, UPDATE, DELETE, MERGE) with their complexity metrics in the stored procedure
     */
    private List<DmlStatementMetrics> dmlStatements;
    
    /**
     * The number of dynamic SQL statements
     */
    private int dynamicSqlCount;
    
    /**
     * The number of parameter bindings in dynamic SQL
     */
    private int paramBindingCount;
    
    /**
     * The number of nested dynamic SQL statements
     */
    private int nestedDynamicSqlCount;
    
    /**
     * The number of transaction control statements (COMMIT, ROLLBACK, SAVEPOINT)
     */
    private int transactionControlCount;
    
    /**
     * The maximum nesting level of transactions
     */
    private int transactionNestingLevel;
    
    /**
     * Flag indicating whether autonomous transactions are used
     */
    private boolean usesAutonomousTransactions;
    
    /**
     * The number of Java stored procedures
     */
    private int javaStoredProcedureCount;
    
    /**
     * The number of type conversions in Java stored procedures
     */
    private int javaTypeConversionCount;
    
    /**
     * Package-level metrics (for packages containing multiple procedures)
     */
    private PackageComplexityMetrics packageMetrics;
}
