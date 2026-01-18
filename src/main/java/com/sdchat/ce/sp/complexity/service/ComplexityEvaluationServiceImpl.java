package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.evaluator.EvaluatorConfiguration;
import com.sdchat.ce.sp.complexity.parser.SqlParser;
import com.sdchat.ce.sp.complexity.parser.StoredProcedureParser;
import com.sdchat.ce.sp.complexity.evaluator.ComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.OracleComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.GaussComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.HiveComplexityEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of complexity evaluation service.
 */
@Slf4j
@Service
public class ComplexityEvaluationServiceImpl implements ComplexityEvaluationService {

    private final OracleSqlParser oracleSqlParser;
    private final OracleStoredProcedureParser oracleStoredProcedureParser;
    private final OracleComplexityEvaluator oracleComplexityEvaluator;

    private final GaussSqlParser gaussSqlParser;
    private final GaussStoredProcedureParser gaussStoredProcedureParser;
    private final GaussComplexityEvaluator gaussComplexityEvaluator;

    private final HiveSqlParser hiveSqlParser;
    private final HiveStoredProcedureParser hiveStoredProcedureParser;
    private final HiveComplexityEvaluator hiveComplexityEvaluator;

    // Maps to store parsers and evaluators by dialect
    private final Map<String, SqlParser> sqlParsers = new HashMap<>();
    private final Map<String, StoredProcedureParser> storedProcedureParsers = new HashMap<>();
    private final Map<String, ComplexityEvaluator> complexityEvaluators = new HashMap<>();

    /**
     * Initialize as maps with available parsers and evaluators.
     */
    public void init() {
        // Register SQL parsers
        sqlParsers.put(oracleSqlParser.getDialect().toLowerCase(), oracleSqlParser);
        sqlParsers.put(gaussSqlParser.getDialect().toLowerCase(), gaussSqlParser);
        sqlParsers.put(hiveSqlParser.getDialect().toLowerCase(), hiveSqlParser);

        // Register stored procedure parsers
        storedProcedureParsers.put(oracleStoredProcedureParser.getDialect().toLowerCase(), oracleStoredProcedureParser);
        storedProcedureParsers.put(gaussStoredProcedureParser.getDialect().toLowerCase(), gaussStoredProcedureParser);
        storedProcedureParsers.put(hiveStoredProcedureParser.getDialect().toLowerCase(), hiveStoredProcedureParser);

        // Register complexity evaluators
        complexityEvaluators.put(oracleComplexityEvaluator.getDialect().toLowerCase(), oracleComplexityEvaluator);
        complexityEvaluators.put(gaussComplexityEvaluator.getDialect().toLowerCase(), gaussComplexityEvaluator);
        complexityEvaluators.put(hiveComplexityEvaluator.getDialect().toLowerCase(), hiveComplexityEvaluator);
    }

    @Override
    public ComplexityMetrics evaluateSqlStatement(String sql, String dialect) throws Exception {
        log.debug("Evaluating SQL statement with dialect: {}", dialect);

        // Initialize if not already done
        if (sqlParsers.isEmpty()) {
            init();
        }

        log.debug("Available SQL parsers: {}", sqlParsers.keySet());
        log.debug("Available complexity evaluators: {}", complexityEvaluators.keySet());

        // Get appropriate parser and evaluator for dialect
        SqlParser parser = getSqlParser(dialect);
        ComplexityEvaluator evaluator = getComplexityEvaluator(dialect);

        log.debug("Using parser: {} and evaluator: {}", parser.getClass().getSimpleName(), evaluator.getClass().getSimpleName());

        // Parse SQL statement
        SqlStatement statement = parser.parse(sql);
        log.debug("Parsed statement - Type: {}, Tables: {}", statement.getType(), statement.getTableList());

        // Evaluate complexity
        ComplexityMetrics result = evaluator.evaluateSqlStatement(statement);
        log.debug("Evaluation result - Score: {}, Tables: {}", result.getOverallScore(), result.getTableList());

        return result;
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect) throws Exception {
        // Call overloaded method with null custom functions
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions) throws Exception {
        // Call overloaded method with null high-weight tables
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception {
        // Call overloaded method with null high-weight procedures
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, highWeightTables, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception {
        // Initialize if not already done
        if (storedProcedureParsers.isEmpty()) {
            init();
        }

        // Get appropriate parser and evaluator for dialect
        StoredProcedureParser parser = getStoredProcedureParser(dialect);
        ComplexityEvaluator evaluator = getComplexityEvaluator(dialect);

        // Parse stored procedure
        StoredProcedure procedure = parser.parse(sourceCode, name, schema);

        // Apply configuration using EvaluatorConfiguration interface (eliminates 18 instanceof chains)
        if (evaluator instanceof EvaluatorConfiguration evaluatorConfig) {
            evaluatorConfig.setCustomFunctions(customFunctions);
            evaluatorConfig.setHighWeightTables(highWeightTables);
            evaluatorConfig.setHighWeightProcedures(highWeightProcedures);
        }

        // Evaluate complexity
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
