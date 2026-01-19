package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.evaluator.ComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.GaussComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.HiveComplexityEvaluator;
import com.sdchat.ce.sp.complexity.evaluator.OracleComplexityEvaluator;
import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.ComplexityMetricsCollection;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.parser.GaussSqlParser;
import com.sdchat.ce.sp.complexity.parser.GaussStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.HiveSqlParser;
import com.sdchat.ce.sp.complexity.parser.HiveStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.OracleSqlParser;
import com.sdchat.ce.sp.complexity.parser.OracleStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.SqlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@Validated
public class ComplexityEvaluationServiceImpl implements ComplexityEvaluationService {

    private final OracleComplexityEvaluator oracleEvaluator;
    private final GaussComplexityEvaluator gaussEvaluator;
    private final HiveComplexityEvaluator hiveEvaluator;
    private final OracleSqlParser oracleSqlParser;
    private final GaussSqlParser gaussSqlParser;
    private final HiveSqlParser hiveSqlParser;

    public ComplexityEvaluationServiceImpl(
            OracleComplexityEvaluator oracleEvaluator,
            GaussComplexityEvaluator gaussEvaluator,
            HiveComplexityEvaluator hiveEvaluator,
            OracleSqlParser oracleSqlParser,
            GaussSqlParser gaussSqlParser,
            HiveSqlParser hiveSqlParser) {
        this.oracleEvaluator = oracleEvaluator;
        this.gaussEvaluator = gaussEvaluator;
        this.hiveEvaluator = hiveEvaluator;
        this.oracleSqlParser = oracleSqlParser;
        this.gaussSqlParser = gaussSqlParser;
        this.hiveSqlParser = hiveSqlParser;
    }

    private SqlParser getSqlParser(String dialect) {
        if (dialect == null || dialect.trim().isEmpty()) {
            throw new IllegalArgumentException("Dialect is required");
        }

        String dialectUpper = dialect.trim().toUpperCase();
        switch (dialectUpper) {
            case "ORACLE":
                return oracleSqlParser;
            case "GAUSS":
                return gaussSqlParser;
            case "HIVE":
                return hiveSqlParser;
            default:
                throw new IllegalArgumentException("Unsupported dialect: " + dialect);
        }
    }

    private ComplexityEvaluator getEvaluator(String dialect) {
        if (dialect == null || dialect.trim().isEmpty()) {
            throw new IllegalArgumentException("Dialect is required");
        }

        String dialectUpper = dialect.trim().toUpperCase();
        switch (dialectUpper) {
            case "ORACLE":
                return oracleEvaluator;
            case "GAUSS":
                return gaussEvaluator;
            case "HIVE":
                return hiveEvaluator;
            default:
                throw new IllegalArgumentException("Unsupported dialect: " + dialect);
        }
    }

