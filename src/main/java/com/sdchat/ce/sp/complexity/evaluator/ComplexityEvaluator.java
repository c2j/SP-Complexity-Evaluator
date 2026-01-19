package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;

/**
 * Interface for complexity evaluators that analyze SQL statements and stored procedures.
 */
public interface ComplexityEvaluator {

    /**
     * Evaluate complexity of a SQL statement.
     *
     * @param statement The SQL statement to evaluate
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateSqlStatement(SqlStatement statement) throws Exception;

    /**
     * Evaluate complexity of a stored procedure.
     *
     * @param procedure The stored procedure to evaluate
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateStoredProcedure(StoredProcedure procedure) throws Exception;

    /**
     * Get dialect supported by this evaluator.
     *
     * @return The SQL dialect name
     */
    String getDialect();
}
