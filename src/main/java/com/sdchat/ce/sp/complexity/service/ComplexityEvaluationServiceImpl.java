package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.evaluator.ComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.GaussComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.OracleComplexityEvaluator;
import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.parser.GaussSqlParser;
import com.sdchat.ce.sp.complexity.parser.GaussStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.OracleSqlParser;
import com.sdchat.ce.sp.complexity.parser.OracleStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.SqlParser;
import com.sdchat.ce.sp.complexity.parser.StoredProcedureParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the complexity evaluation service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexityEvaluationServiceImpl implements ComplexityEvaluationService {

    private final OracleSqlParser oracleSqlParser;
    private final OracleStoredProcedureParser oracleStoredProcedureParser;
    private final OracleComplexityEvaluator oracleComplexityEvaluator;

    private final GaussSqlParser gaussSqlParser;
    private final GaussStoredProcedureParser gaussStoredProcedureParser;
    private final GaussComplexityEvaluator gaussComplexityEvaluator;

    // Maps to store parsers and evaluators by dialect
    private final Map<String, SqlParser> sqlParsers = new HashMap<>();
    private final Map<String, StoredProcedureParser> storedProcedureParsers = new HashMap<>();
    private final Map<String, ComplexityEvaluator> complexityEvaluators = new HashMap<>();

    /**
     * Initialize the maps with available parsers and evaluators.
     */
    public void init() {
        // Register SQL parsers
        sqlParsers.put(oracleSqlParser.getDialect().toLowerCase(), oracleSqlParser);
        sqlParsers.put(gaussSqlParser.getDialect().toLowerCase(), gaussSqlParser);

        // Register stored procedure parsers
        storedProcedureParsers.put(oracleStoredProcedureParser.getDialect().toLowerCase(), oracleStoredProcedureParser);
        storedProcedureParsers.put(gaussStoredProcedureParser.getDialect().toLowerCase(), gaussStoredProcedureParser);

        // Register complexity evaluators
        complexityEvaluators.put(oracleComplexityEvaluator.getDialect().toLowerCase(), oracleComplexityEvaluator);
        complexityEvaluators.put(gaussComplexityEvaluator.getDialect().toLowerCase(), gaussComplexityEvaluator);
    }

    @Override
    public ComplexityMetrics evaluateSqlStatement(String sql, String dialect) throws Exception {
        // Initialize if not already done
        if (sqlParsers.isEmpty()) {
            init();
        }

        // Get the appropriate parser and evaluator for the dialect
        SqlParser parser = getSqlParser(dialect);
        ComplexityEvaluator evaluator = getComplexityEvaluator(dialect);

        // Parse the SQL statement
        SqlStatement statement = parser.parse(sql);

        // Evaluate the complexity
        return evaluator.evaluateSqlStatement(statement);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect) throws Exception {
        // Call the overloaded method with null custom functions
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions) throws Exception {
        // Call the overloaded method with null high-weight tables
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception {
        // Call the overloaded method with null high-weight procedures
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, highWeightTables, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception {
        // Initialize if not already done
        if (storedProcedureParsers.isEmpty()) {
            init();
        }

        // Get the appropriate parser and evaluator for the dialect
        StoredProcedureParser parser = getStoredProcedureParser(dialect);
        ComplexityEvaluator evaluator = getComplexityEvaluator(dialect);

        // Parse the stored procedure
        StoredProcedure procedure = parser.parse(sourceCode, name, schema);

        // Set custom functions if provided
        if (customFunctions != null && !customFunctions.isEmpty()) {
            // Pass custom functions to the evaluator
            if (evaluator instanceof OracleComplexityEvaluator) {
                ((OracleComplexityEvaluator) evaluator).setCustomFunctions(customFunctions);
            } else if (evaluator instanceof GaussComplexityEvaluator) {
                ((GaussComplexityEvaluator) evaluator).setCustomFunctions(customFunctions);
            }
        }

        // Set high-weight tables if provided
        if (highWeightTables != null && !highWeightTables.isEmpty()) {
            // Pass high-weight tables to the evaluator
            if (evaluator instanceof OracleComplexityEvaluator) {
                ((OracleComplexityEvaluator) evaluator).setHighWeightTables(highWeightTables);
            } else if (evaluator instanceof GaussComplexityEvaluator) {
                ((GaussComplexityEvaluator) evaluator).setHighWeightTables(highWeightTables);
            }
        }

        // Set high-weight procedures if provided
        if (highWeightProcedures != null && !highWeightProcedures.isEmpty()) {
            // Pass high-weight procedures to the evaluator
            if (evaluator instanceof OracleComplexityEvaluator) {
                ((OracleComplexityEvaluator) evaluator).setHighWeightProcedures(highWeightProcedures);
            } else if (evaluator instanceof GaussComplexityEvaluator) {
                ((GaussComplexityEvaluator) evaluator).setHighWeightProcedures(highWeightProcedures);
            }
        }

        // Evaluate the complexity
        return evaluator.evaluateStoredProcedure(procedure);
    }

    /**
     * Get the SQL parser for the specified dialect.
     *
     * @param dialect The SQL dialect
     * @return The SQL parser
     * @throws IllegalArgumentException If no parser is available for the dialect
     */
    private SqlParser getSqlParser(String dialect) {
        SqlParser parser = sqlParsers.get(dialect.toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("No SQL parser available for dialect: " + dialect);
        }
        return parser;
    }

    /**
     * Get the stored procedure parser for the specified dialect.
     *
     * @param dialect The SQL dialect
     * @return The stored procedure parser
     * @throws IllegalArgumentException If no parser is available for the dialect
     */
    private StoredProcedureParser getStoredProcedureParser(String dialect) {
        StoredProcedureParser parser = storedProcedureParsers.get(dialect.toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("No stored procedure parser available for dialect: " + dialect);
        }
        return parser;
    }

    /**
     * Get the complexity evaluator for the specified dialect.
     *
     * @param dialect The SQL dialect
     * @return The complexity evaluator
     * @throws IllegalArgumentException If no evaluator is available for the dialect
     */
    private ComplexityEvaluator getComplexityEvaluator(String dialect) {
        ComplexityEvaluator evaluator = complexityEvaluators.get(dialect.toLowerCase());
        if (evaluator == null) {
            throw new IllegalArgumentException("No complexity evaluator available for dialect: " + dialect);
        }
        return evaluator;
    }
}
