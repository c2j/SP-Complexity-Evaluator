package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Gauss complexity evaluator implementation.
 */
@Slf4j
@Component
public class GaussComplexityEvaluator implements ComplexityEvaluator {

    private static final String DIALECT = "Gauss";

    // Weights for different SQL constructs
    private static final double TABLE_WEIGHT = 1.0;
    private static final double JOIN_WEIGHT = 2.0;
    private static final double WHERE_CONDITION_WEIGHT = 1.0;
    private static final double SUBQUERY_WEIGHT = 3.0;
    private static final double AGGREGATE_FUNCTION_WEIGHT = 1.5;
    private static final double CASE_EXPRESSION_WEIGHT = 1.5;
    private static final double SET_OPERATION_WEIGHT = 2.0;
    private static final double LOOP_WEIGHT = 2.0;
    private static final double NESTED_LOOP_WEIGHT = 3.0;
    private static final double CUSTOM_FUNCTION_WEIGHT = 1.5;
    private static final double HIGH_WEIGHT_TABLE_WEIGHT = 2.0;
    private static final double HIGH_WEIGHT_PROCEDURE_WEIGHT = 2.5;
    private static final double NESTED_PROCEDURE_WEIGHT = 3.0;

    // Patterns for identifying SQL constructs
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\b(JOIN)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("\\(\\s*SELECT", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGGREGATE_FUNCTION_PATTERN = Pattern.compile("\\b(COUNT|SUM|AVG|MIN|MAX)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASE_EXPRESSION_PATTERN = Pattern.compile("\\bCASE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_OPERATION_PATTERN = Pattern.compile("\\b(UNION|INTERSECT|MINUS|EXCEPT)\\b", Pattern.CASE_INSENSITIVE);
    // Pattern for identifying loop constructs (used in the removeComments method)
    // 更精确的嵌套存储过程调用模式，匹配完整的过程调用，包括参数和结束分号
    private static final Pattern NESTED_PROCEDURE_PATTERN = Pattern.compile("\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*;", Pattern.CASE_INSENSITIVE);
    // 匹配存储过程调用，但不要求结束分号（用于嵌套调用）
    private static final Pattern NESTED_PROCEDURE_NO_SEMICOLON_PATTERN = Pattern.compile("\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)(?!\\s*\\()", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXECUTE_IMMEDIATE_PATTERN = Pattern.compile("\\bEXECUTE\\s+IMMEDIATE\\b", Pattern.CASE_INSENSITIVE);

    // List of custom functions to check for
    private List<String> customFunctions = new ArrayList<>();

    // List of high-weight tables to check for
    private List<String> highWeightTables = new ArrayList<>();

    // List of high-weight stored procedures to check for
    private List<String> highWeightProcedures = new ArrayList<>();

    /**
     * Set the list of custom functions to check for.
     *
     * @param customFunctions The list of custom function names
     */
    public void setCustomFunctions(List<String> customFunctions) {
        this.customFunctions = customFunctions != null ? customFunctions : new ArrayList<>();
    }

    /**
     * Set the list of high-weight tables to check for.
     *
     * @param highWeightTables The list of high-weight table names
     */
    public void setHighWeightTables(List<String> highWeightTables) {
        this.highWeightTables = highWeightTables != null ? highWeightTables : new ArrayList<>();
    }

    /**
     * Set the list of high-weight stored procedures to check for.
     *
     * @param highWeightProcedures The list of high-weight stored procedure names
     */
    public void setHighWeightProcedures(List<String> highWeightProcedures) {
        if (highWeightProcedures == null || highWeightProcedures.isEmpty()) {
            this.highWeightProcedures = new ArrayList<>();
            return;
        }

        // Convert all procedure names to uppercase for case-insensitive comparison
        this.highWeightProcedures = highWeightProcedures.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    @Override
    public ComplexityMetrics evaluateSqlStatement(SqlStatement statement) throws Exception {
        if ("DYNAMIC_SQL".equals(statement.getType())) {
            // For dynamic SQL statements, use the table list from the statement
            return evaluateDynamicSqlStatement(statement);
        } else if (!"SELECT".equals(statement.getType())) {
            // For non-SELECT statements, use a simplified evaluation
            return evaluateNonSelectStatement(statement);
        }

        return evaluateSelectStatement(statement.getSql());
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(StoredProcedure procedure) throws Exception {
        List<SqlStatement> statements = procedure.getSqlStatements();
        if (statements == null || statements.isEmpty()) {
            return ComplexityMetrics.builder()
                    .overallScore(0)
                    .build();
        }

        // Evaluate each SQL statement
        List<ComplexityMetrics> statementMetrics = new ArrayList<>();
        for (SqlStatement statement : statements) {
            try {
                ComplexityMetrics metrics = evaluateSqlStatement(statement);
                statementMetrics.add(metrics);
            } catch (Exception e) {
                log.warn("Failed to evaluate statement in Gauss stored procedure: {}", statement.getSql(), e);
            }
        }

        // Calculate overall metrics
        double overallScore = statementMetrics.stream()
                .mapToDouble(ComplexityMetrics::getOverallScore)
                .sum();

        int tableCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getTableCount)
                .sum();

        List<String> tableList = statementMetrics.stream()
                .filter(m -> m.getTableList() != null)
                .flatMap(m -> m.getTableList().stream())
                .distinct()
                .collect(Collectors.toList());

        // Special case for DELETE statements without FROM clause
        if (procedure.getSourceCode() != null) {
            Pattern deletePattern = Pattern.compile("\\bDELETE\\s+([\\w\\.]+)\\s+WHERE\\b", Pattern.CASE_INSENSITIVE);
            Matcher deleteMatcher = deletePattern.matcher(procedure.getSourceCode());
            while (deleteMatcher.find()) {
                String tableName = deleteMatcher.group(1);
                if (tableName != null && !tableList.contains(tableName)) {
                    tableList.add(tableName);
                    tableCount++;
                }
            }
        }

        int joinCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getJoinCount)
                .sum();

        int whereConditionCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getWhereConditionCount)
                .sum();

        int subqueryCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getSubqueryCount)
                .sum();

        int aggregateFunctionCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getAggregateFunctionCount)
                .sum();

        int caseExpressionCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getCaseExpressionCount)
                .sum();

