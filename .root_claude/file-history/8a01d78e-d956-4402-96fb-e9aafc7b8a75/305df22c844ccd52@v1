package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.ComplexityMetricsCollection;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;

import java.util.List;

/**
 * Service interface for evaluating the complexity of SQL statements and stored procedures.
 */
public interface ComplexityEvaluationService {

    /**
     * Evaluate the complexity of a SQL statement.
     *
     * @param sql The SQL statement text
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateSqlStatement(String sql, String dialect) throws Exception;

    /**
     * Evaluate the complexity of a stored procedure.
     *
     * @param sourceCode The source code of the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect) throws Exception;

    /**
     * Evaluate the complexity of a stored procedure with custom function list.
     *
     * @param sourceCode The source code of the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions) throws Exception;

    /**
     * Evaluate the complexity of a stored procedure with custom function list and high-weight table list.
     *
     * @param sourceCode The source code of the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @param highWeightTables List of high-weight table names
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception;

    /**
     * Evaluate the complexity of a stored procedure with custom function list, high-weight table list, and high-weight procedure list.
     *
     * @param sourceCode The source code of the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @param highWeightTables List of high-weight table names
     * @param highWeightProcedures List of high-weight procedure names
     * @return The complexity metrics
     * @throws Exception If evaluation fails
     */
    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception;

    /**
     * Evaluate the complexity of a package body and all its procedures.
     *
     * @param sourceCode The source code of the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @return A collection of complexity metrics for all procedures in the package body
     * @throws Exception If evaluation fails
     */
    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect) throws Exception;

    /**
     * Evaluate the complexity of a package body and all its procedures with custom function list.
     *
     * @param sourceCode The source code of the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @return A collection of complexity metrics for all procedures in the package body
     * @throws Exception If evaluation fails
     */
    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions) throws Exception;

    /**
     * Evaluate the complexity of a package body and all its procedures with custom function list and high-weight table list.
     *
     * @param sourceCode The source code of the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @param highWeightTables List of high-weight table names
     * @return A collection of complexity metrics for all procedures in the package body
     * @throws Exception If evaluation fails
     */
    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception;

    /**
     * Evaluate the complexity of a package body and all its procedures with custom function list, high-weight table list, and high-weight procedure list.
     *
     * @param sourceCode The source code of the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @param dialect The SQL dialect (Oracle, MySQL, etc.)
     * @param customFunctions List of custom function names
     * @param highWeightTables List of high-weight table names
     * @param highWeightProcedures List of high-weight procedure names
     * @return A collection of complexity metrics for all procedures in the package body
     * @throws Exception If evaluation fails
     */
    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception;
}