    @Override
    public ComplexityMetrics evaluateSqlStatement(String sql, String dialect) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement is required");
        }
        if (dialect == null || dialect.trim().isEmpty()) {
            throw new IllegalArgumentException("Dialect is required");
        }

        try {
            SqlParser parser = getSqlParser(dialect);
            SqlStatement statement = parser.parse(sql);

            ComplexityEvaluator evaluator = getEvaluator(dialect);
            return evaluator.evaluateSqlStatement(statement);
        } catch (Exception e) {
            log.error("Failed to evaluate SQL statement", e);
            throw new Exception("Failed to parse SQL statement: " + e.getMessage(), e);
        }
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect) throws Exception {
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, null, null, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions) throws Exception {
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, null, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception {
        return evaluateStoredProcedure(sourceCode, name, schema, dialect, customFunctions, highWeightTables, null);
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Source code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Procedure name is required");
        }
        if (dialect == null || dialect.trim().isEmpty()) {
            throw new IllegalArgumentException("Dialect is required");
        }

        try {
            String effectiveSchema = (schema != null && !schema.trim().isEmpty()) ? schema.trim() : "HR";

            ComplexityEvaluator evaluator = getEvaluator(dialect);

            // Configure evaluator with optional parameters
            if (evaluator instanceof GaussComplexityEvaluator) {
                GaussComplexityEvaluator gaussEvaluator = (GaussComplexityEvaluator) evaluator;
                if (customFunctions != null) {
                    gaussEvaluator.setCustomFunctions(customFunctions);
                }
                if (highWeightTables != null) {
                    gaussEvaluator.setHighWeightTables(highWeightTables);
                }
                if (highWeightProcedures != null) {
                    gaussEvaluator.setHighWeightProcedures(highWeightProcedures);
                }
            } else if (evaluator instanceof OracleComplexityEvaluator) {
                OracleComplexityEvaluator oracleEvaluator = (OracleComplexityEvaluator) evaluator;
                if (customFunctions != null) {
                    oracleEvaluator.setCustomFunctions(customFunctions);
                }
                if (highWeightTables != null) {
                    oracleEvaluator.setHighWeightTables(highWeightTables);
                }
                if (highWeightProcedures != null) {
                    oracleEvaluator.setHighWeightProcedures(highWeightProcedures);
                }
            } else if (evaluator instanceof HiveComplexityEvaluator) {
                HiveComplexityEvaluator hiveEvaluator = (HiveComplexityEvaluator) evaluator;
                if (customFunctions != null) {
                    hiveEvaluator.setCustomFunctions(customFunctions);
                }
                if (highWeightTables != null) {
                    hiveEvaluator.setHighWeightTables(highWeightTables);
                }
                if (highWeightProcedures != null) {
                    hiveEvaluator.setHighWeightProcedures(highWeightProcedures);
                }
            }

            // Parse stored procedure using appropriate parser
            com.sdchat.ce.sp.complexity.parser.StoredProcedureParser parser = getStoredProcedureParser(dialect);
            StoredProcedure procedure = parser.parse(sourceCode, name, effectiveSchema);

            // Evaluate complexity
            ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedure);

            return metrics;
        } catch (Exception e) {
            log.error("Failed to evaluate stored procedure", e);
            throw new Exception("Failed to parse stored procedure: " + e.getMessage(), e);
        }
    }

    @Override
    public ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect) throws Exception {
        return evaluatePackageBody(sourceCode, packageName, schema, dialect, null, null, null);
    }

    @Override
    public ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions) throws Exception {
        return evaluatePackageBody(sourceCode, packageName, schema, dialect, customFunctions, null, null);
    }

    @Override
    public ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception {
        return evaluatePackageBody(sourceCode, packageName, schema, dialect, customFunctions, highWeightTables, null);
    }

    @Override
    public ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Source code is required");
        }
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }
        if (dialect == null || dialect.trim().isEmpty()) {
            throw new IllegalArgumentException("Dialect is required");
        }

        try {
            String effectiveSchema = (schema != null && !schema.trim().isEmpty()) ? schema.trim() : "HR";

            com.sdchat.ce.sp.complexity.parser.StoredProcedureParser parser = getStoredProcedureParser(dialect);

            // Parse package body to get all procedures
            List<StoredProcedure> procedures = parser.parsePackageBody(sourceCode, packageName, effectiveSchema);

            ComplexityMetricsCollection collection = new ComplexityMetricsCollection();
            collection.setPackageName(packageName);
            collection.setSchema(effectiveSchema);

            List<ComplexityMetrics> metricsList = new java.util.ArrayList<>();

            // Evaluate each procedure
            for (StoredProcedure procedure : procedures) {
                ComplexityMetrics metrics = evaluateStoredProcedure(
                        procedure.getSourceCode(),
                        procedure.getName(),
                        effectiveSchema,
                        dialect,
                        customFunctions,
                        highWeightTables,
                        highWeightProcedures
                );
                metricsList.add(metrics);
            }

            collection.setProcedures(metricsList);
            return collection;
        } catch (Exception e) {
            log.error("Failed to evaluate package body", e);
            throw new Exception("Failed to parse package body: " + e.getMessage(), e);
        }
    }

    private com.sdchat.ce.sp.complexity.parser.StoredProcedureParser getStoredProcedureParser(String dialect) {
        String dialectUpper = dialect.trim().toUpperCase();
        switch (dialectUpper) {
            case "ORACLE":
                return new OracleStoredProcedureParser(oracleSqlParser);
            case "GAUSS":
                return new GaussStoredProcedureParser(gaussSqlParser);
            case "HIVE":
                return new HiveStoredProcedureParser(hiveSqlParser);
            default:
                throw new IllegalArgumentException("Unsupported dialect: " + dialect);
        }
    }
}