        int setOperationCount = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getSetOperationCount)
                .sum();

        int queryDepth = statementMetrics.stream()
                .mapToInt(ComplexityMetrics::getQueryDepth)
                .max()
                .orElse(0);

        // Calculate line count
        int lineCount = 0;
        if (procedure.getSourceCode() != null) {
            // 使用更可靠的方法计算行数
            String[] lines = procedure.getSourceCode().split("\r?\n");
            lineCount = lines.length;

            // 处理空字符串的情况
            if (procedure.getSourceCode().trim().isEmpty()) {
                lineCount = 0;
            }

            // 确保行数至少为1
            if (lineCount == 0 && !procedure.getSourceCode().trim().isEmpty()) {
                lineCount = 1;
            }

            // 打印行数计算信息，用于调试
            log.debug("Calculated {} lines for procedure {}", lineCount, procedure.getName());
        }

        // Count loops and nested loops
        int loopCount = 0;
        int maxLoopNestingLevel = 0;
        if (procedure.getSourceCode() != null) {
            // Remove comments from the source code before counting loops
            String sourceCodeWithoutComments = removeComments(procedure.getSourceCode());

            // Define patterns for different types of loops
            Pattern forLoopPattern = Pattern.compile("\\bFOR\\b[^;]*?\\bLOOP\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern whileLoopPattern = Pattern.compile("\\bWHILE\\b[^;]*?\\bLOOP\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern simpleLoopPattern = Pattern.compile("\\bLOOP\\b(?!\\s*\\w)", Pattern.CASE_INSENSITIVE);

            // Count each type of loop
            Matcher forMatcher = forLoopPattern.matcher(sourceCodeWithoutComments);
            while (forMatcher.find()) {
                loopCount++;
            }

            Matcher whileMatcher = whileLoopPattern.matcher(sourceCodeWithoutComments);
            while (whileMatcher.find()) {
                loopCount++;
            }

            Matcher simpleMatcher = simpleLoopPattern.matcher(sourceCodeWithoutComments);
            while (simpleMatcher.find()) {
                // Make sure this isn't part of "END LOOP"
                String beforeLoop = sourceCodeWithoutComments.substring(Math.max(0, simpleMatcher.start() - 5), simpleMatcher.start());
                if (!beforeLoop.toUpperCase().contains("END")) {
                    loopCount++;
                }
            }

            // Calculate max loop nesting level by tracking loop depth
            // This is a more accurate approach that counts actual nesting
            String[] lines = sourceCodeWithoutComments.split("\n");
            int currentNestingLevel = 0;
            for (String line : lines) {
                String upperLine = line.toUpperCase();

                // Check for loop starts (but not if they're in an END LOOP statement)
                if ((upperLine.contains(" FOR ") || upperLine.contains("FOR ")) && upperLine.contains(" LOOP") && !upperLine.contains("END LOOP") ||
                    (upperLine.contains(" WHILE ") || upperLine.contains("WHILE ")) && upperLine.contains(" LOOP") && !upperLine.contains("END LOOP") ||
                    upperLine.matches(".*\\bLOOP\\b(?!\\s*\\w).*") && !upperLine.contains("END LOOP")) {
                    currentNestingLevel++;
                    maxLoopNestingLevel = Math.max(maxLoopNestingLevel, currentNestingLevel);
                }

                // Check for loop ends
                if (upperLine.contains("END LOOP")) {
                    currentNestingLevel = Math.max(0, currentNestingLevel - 1);
                }
            }

            log.debug("Found {} loops with max nesting level {} in procedure {}", loopCount, maxLoopNestingLevel, procedure.getName());
        }

        // Add loop complexity to overall score
        overallScore += (loopCount * LOOP_WEIGHT) + (maxLoopNestingLevel * NESTED_LOOP_WEIGHT);

        // Count custom function calls
        int customFunctionCount = 0;
        List<String> customFunctionList = new ArrayList<>();
        Map<String, Integer> customFunctionCounts = new HashMap<>();

        if (!customFunctions.isEmpty() && procedure.getSourceCode() != null) {
            for (String function : customFunctions) {
                Pattern functionPattern = Pattern.compile("\\b" + function + "\\s*\\(", Pattern.CASE_INSENSITIVE);
                Matcher functionMatcher = functionPattern.matcher(procedure.getSourceCode());
                int count = 0;
                while (functionMatcher.find()) {
                    count++;
                }
                if (count > 0) {
                    customFunctionCount += count;
                    customFunctionList.add(function);
                    customFunctionCounts.put(function, count);
                }
            }
        }

        // Add custom function complexity to overall score
        overallScore += customFunctionCount * CUSTOM_FUNCTION_WEIGHT;

        // Count high-weight table references
        int highWeightTableCount = 0;
        List<String> highWeightTableList = new ArrayList<>();
        Map<String, Integer> highWeightTableCounts = new HashMap<>();

        if (!highWeightTables.isEmpty()) {
            // Find all SQL statements that reference high-weight tables
            for (SqlStatement statement : statements) {
                String sql = statement.getSql().toUpperCase();

                for (String highWeightTable : highWeightTables) {
                    // Check if this statement references the high-weight table
                    if (containsTable(sql, highWeightTable)) {
                        highWeightTableCount++;

                        // Add to the list if not already present
                        if (!highWeightTableList.contains(highWeightTable)) {
                            highWeightTableList.add(highWeightTable);
                        }

                        // Increment the count for this table
                        highWeightTableCounts.put(highWeightTable,
                            highWeightTableCounts.getOrDefault(highWeightTable, 0) + 1);
                    }
                }
            }
        }

        // Add high-weight table complexity to overall score
        overallScore += highWeightTableCount * HIGH_WEIGHT_TABLE_WEIGHT;

        // Count nested stored procedure calls
        int nestedProcedureCount = 0;
        List<String> nestedProcedureList = new ArrayList<>();
        Map<String, Integer> nestedProcedureCounts = new HashMap<>();

        // Process high-weight stored procedures
        List<String> highWeightProcedureList = new ArrayList<>();
        Map<String, Integer> highWeightProcedureCounts = new HashMap<>();
        int highWeightProcedureCount = 0;

        if (procedure.getSourceCode() != null) {
            // 创建自定义函数集合（转为大写）
            Set<String> customFunctionSet = customFunctions.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

            // 存储过程名称的正则表达式，用于过滤出可能的存储过程名称
            // 允许多个点号分隔的名称，如 pkg_name.proc_name 或 schema.pkg_name.proc_name
            Pattern procedureNamePattern = Pattern.compile("^[A-Z][A-Z0-9_]*(\\.[A-Z][A-Z0-9_]*)*$");

            // 查找带分号的存储过程调用
            Matcher procMatcher = NESTED_PROCEDURE_PATTERN.matcher(procedure.getSourceCode().toUpperCase());
            while (procMatcher.find()) {
                String procName = procMatcher.group(1);

                // 排除内置函数和自定义函数
                if (!isBuiltInFunction(procName) && !customFunctionSet.contains(procName)) {
                    // 验证过程名称格式，确保它符合命名规范
                    if (!procedureNamePattern.matcher(procName).matches()) {
                        continue;
                    }

                    // 排除存储过程自身的名称（不区分大小写）
                    if (procedure.getName() != null && procName.equalsIgnoreCase(procedure.getName())) {
                        continue;
                    }

                    nestedProcedureCount++;

                    // 更新过程调用计数
                    nestedProcedureCounts.put(procName,
                        nestedProcedureCounts.getOrDefault(procName, 0) + 1);

                    // 添加到过程列表（如果尚未存在）
                    if (!nestedProcedureList.contains(procName)) {
                        nestedProcedureList.add(procName);
                    }

                    // 检查是否是高权重存储过程（不区分大小写）
                    boolean isHighWeight = false;
                    for (String highWeightProc : highWeightProcedures) {
                        if (procName.equalsIgnoreCase(highWeightProc)) {
                            isHighWeight = true;
                            break;
                        }
                    }

                    if (isHighWeight) {
                        highWeightProcedureCount++;

                        // 添加到高权重过程列表（如果尚未存在）
                        if (!highWeightProcedureList.contains(procName)) {
                            highWeightProcedureList.add(procName);
                        }

                        // 更新高权重过程调用计数
                        highWeightProcedureCounts.put(procName,
                            highWeightProcedureCounts.getOrDefault(procName, 0) + 1);
                    }
                }
            }

            // 查找不带分号的存储过程调用（可能是嵌套调用）
            procMatcher = NESTED_PROCEDURE_NO_SEMICOLON_PATTERN.matcher(procedure.getSourceCode().toUpperCase());
            while (procMatcher.find()) {
                String procName = procMatcher.group(1);

                // 排除内置函数和自定义函数
                if (!isBuiltInFunction(procName) && !customFunctionSet.contains(procName)) {
                    // 验证过程名称格式，确保它符合命名规范
                    if (!procedureNamePattern.matcher(procName).matches()) {
                        continue;
                    }

                    // 排除存储过程自身的名称（不区分大小写）
                    if (procedure.getName() != null && procName.equalsIgnoreCase(procedure.getName())) {
                        continue;
                    }

                    nestedProcedureCount++;

                    // 更新过程调用计数
                    nestedProcedureCounts.put(procName,
                        nestedProcedureCounts.getOrDefault(procName, 0) + 1);

                    // 添加到过程列表（如果尚未存在）
                    if (!nestedProcedureList.contains(procName)) {
                        nestedProcedureList.add(procName);
                    }

                    // 检查是否是高权重存储过程（不区分大小写）
                    boolean isHighWeight = false;
                    for (String highWeightProc : highWeightProcedures) {
                        if (procName.equalsIgnoreCase(highWeightProc)) {
                            isHighWeight = true;
                            break;
                        }
                    }

                    if (isHighWeight) {
                        highWeightProcedureCount++;

                        // 添加到高权重过程列表（如果尚未存在）
                        if (!highWeightProcedureList.contains(procName)) {
                            highWeightProcedureList.add(procName);
                        }

                        // 更新高权重过程调用计数
                        highWeightProcedureCounts.put(procName,
                            highWeightProcedureCounts.getOrDefault(procName, 0) + 1);
                    }
                }
            }

            // 检查 EXECUTE IMMEDIATE 语句（可能包含动态 SQL）
            int executeImmediateCount = 0;
            Matcher execMatcher = EXECUTE_IMMEDIATE_PATTERN.matcher(procedure.getSourceCode());
            while (execMatcher.find()) {
                executeImmediateCount++;
            }

            // 如果有 EXECUTE IMMEDIATE 语句，添加到嵌套过程计数
            if (executeImmediateCount > 0) {
                nestedProcedureCount += executeImmediateCount;
                nestedProcedureList.add("EXECUTE_IMMEDIATE");
                nestedProcedureCounts.put("EXECUTE_IMMEDIATE", executeImmediateCount);
            }
        }

        // Add nested procedure complexity to overall score
        overallScore += nestedProcedureCount * NESTED_PROCEDURE_WEIGHT;

        // Add high-weight procedure complexity to overall score
        if (highWeightProcedureCount > 0) {
            overallScore += highWeightProcedureCount * HIGH_WEIGHT_PROCEDURE_WEIGHT;
        }

        // 创建额外指标映射
        Map<String, Object> additionalMetrics = new HashMap<>();

        // 添加嵌套过程调用计数映射
        if (!nestedProcedureCounts.isEmpty()) {
            additionalMetrics.put("nestedProcedureCounts", nestedProcedureCounts);
        }

        // 添加高权重过程调用计数映射
        if (!highWeightProcedureCounts.isEmpty()) {
            additionalMetrics.put("highWeightProcedureCounts", highWeightProcedureCounts);
        }

        return ComplexityMetrics.builder()
                .overallScore(overallScore)
                .tableCount(tableCount)
                .tableList(tableList)
                .joinCount(joinCount)
                .whereConditionCount(whereConditionCount)
                .subqueryCount(subqueryCount)
                .aggregateFunctionCount(aggregateFunctionCount)
                .caseExpressionCount(caseExpressionCount)
                .setOperationCount(setOperationCount)
                .queryDepth(queryDepth)
                .loopCount(loopCount)
                .maxLoopNestingLevel(maxLoopNestingLevel)
                .customFunctionCount(customFunctionCount)
                .customFunctionList(customFunctionList)
                .highWeightTableCount(highWeightTableCount)
                .highWeightTableList(highWeightTableList)
                .nestedProcedureCount(nestedProcedureCount)
                .nestedProcedureList(nestedProcedureList)
                .highWeightProcedureCount(highWeightProcedureCount)
                .highWeightProcedureList(highWeightProcedureList)
                .procedureName(procedure.getName())
                .lineCount(lineCount)
                .additionalMetrics(additionalMetrics)
                .build();
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Evaluate a SELECT statement.
     *
     * @param sql The SQL statement text
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateSelectStatement(String sql) {
        // Count tables
        int tableCount = 0;
        List<String> tableList = new ArrayList<>();
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            tableCount++;
            String tableName = tableMatcher.group(1);
            tableList.add(tableName);
        }

        // Count joins
        int joinCount = 0;
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            joinCount++;
        }

        // Count subqueries
        int subqueryCount = 0;
        Matcher subqueryMatcher = SUBQUERY_PATTERN.matcher(sql);
        while (subqueryMatcher.find()) {
            subqueryCount++;
        }

        // Count aggregate functions
        int aggregateFunctionCount = 0;
        Matcher aggregateMatcher = AGGREGATE_FUNCTION_PATTERN.matcher(sql);
        while (aggregateMatcher.find()) {
            aggregateFunctionCount++;
        }

        // Count CASE expressions
        int caseExpressionCount = 0;
        Matcher caseMatcher = CASE_EXPRESSION_PATTERN.matcher(sql);
        while (caseMatcher.find()) {
            caseExpressionCount++;
        }

        // Count set operations
        int setOperationCount = 0;
        Matcher setOpMatcher = SET_OPERATION_PATTERN.matcher(sql);
        while (setOpMatcher.find()) {
            setOperationCount++;
        }

        // Calculate query depth (simplified)
        int queryDepth = 1 + subqueryCount;

        // Count WHERE conditions (simplified)
        int whereConditionCount = sql.toUpperCase().contains("WHERE") ? 1 : 0;

        // Calculate overall complexity score
        double overallScore = (tableCount * TABLE_WEIGHT) +
                (joinCount * JOIN_WEIGHT) +
                (whereConditionCount * WHERE_CONDITION_WEIGHT) +
                (subqueryCount * SUBQUERY_WEIGHT) +
                (aggregateFunctionCount * AGGREGATE_FUNCTION_WEIGHT) +
                (caseExpressionCount * CASE_EXPRESSION_WEIGHT) +
                (setOperationCount * SET_OPERATION_WEIGHT);

        // Calculate line count
        int lineCount = 0;
        if (sql != null) {
            // 使用更可靠的方法计算行数
            String[] lines = sql.split("\r?\n");
            lineCount = lines.length;

            // 处理空字符串的情况
            if (sql.trim().isEmpty()) {
                lineCount = 0;
            }
        }

        return ComplexityMetrics.builder()
                .overallScore(overallScore)
                .tableCount(tableCount)
                .tableList(tableList)
                .joinCount(joinCount)
                .whereConditionCount(whereConditionCount)
                .subqueryCount(subqueryCount)
                .aggregateFunctionCount(aggregateFunctionCount)
                .caseExpressionCount(caseExpressionCount)
                .setOperationCount(setOperationCount)
                .queryDepth(queryDepth)
                .lineCount(lineCount)
                .build();
    }

    /**
     * Evaluate the complexity of a dynamic SQL statement.
     * This method uses the table list from the statement and assigns a complexity score
     * based on the number of tables and the statement length.
     *
     * @param statement The dynamic SQL statement to evaluate
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateDynamicSqlStatement(SqlStatement statement) {
        String sql = statement.getSql();
        int length = sql.length();

        // Get table list from the statement
        List<String> tableList = statement.getTableList() != null ? statement.getTableList() : new ArrayList<>();
        int tableCount = tableList.size();

        // If no tables were found, add a placeholder
        if (tableCount == 0) {
            tableCount = 1; // At least one table
            tableList.add("DYNAMIC_TABLE"); // Add a placeholder for dynamic tables
        }

        // Estimate complexity based on statement length and table count
        double baseScore = Math.log10(length) * 5;
        double overallScore = baseScore * (1 + 0.1 * tableCount);

        // Calculate line count
        int lineCount = 0;
        if (sql != null) {
            String[] lines = sql.split("\r?\n");
            lineCount = lines.length;

            if (sql.trim().isEmpty()) {
                lineCount = 0;
            }
        }

        return ComplexityMetrics.builder()
                .overallScore(overallScore)
                .tableCount(tableCount)
                .tableList(tableList)
                .lineCount(lineCount)
                .build();
    }

    /**
     * Evaluate a non-SELECT statement (INSERT, UPDATE, DELETE, etc.).
     *
     * @param statement The SQL statement
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateNonSelectStatement(SqlStatement statement) {
        String sql = statement.getSql();

        // Count tables (simplified)
        int tableCount = 0;
        List<String> tableList = new ArrayList<>();

        if ("INSERT".equals(statement.getType())) {
            Pattern insertTablePattern = Pattern.compile("\\bINTO\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = insertTablePattern.matcher(sql);
            if (tableMatcher.find()) {
                tableCount++;
                tableList.add(tableMatcher.group(1));
            }
        } else if ("UPDATE".equals(statement.getType())) {
            Pattern updateTablePattern = Pattern.compile("\\bUPDATE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = updateTablePattern.matcher(sql);
            if (tableMatcher.find()) {
                tableCount++;
                tableList.add(tableMatcher.group(1));
            }
        } else if ("DELETE".equals(statement.getType())) {
            // First try to find table with FROM clause
            Pattern deleteTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = deleteTablePattern.matcher(sql);
            if (tableMatcher.find()) {
                tableCount++;
                tableList.add(tableMatcher.group(1));
            } else {
                // Try to find table without FROM clause (directly after DELETE)
                Pattern deleteNoFromPattern = Pattern.compile("\\bDELETE\\s+([\\w\\.]+)\\s+WHERE\\b|\\bDELETE\\s+([\\w\\.]+)\\s*;", Pattern.CASE_INSENSITIVE);
                Matcher noFromMatcher = deleteNoFromPattern.matcher(sql);
                if (noFromMatcher.find()) {
                    // Group 1 is for the pattern with WHERE, Group 2 is for the pattern with semicolon
                    String tableName = noFromMatcher.group(1) != null ? noFromMatcher.group(1) : noFromMatcher.group(2);
                    if (tableName != null) {
                        tableCount++;
                        tableList.add(tableName.trim());
                    }
                } else {
                    // Try an even simpler pattern
                    Pattern simpleDeletePattern = Pattern.compile("\\bDELETE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                    Matcher simpleMatcher = simpleDeletePattern.matcher(sql);
                    if (simpleMatcher.find()) {
                        String tableName = simpleMatcher.group(1);
                        tableCount++;
                        tableList.add(tableName.trim());
                    }
                }
            }
        } else if ("MERGE".equals(statement.getType())) {
            Pattern mergeTablePattern = Pattern.compile("\\bINTO\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher tableMatcher = mergeTablePattern.matcher(sql);
            if (tableMatcher.find()) {
                tableCount++;
                tableList.add(tableMatcher.group(1));
            }

            Pattern mergeUsingTablePattern = Pattern.compile("\\bUSING\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher usingTableMatcher = mergeUsingTablePattern.matcher(sql);
            if (usingTableMatcher.find()) {
                tableCount++;
                tableList.add(usingTableMatcher.group(1));
            }
        }

        // Calculate a simple overall score based on statement type and table count
        double overallScore = tableCount * TABLE_WEIGHT;

        // Add complexity for WHERE clause
        int whereConditionCount = sql.toUpperCase().contains("WHERE") ? 1 : 0;

        // Calculate line count
        int lineCount = 0;
        if (sql != null) {
            // 使用更可靠的方法计算行数
            String[] lines = sql.split("\r?\n");
            lineCount = lines.length;

            // 处理空字符串的情况
            if (sql.trim().isEmpty()) {
                lineCount = 0;
            }
        }

        return ComplexityMetrics.builder()
                .overallScore(overallScore)
                .tableCount(tableCount)
                .tableList(tableList)
                .whereConditionCount(whereConditionCount)
                .lineCount(lineCount)
                .build();
    }

    /**
     * Check if a SQL statement contains a reference to a specific table.
     * This method checks for common SQL patterns that reference tables.
     *
     * @param sql The SQL statement text (in uppercase)
     * @param tableName The table name to check for (in uppercase)
     * @return True if the SQL statement references the table
     */
    private boolean containsTable(String sql, String tableName) {
        // Skip type definitions using %TYPE
        Pattern typePattern = Pattern.compile(tableName + "\\.[A-Z0-9_]+%TYPE", Pattern.CASE_INSENSITIVE);
        if (typePattern.matcher(sql).find()) {
            return false;
        }

        // Common SQL patterns that reference tables
        String[] patterns = {
            "FROM\\s+" + tableName + "\\b",                  // FROM table
            "JOIN\\s+" + tableName + "\\b",                  // JOIN table
            "INTO\\s+" + tableName + "\\b",                  // INSERT INTO table
            "UPDATE\\s+" + tableName + "\\b",                // UPDATE table
            "FROM\\s+" + tableName + "\\.",                  // FROM table.
            "JOIN\\s+" + tableName + "\\.",                  // JOIN table.
            "INTO\\s+" + tableName + "\\.",                  // INSERT INTO table.
            "UPDATE\\s+" + tableName + "\\.",                // UPDATE table.
            "FROM\\s+\\w+\\." + tableName + "\\b",         // FROM schema.table
            "JOIN\\s+\\w+\\." + tableName + "\\b",         // JOIN schema.table
            "INTO\\s+\\w+\\." + tableName + "\\b",         // INSERT INTO schema.table
            "UPDATE\\s+\\w+\\." + tableName + "\\b",       // UPDATE schema.table
            "DELETE\\s+" + tableName + "\\b"                 // DELETE table
        };

        // Check each pattern
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove comments from the source code.
     * This method removes both single-line comments (--) and multi-line comments.
     *
     * @param sourceCode The source code to process
     * @return The source code with comments removed
     */
    private String removeComments(String sourceCode) {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = sourceCode.split("\n");
        boolean inMultiLineComment = false;

        for (String line : lines) {
            // Handle multi-line comments
            if (inMultiLineComment) {
                int endCommentPos = line.indexOf("*/");
                if (endCommentPos >= 0) {
                    // End of multi-line comment found
                    inMultiLineComment = false;
                    // Add the rest of the line after the comment end
                    if (endCommentPos + 2 < line.length()) {
                        result.append(line.substring(endCommentPos + 2));
                    }
                    result.append("\n");
                } else {
                    // Still in multi-line comment, skip this line
                    result.append("\n");
                }
                continue;
            }

            // Check for multi-line comment start
            int startCommentPos = line.indexOf("/*");
            if (startCommentPos >= 0) {
                // Add the part before the comment
                result.append(line.substring(0, startCommentPos));

                // Check if the comment ends on the same line
                int endCommentPos = line.indexOf("*/", startCommentPos + 2);
                if (endCommentPos >= 0) {
                    // Comment ends on the same line
                    // Add the part after the comment
                    if (endCommentPos + 2 < line.length()) {
                        // Process the rest of the line (might contain more comments)
                        String restOfLine = line.substring(endCommentPos + 2);
                        // Recursively process the rest of the line
                        result.append(removeComments(restOfLine));
                    }
                } else {
                    // Comment continues to next line
                    inMultiLineComment = true;
                }
                result.append("\n");
                continue;
            }

            // Handle single-line comments
            int singleCommentPos = line.indexOf("--");
            if (singleCommentPos >= 0) {
                // Add only the part before the comment
                result.append(line.substring(0, singleCommentPos)).append("\n");
            } else {
                // No comments in this line
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Check if a function name is a built-in function.
     *
     * @param functionName The function name to check
     * @return True if it's a built-in function, false otherwise
     */
    private boolean isBuiltInFunction(String functionName) {
        // 转换为大写进行比较
        String upperFunctionName = functionName.toUpperCase();

        // Gauss 内置函数和包
        String[] builtIns = {
            // 常见内置函数
            "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "AVG", "CEIL", "CEILING",
            "COS", "COT", "COUNT", "EXP", "FLOOR", "GREATEST", "LEAST", "LN",
            "LOG", "MAX", "MIN", "MOD", "POWER", "ROUND", "SIGN", "SIN", "SQRT",
            "SUM", "TAN", "TRUNC", "UPPER", "LOWER", "SUBSTR", "SUBSTRING",
            "TRIM", "LTRIM", "RTRIM", "LENGTH", "REPLACE", "TO_CHAR", "TO_DATE",
            "TO_NUMBER", "NVL", "DECODE", "CAST", "COALESCE", "NULLIF", "SYSDATE",
            "CURRENT_DATE", "CURRENT_TIMESTAMP", "EXTRACT", "MONTHS_BETWEEN",

            // SQL 关键字，可能被误认为是存储过程
            "VALUES", "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE",
            "CREATE", "ALTER", "DROP", "TRUNCATE", "GRANT", "REVOKE",

            // PL/SQL 关键字
            "DECLARE", "BEGIN", "EXCEPTION", "END", "IF", "THEN", "ELSE", "ELSIF",
            "LOOP", "WHILE", "FOR", "EXIT", "CONTINUE", "RETURN", "GOTO",
            "EXCEPTION_INIT", "PRAGMA",

            // 数据类型
            "VARCHAR", "VARCHAR2", "CHAR", "NUMBER", "DATE", "TIMESTAMP", "BOOLEAN",
            "INTEGER", "FLOAT", "DOUBLE", "DECIMAL", "BINARY", "BLOB", "CLOB", "NCLOB",
            "RAW", "LONG", "LONG RAW", "ROWID", "UROWID", "REF", "CURSOR"
        };

        for (String builtIn : builtIns) {
            if (upperFunctionName.equals(builtIn) || upperFunctionName.startsWith(builtIn + ".")) {
                return true;
            }
        }

        return false;
    }
}
