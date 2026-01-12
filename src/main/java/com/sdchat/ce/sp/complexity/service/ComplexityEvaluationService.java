package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.ComplexityMetricsCollection;

import java.util.List;

public interface ComplexityEvaluationService {

    ComplexityMetrics evaluateSqlStatement(String sql, String dialect) throws Exception;

    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect) throws Exception;

    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions) throws Exception;

    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception;

    ComplexityMetrics evaluateStoredProcedure(String sourceCode, String name, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception;

    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect) throws Exception;

    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions) throws Exception;

    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables) throws Exception;

    ComplexityMetricsCollection evaluatePackageBody(String sourceCode, String packageName, String schema, String dialect, List<String> customFunctions, List<String> highWeightTables, List<String> highWeightProcedures) throws Exception;
}
