package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.DmlStatementMetrics;
import com.sdchat.ce.sp.complexity.model.ProcedureCallMetric;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hive complexity evaluator implementation.
 */
@Slf4j
@Component
public class HiveComplexityEvaluator implements ComplexityEvaluator {

    private static final String DIALECT = "Hive";

    // Weights for different SQL constructs
    private static final double TABLE_WEIGHT = 1.0;
    private static final double JOIN_WEIGHT = 2.0;
    private static final double WHERE_CONDITION_WEIGHT = 1.5;
    private static final double SUBQUERY_WEIGHT = 3.0;
    private static final double AGGREGATE_FUNCTION_WEIGHT = 1.0;
    private static final double CASE_EXPRESSION_WEIGHT = 1.5;
    private static final double SET_OPERATION_WEIGHT = 2.0;
    private static final double GROUP_BY_WEIGHT = 1.5;
    private static final double ORDER_BY_WEIGHT = 1.0;
    private static final double LATERAL_VIEW_WEIGHT = 2.0;
    private static final double DISTRIBUTE_BY_WEIGHT = 1.5;
    private static final double CLUSTER_BY_WEIGHT = 1.5;
    private static final double SORT_BY_WEIGHT = 1.0;
    private static final double PARTITION_WEIGHT = 1.5;
    private static final double WINDOW_FUNCTION_WEIGHT = 2.5;

    // Hive-specific weights for long statements and complex operations
    private static final double UNION_WEIGHT = 2.5;
    private static final double UNION_ALL_WEIGHT = 2.0;
    private static final double UNION_NESTING_MULTIPLIER = 1.5;
    private static final double LONG_STATEMENT_THRESHOLD = 1000; // characters
    private static final double VERY_LONG_STATEMENT_THRESHOLD = 5000; // characters
    private static final double LONG_STATEMENT_MULTIPLIER = 1.2;
    private static final double VERY_LONG_STATEMENT_MULTIPLIER = 1.5;
    private static final double LARGE_LINE_COUNT_THRESHOLD = 50; // lines
    private static final double VERY_LARGE_LINE_COUNT_THRESHOLD = 200; // lines
    private static final double LARGE_LINE_COUNT_MULTIPLIER = 1.3;
    private static final double VERY_LARGE_LINE_COUNT_MULTIPLIER = 1.8;
    private static final double WITH_CLAUSE_WEIGHT = 2.0;
    private static final double NESTED_WITH_MULTIPLIER = 1.4;

    // Patterns for SQL constructs
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\b(JOIN|INNER\\s+JOIN|LEFT\\s+JOIN|RIGHT\\s+JOIN|FULL\\s+JOIN|CROSS\\s+JOIN|LEFT\\s+OUTER\\s+JOIN|RIGHT\\s+OUTER\\s+JOIN|FULL\\s+OUTER\\s+JOIN)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("\\(\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGGREGATE_FUNCTION_PATTERN = Pattern.compile("\\b(COUNT|SUM|AVG|MIN|MAX|STDDEV|VARIANCE)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASE_EXPRESSION_PATTERN = Pattern.compile("\\bCASE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_OPERATION_PATTERN = Pattern.compile("\\b(UNION|UNION\\s+ALL|INTERSECT|EXCEPT|MINUS)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("\\bGROUP\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("\\bORDER\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LATERAL_VIEW_PATTERN = Pattern.compile("\\bLATERAL\\s+VIEW\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTRIBUTE_BY_PATTERN = Pattern.compile("\\bDISTRIBUTE\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLUSTER_BY_PATTERN = Pattern.compile("\\bCLUSTER\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SORT_BY_PATTERN = Pattern.compile("\\bSORT\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARTITION_PATTERN = Pattern.compile("\\bPARTITION\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOW_FUNCTION_PATTERN = Pattern.compile("\\bOVER\\s*\\(", Pattern.CASE_INSENSITIVE);

    // Enhanced patterns for Hive-specific complex operations
    private static final Pattern UNION_PATTERN = Pattern.compile("\\bUNION\\s+(?!ALL)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNION_ALL_PATTERN = Pattern.compile("\\bUNION\\s+ALL\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WITH_CLAUSE_PATTERN = Pattern.compile("\\bWITH\\s+([\\w_]+)\\s+AS\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern NESTED_WITH_PATTERN = Pattern.compile("\\bWITH\\s+[\\w_,\\s]+AS\\s*\\([^)]*\\bWITH\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Patterns for stored procedure constructs
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\b(FOR|WHILE|LOOP)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURSOR_PATTERN = Pattern.compile("\\bCURSOR\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURSOR_OPERATION_PATTERN = Pattern.compile("\\b(OPEN|FETCH|CLOSE)\\s+[\\w\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROCEDURE_CALL_PATTERN = Pattern.compile("\\bCALL\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXECUTE_IMMEDIATE_PATTERN = Pattern.compile("\\bEXECUTE\\s+IMMEDIATE\\b", Pattern.CASE_INSENSITIVE);

    // Regex patterns for procedure call tracking with loop context
    private static final Pattern PROCEDURE_CALL_SIMPLE = Pattern.compile("\\b([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE);

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
     * @param highWeightProcedures The list of high-weight procedure names
     */
    public void setHighWeightProcedures(List<String> highWeightProcedures) {
        this.highWeightProcedures = highWeightProcedures != null ? highWeightProcedures : new ArrayList<>();
    }

    @Override
    public ComplexityMetrics evaluateSqlStatement(SqlStatement statement) throws Exception {
        log.debug("Evaluating Hive SQL statement - Type: {}, SQL: {}", statement.getType(), statement.getSql());

        if ("DYNAMIC_SQL".equals(statement.getType())) {
            // For dynamic SQL statements, use the table list from the statement
            log.debug("Processing as dynamic SQL");
            return evaluateDynamicSqlStatement(statement);
        } else if (!"SELECT".equals(statement.getType())) {
            // For non-SELECT statements, use a simplified evaluation
            log.debug("Processing as non-SELECT statement");
            return evaluateNonSelectStatement(statement);
        }

        log.debug("Processing as SELECT statement");
        return evaluateSelectStatement(statement.getSql(), statement.getTableList());
    }

    @Override
    public ComplexityMetrics evaluateStoredProcedure(StoredProcedure procedure) throws Exception {
        List<SqlStatement> statements = procedure.getSqlStatements();
        if (statements == null || statements.isEmpty()) {
            return ComplexityMetrics.builder()
                    .overallScore(0)
                    .build();
        }

        // Get any failed statements from the collector
        List<String> failedStatements = new ArrayList<>(com.sdchat.ce.sp.complexity.parser.SqlParserExceptionCollector.getFailedStatements());
        boolean hasExceptions = !failedStatements.isEmpty();

        // Collect DML statements (INSERT, UPDATE, DELETE, MERGE) with their metrics
        List<DmlStatementMetrics> dmlStatements = new ArrayList<>();
        for (SqlStatement statement : statements) {
            String type = statement.getType();
            if ("INSERT".equals(type) || "UPDATE".equals(type) ||
                "DELETE".equals(type) || "MERGE".equals(type)) {
                try {
                    // Evaluate the DML statement to get its metrics
                    ComplexityMetrics metrics = evaluateSqlStatement(statement);
                    // Create a DmlStatementMetrics object that combines the statement and its metrics
                    DmlStatementMetrics dmlMetrics = DmlStatementMetrics.from(statement, metrics);
                    dmlStatements.add(dmlMetrics);
                } catch (Exception e) {
                    log.warn("Failed to evaluate DML statement in Hive stored procedure: {}", statement.getSql(), e);
                    failedStatements.add(statement.getSql());
                    hasExceptions = true;
                }
            }
        }

        // Evaluate each SQL statement
        List<ComplexityMetrics> statementMetrics = new ArrayList<>();
        for (SqlStatement statement : statements) {
            try {
                ComplexityMetrics metrics = evaluateSqlStatement(statement);
                statementMetrics.add(metrics);
            } catch (Exception e) {
                log.warn("Failed to evaluate statement in Hive stored procedure: {}", statement.getSql(), e);
                failedStatements.add(statement.getSql());
                hasExceptions = true;
            }
        }

        // Calculate overall metrics
        double overallScore = 0;
        int tableCount = 0;
        Set<String> tableSet = new HashSet<>();
        int joinCount = 0;
        int whereConditionCount = 0;
        int subqueryCount = 0;
        int aggregateFunctionCount = 0;
        int caseExpressionCount = 0;
        int setOperationCount = 0;
        int groupByCount = 0;
        int orderByCount = 0;
        int queryDepth = 0;
        int loopCount = 0;
        int maxLoopNestingLevel = 0;
        int customFunctionCount = 0;
        Set<String> customFunctionSet = new HashSet<>();
        int highWeightTableCount = 0;
        Set<String> highWeightTableSet = new HashSet<>();
        int nestedProcedureCount = 0;
        Set<String> nestedProcedureSet = new HashSet<>();
        int highWeightProcedureCount = 0;
        Set<String> highWeightProcedureSet = new HashSet<>();
        int cursorCount = 0;
        Set<String> cursorSet = new HashSet<>();
        int cursorOperationCount = 0;
        int maxCursorNestingLevel = 0;
        int lateralViewCount = 0;
        int distributeByCount = 0;
        int clusterByCount = 0;
        int sortByCount = 0;
        int partitionCount = 0;
        int windowFunctionCount = 0;

        // Combine metrics from all statements
        for (ComplexityMetrics metrics : statementMetrics) {
            overallScore += metrics.getOverallScore();

            if (metrics.getTableList() != null) {
                tableSet.addAll(metrics.getTableList());
            }

            joinCount += metrics.getJoinCount();
            whereConditionCount += metrics.getWhereConditionCount();
            subqueryCount += metrics.getSubqueryCount();
            aggregateFunctionCount += metrics.getAggregateFunctionCount();
            caseExpressionCount += metrics.getCaseExpressionCount();
            setOperationCount += metrics.getSetOperationCount();
            groupByCount += metrics.getGroupByCount();
            orderByCount += metrics.getOrderByCount();
            queryDepth = Math.max(queryDepth, metrics.getQueryDepth());
        }

        // Count tables
        tableCount = tableSet.size();

        // Extract custom function calls from the source code
        if (!customFunctions.isEmpty()) {
            for (String function : customFunctions) {
                Pattern functionPattern = Pattern.compile("\\b" + Pattern.quote(function) + "\\s*\\(", Pattern.CASE_INSENSITIVE);
                Matcher matcher = functionPattern.matcher(procedure.getSourceCode());
                while (matcher.find()) {
                    customFunctionSet.add(function);
                    customFunctionCount++;
                }
            }
        }

        // Extract high-weight table references from the source code
        if (!highWeightTables.isEmpty()) {
            for (String table : highWeightTables) {
                // Skip type definitions like 'v_all_acnt_info_base.acnt_id%TYPE'
                if (table.contains("%TYPE")) {
                    continue;
                }

                Pattern tablePattern = Pattern.compile("\\b" + Pattern.quote(table) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = tablePattern.matcher(procedure.getSourceCode());
                while (matcher.find()) {
                    highWeightTableSet.add(table);
                    highWeightTableCount++;
                }
            }
        }

        // Extract nested procedure calls from the source code
        Matcher procedureCallMatcher = PROCEDURE_CALL_PATTERN.matcher(procedure.getSourceCode());
        while (procedureCallMatcher.find()) {
            String calledProcedure = procedureCallMatcher.group(1);

            // Skip if the procedure is calling itself
            if (calledProcedure.equalsIgnoreCase(procedure.getName())) {
                continue;
            }

            nestedProcedureSet.add(calledProcedure);
            nestedProcedureCount++;

            // Check if this is a high-weight procedure
            if (highWeightProcedures != null) {
                for (String hwp : highWeightProcedures) {
                    if (calledProcedure.equalsIgnoreCase(hwp)) {
                        highWeightProcedureSet.add(hwp);
                        highWeightProcedureCount++;
                        break;
                    }
                }
            }
        }

        // Count loops and determine nesting level
        loopCount = countMatches(LOOP_PATTERN, procedure.getSourceCode());
        maxLoopNestingLevel = calculateMaxLoopNestingLevel(procedure.getSourceCode());

        // Count cursors and cursor operations
        Matcher cursorMatcher = CURSOR_PATTERN.matcher(procedure.getSourceCode());
        while (cursorMatcher.find()) {
            cursorCount++;
            // Extract cursor name (simplified)
            int endPos = procedure.getSourceCode().indexOf(";", cursorMatcher.end());
            if (endPos > 0) {
                String cursorDecl = procedure.getSourceCode().substring(cursorMatcher.start(), endPos);
                // Add to set (simplified)
                cursorSet.add(cursorDecl);
            }
        }

        cursorOperationCount = countMatches(CURSOR_OPERATION_PATTERN, procedure.getSourceCode());
        maxCursorNestingLevel = calculateMaxCursorNestingLevel(procedure.getSourceCode());

        // Count Hive-specific constructs
        lateralViewCount = countMatches(LATERAL_VIEW_PATTERN, procedure.getSourceCode());
        distributeByCount = countMatches(DISTRIBUTE_BY_PATTERN, procedure.getSourceCode());
        clusterByCount = countMatches(CLUSTER_BY_PATTERN, procedure.getSourceCode());
        sortByCount = countMatches(SORT_BY_PATTERN, procedure.getSourceCode());
        partitionCount = countMatches(PARTITION_PATTERN, procedure.getSourceCode());
        windowFunctionCount = countMatches(WINDOW_FUNCTION_PATTERN, procedure.getSourceCode());

        // Add Hive-specific weights to the overall score
        overallScore += (lateralViewCount * LATERAL_VIEW_WEIGHT) +
                (distributeByCount * DISTRIBUTE_BY_WEIGHT) +
                (clusterByCount * CLUSTER_BY_WEIGHT) +
                (sortByCount * SORT_BY_WEIGHT) +
                (partitionCount * PARTITION_WEIGHT) +
                (windowFunctionCount * WINDOW_FUNCTION_WEIGHT);

        // Calculate line count
        int lineCount = 0;
        if (procedure.getSourceCode() != null) {
            String[] lines = procedure.getSourceCode().split("\r?\n");
            lineCount = lines.length;
        }

        // Extract procedure calls with loop tracking
        int procedureCallCount = 0;
        List<ProcedureCallMetric> procedureCallDetails = new ArrayList<>();

        if (procedure.getSourceCode() != null) {
            Map<String, ProcedureCallMetric> procedureCallsWithLoop = extractProcedureCallsWithLoopTracking(
                    procedure.getSourceCode(),
                    customFunctions != null ? customFunctions : new ArrayList<>());

            procedureCallCount = procedureCallsWithLoop.values().stream()
                    .mapToInt(ProcedureCallMetric::getCallCount)
                    .sum();

            procedureCallDetails = new ArrayList<>(procedureCallsWithLoop.values());
            Collections.sort(procedureCallDetails, Comparator.comparing(ProcedureCallMetric::getProcedureName));
        }

        // Clear the exception collector after retrieving the failed statements
        List<String> failedStatementsToInclude = new ArrayList<>(failedStatements);
        com.sdchat.ce.sp.complexity.parser.SqlParserExceptionCollector.clear();

        return ComplexityMetrics.builder()
                .overallScore(overallScore)
                .tableCount(tableCount)
                .tableList(new ArrayList<>(tableSet))
                .joinCount(joinCount)
                .whereConditionCount(whereConditionCount)
                .subqueryCount(subqueryCount)
                .aggregateFunctionCount(aggregateFunctionCount)
                .caseExpressionCount(caseExpressionCount)
                .setOperationCount(setOperationCount)
                .groupByCount(groupByCount)
                .orderByCount(orderByCount)
                .queryDepth(queryDepth)
                .loopCount(loopCount)
                .maxLoopNestingLevel(maxLoopNestingLevel)
                .customFunctionCount(customFunctionCount)
                .customFunctionList(new ArrayList<>(customFunctionSet))
                .highWeightTableCount(highWeightTableCount)
                .highWeightTableList(new ArrayList<>(highWeightTableSet))
                .nestedProcedureCount(nestedProcedureCount)
                .nestedProcedureList(new ArrayList<>(nestedProcedureSet))
                .highWeightProcedureCount(highWeightProcedureCount)
                .highWeightProcedureList(new ArrayList<>(highWeightProcedureSet))
                .cursorCount(cursorCount)
                .cursorList(new ArrayList<>(cursorSet))
                .cursorOperationCount(cursorOperationCount)
                .maxCursorNestingLevel(maxCursorNestingLevel)
                .procedureName(procedure.getName())
                .lineCount(lineCount)
                .dmlStatements(dmlStatements)
                .failedStatements(failedStatementsToInclude)
                .hasExceptions(hasExceptions)
                .procedureCallCount(procedureCallCount)
                .procedureCallDetails(procedureCallDetails)
                .build();
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Evaluate a SELECT statement.
     *
     * @param sql The SQL statement
     * @param parsedTableList The table list already parsed by the SQL parser
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateSelectStatement(String sql, List<String> parsedTableList) {
        log.debug("Evaluating Hive SELECT statement: {}", sql);

        // Use the table list from the parser, or extract if not provided
        List<String> rawTableList = (parsedTableList != null && !parsedTableList.isEmpty())
            ? parsedTableList
            : extractTablesFromSql(sql);

        // Remove duplicates from table list
        List<String> tableList = rawTableList.stream().distinct().collect(Collectors.toList());
        int tableCount = tableList.size();
        log.debug("Found {} tables: {}", tableCount, tableList);

        // Count joins
        int joinCount = countMatches(JOIN_PATTERN, sql);
        log.debug("Found {} joins", joinCount);

        // Count WHERE clauses
        int whereConditionCount = countMatches(WHERE_PATTERN, sql);

        // Count subqueries
        int subqueryCount = countMatches(SUBQUERY_PATTERN, sql);

        // Count aggregate functions
        int aggregateFunctionCount = countMatches(AGGREGATE_FUNCTION_PATTERN, sql);
        log.debug("Found {} aggregate functions", aggregateFunctionCount);

        // Count CASE expressions
        int caseExpressionCount = countMatches(CASE_EXPRESSION_PATTERN, sql);

        // Count set operations (UNION, INTERSECT, etc.)
        int setOperationCount = countMatches(SET_OPERATION_PATTERN, sql);

        // Count GROUP BY clauses
        int groupByCount = countMatches(GROUP_BY_PATTERN, sql);
        log.debug("Found {} GROUP BY clauses", groupByCount);

        // Count ORDER BY clauses
        int orderByCount = countMatches(ORDER_BY_PATTERN, sql);
        log.debug("Found {} ORDER BY clauses", orderByCount);

        // Count Hive-specific constructs
        int lateralViewCount = countMatches(LATERAL_VIEW_PATTERN, sql);
        int distributeByCount = countMatches(DISTRIBUTE_BY_PATTERN, sql);
        int clusterByCount = countMatches(CLUSTER_BY_PATTERN, sql);
        int sortByCount = countMatches(SORT_BY_PATTERN, sql);
        int partitionCount = countMatches(PARTITION_PATTERN, sql);
        int windowFunctionCount = countMatches(WINDOW_FUNCTION_PATTERN, sql);

        // Count enhanced Hive-specific operations
        int unionCount = countMatches(UNION_PATTERN, sql);
        int unionAllCount = countMatches(UNION_ALL_PATTERN, sql);
        int withClauseCount = countMatches(WITH_CLAUSE_PATTERN, sql);
        int nestedWithCount = countMatches(NESTED_WITH_PATTERN, sql);

        log.debug("Found {} UNION operations", unionCount);
        log.debug("Found {} UNION ALL operations", unionAllCount);
        log.debug("Found {} WITH clauses", withClauseCount);
        log.debug("Found {} nested WITH clauses", nestedWithCount);

        // Calculate query depth (enhanced for UNION operations)
        int unionDepth = calculateUnionDepth(sql);
        int queryDepth = Math.max(1 + subqueryCount, unionDepth);

        // Calculate base complexity score
        double baseScore = (tableCount * TABLE_WEIGHT) +
                (joinCount * JOIN_WEIGHT) +
                (whereConditionCount * WHERE_CONDITION_WEIGHT) +
                (subqueryCount * SUBQUERY_WEIGHT) +
                (aggregateFunctionCount * AGGREGATE_FUNCTION_WEIGHT) +
                (caseExpressionCount * CASE_EXPRESSION_WEIGHT) +
                (setOperationCount * SET_OPERATION_WEIGHT) +
                (groupByCount * GROUP_BY_WEIGHT) +
                (orderByCount * ORDER_BY_WEIGHT) +
                (lateralViewCount * LATERAL_VIEW_WEIGHT) +
                (distributeByCount * DISTRIBUTE_BY_WEIGHT) +
                (clusterByCount * CLUSTER_BY_WEIGHT) +
                (sortByCount * SORT_BY_WEIGHT) +
                (partitionCount * PARTITION_WEIGHT) +
                (windowFunctionCount * WINDOW_FUNCTION_WEIGHT);

        // Add enhanced Hive-specific complexity
        double hiveSpecificScore = (unionCount * UNION_WEIGHT) +
                (unionAllCount * UNION_ALL_WEIGHT) +
                (withClauseCount * WITH_CLAUSE_WEIGHT) +
                (nestedWithCount * WITH_CLAUSE_WEIGHT * NESTED_WITH_MULTIPLIER);

        // Apply UNION nesting multiplier if there are multiple UNION operations
        if (unionCount + unionAllCount > 1) {
            double unionMultiplier = Math.pow(UNION_NESTING_MULTIPLIER, Math.min(unionCount + unionAllCount - 1, 5));
            hiveSpecificScore *= unionMultiplier;
            log.debug("Applied UNION nesting multiplier: {}", unionMultiplier);
        }

        double overallScore = baseScore + hiveSpecificScore;

        // Calculate line count
        int lineCount = 0;
        if (sql != null) {
            String[] lines = sql.split("\r?\n");
            lineCount = lines.length;

            if (sql.trim().isEmpty()) {
                lineCount = 0;
            }
        }

        // Apply long statement and large line count multipliers (Hive-specific)
        double lengthMultiplier = calculateLengthComplexityMultiplier(sql, lineCount);
        overallScore *= lengthMultiplier;

        if (lengthMultiplier > 1.0) {
            log.debug("Applied length complexity multiplier: {} (SQL length: {}, Line count: {})",
                     lengthMultiplier, sql.length(), lineCount);
        }

        log.debug("Final calculated overall score: {}", overallScore);

        // Calculate additional metrics for JSON response
        int totalUnionCount = unionCount + unionAllCount;
        double unionMultiplier = (totalUnionCount > 1) ? Math.pow(UNION_NESTING_MULTIPLIER, Math.min(totalUnionCount - 1, 5)) : 1.0;
        int characterCount = sql != null ? sql.length() : 0;
        boolean isLongStatement = characterCount > LONG_STATEMENT_THRESHOLD;
        boolean isVeryLongStatement = characterCount > VERY_LONG_STATEMENT_THRESHOLD;
        boolean hasLargeLineCount = lineCount > LARGE_LINE_COUNT_THRESHOLD;
        boolean hasVeryLargeLineCount = lineCount > VERY_LARGE_LINE_COUNT_THRESHOLD;

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
                .groupByCount(groupByCount)
                .orderByCount(orderByCount)
                .queryDepth(queryDepth)
                .lineCount(lineCount)
                // Hive-specific metrics
                .unionCount(unionCount)
                .unionAllCount(unionAllCount)
                .totalUnionCount(totalUnionCount)
                .unionDepth(unionDepth)
                .withClauseCount(withClauseCount)
                .nestedWithCount(nestedWithCount)
                .lateralViewCount(lateralViewCount)
                .distributeByCount(distributeByCount)
                .clusterByCount(clusterByCount)
                .sortByCount(sortByCount)
                .partitionByCount(partitionCount)
                .windowFunctionCount(windowFunctionCount)
                .lengthComplexityMultiplier(lengthMultiplier)
                .unionNestingMultiplier(unionMultiplier)
                .isLongStatement(isLongStatement)
                .isVeryLongStatement(isVeryLongStatement)
                .hasLargeLineCount(hasLargeLineCount)
                .hasVeryLargeLineCount(hasVeryLargeLineCount)
                .characterCount(characterCount)
                .build();
    }

    /**
     * Evaluate a dynamic SQL statement.
     *
     * @param statement The SQL statement
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateDynamicSqlStatement(SqlStatement statement) {
        String sql = statement.getSql();
        List<String> tableList = statement.getTableList() != null ? statement.getTableList() : new ArrayList<>();
        int tableCount = tableList.size();
        int length = sql.length();

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
     * Evaluate a non-SELECT statement.
     *
     * @param statement The SQL statement
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateNonSelectStatement(SqlStatement statement) {
        String sql = statement.getSql();
        List<String> tableList = statement.getTableList() != null ? statement.getTableList() : new ArrayList<>();
        int tableCount = tableList.size();

        // Calculate a simple overall score based on statement type and table count
        double overallScore = tableCount * TABLE_WEIGHT;

        // Add complexity for WHERE clause
        int whereConditionCount = sql.toUpperCase().contains("WHERE") ? 1 : 0;

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
                .whereConditionCount(whereConditionCount)
                .lineCount(lineCount)
                .build();
    }

    /**
     * Extract table names from a SQL statement.
     *
     * @param sql The SQL statement
     * @return A list of table names
     */
    private List<String> extractTablesFromSql(String sql) {
        List<String> tableNames = new ArrayList<>();

        // Extract tables from WITH clauses (CTEs)
        Pattern withTablePattern = Pattern.compile("\\bWITH\\s+([\\w\\.]+)\\s+AS\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher withMatcher = withTablePattern.matcher(sql);
        while (withMatcher.find()) {
            tableNames.add(withMatcher.group(1));
        }

        // Extract tables from FROM clauses
        Pattern fromTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = fromTablePattern.matcher(sql);
        while (tableMatcher.find()) {
            tableNames.add(tableMatcher.group(1));
        }

        // Extract tables from various JOIN types (improved pattern)
        Pattern joinTablePattern = Pattern.compile("\\b(?:INNER\\s+|LEFT\\s+|RIGHT\\s+|FULL\\s+|CROSS\\s+|LEFT\\s+OUTER\\s+|RIGHT\\s+OUTER\\s+|FULL\\s+OUTER\\s+)?JOIN\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinTablePattern.matcher(sql);
        while (joinMatcher.find()) {
            tableNames.add(joinMatcher.group(1));
        }

        // Extract tables from INSERT INTO clauses (including Hive INSERT OVERWRITE)
        Pattern insertTablePattern = Pattern.compile("\\b(?:INTO|OVERWRITE\\s+TABLE)\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertTablePattern.matcher(sql);
        while (insertMatcher.find()) {
            tableNames.add(insertMatcher.group(1));
        }

        // Extract tables from UPDATE clauses
        Pattern updateTablePattern = Pattern.compile("\\bUPDATE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher updateMatcher = updateTablePattern.matcher(sql);
        while (updateMatcher.find()) {
            tableNames.add(updateMatcher.group(1));
        }

        // Extract tables from DELETE clauses
        Pattern deleteTablePattern = Pattern.compile("\\bDELETE\\s+FROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher deleteMatcher = deleteTablePattern.matcher(sql);
        while (deleteMatcher.find()) {
            tableNames.add(deleteMatcher.group(1));
        }

        // Extract tables from CREATE TABLE AS SELECT
        Pattern createTablePattern = Pattern.compile("\\bCREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher createMatcher = createTablePattern.matcher(sql);
        while (createMatcher.find()) {
            tableNames.add(createMatcher.group(1));
        }

        // Remove duplicates
        return tableNames.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Extract table names from a dynamic SQL statement.
     *
     * @param sql The dynamic SQL statement
     * @return A list of table names
     */
    private List<String> extractTablesFromDynamicSql(String sql) {
        List<String> tableNames = new ArrayList<>();

        // Split the dynamic SQL into parts (by string concatenation)
        String[] parts = sql.split("\\|\\|");

        // Process each part
        for (String part : parts) {
            // Remove quotes and trim
            part = part.replace("'", "").trim();

            // Look for common SQL patterns that reference tables
            if (part.toUpperCase().contains(" FROM ")) {
                // Extract table name after FROM
                int fromIndex = part.toUpperCase().indexOf(" FROM ");
                String afterFrom = part.substring(fromIndex + 6).trim();

                // Extract the table name (until the next space or end of string)
                int spaceIndex = afterFrom.indexOf(" ");
                String tableName = spaceIndex > 0 ? afterFrom.substring(0, spaceIndex) : afterFrom;

                if (!tableName.isEmpty()) {
                    tableNames.add(tableName);
                }
            }
        }

        return tableNames;
    }

    /**
     * Count the number of matches for a pattern in a string.
     *
     * @param pattern The pattern to match
     * @param str The string to search
     * @return The number of matches
     */
    private int countMatches(Pattern pattern, String str) {
        if (str == null) {
            return 0;
        }

        Matcher matcher = pattern.matcher(str);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Calculate the maximum nesting level of loops in a stored procedure.
     *
     * @param sourceCode The stored procedure source code
     * @return The maximum nesting level
     */
    private int calculateMaxLoopNestingLevel(String sourceCode) {
        if (sourceCode == null) {
            return 0;
        }

        // Convert to uppercase and split into lines
        String[] lines = sourceCode.toUpperCase().split("\r?\n");

        int currentNestingLevel = 0;
        int maxNestingLevel = 0;

        for (String line : lines) {
            line = line.trim();

            // Check for loop start
            if (line.contains("FOR ") || line.contains("WHILE ") || line.contains("LOOP")) {
                currentNestingLevel++;
                maxNestingLevel = Math.max(maxNestingLevel, currentNestingLevel);
            }
            // Check for loop end
            else if (line.contains("END LOOP") || line.contains("ENDLOOP")) {
                currentNestingLevel = Math.max(0, currentNestingLevel - 1);
            }
        }

        return maxNestingLevel;
    }

    /**
     * Calculate the maximum nesting level of cursors in a stored procedure.
     *
     * @param sourceCode The stored procedure source code
     * @return The maximum nesting level
     */
    private int calculateMaxCursorNestingLevel(String sourceCode) {
        if (sourceCode == null) {
            return 0;
        }

        // 简化实现：计算 DECLARE 块的嵌套级别作为游标嵌套级别的估计
        // 这是一个近似值，因为游标可能在不同的 DECLARE 块中使用

        // 将源代码转换为大写并按行分割
        String[] lines = sourceCode.toUpperCase().split("\r?\n");

        int currentNestingLevel = 0;
        int maxNestingLevel = 0;

        for (String line : lines) {
            line = line.trim();

            // 检查 DECLARE 块开始
            if (line.startsWith("DECLARE") || line.contains(" DECLARE ")) {
                currentNestingLevel++;
                maxNestingLevel = Math.max(maxNestingLevel, currentNestingLevel);
            }
            // 检查 BEGIN 块结束
            else if (line.equals("END;") || line.startsWith("END;")) {
                currentNestingLevel = Math.max(0, currentNestingLevel - 1);
            }
        }

        // 如果没有检测到嵌套，但有游标声明，则至少返回级别 1
        if (maxNestingLevel == 0 && sourceCode.toUpperCase().contains("CURSOR")) {
            return 1;
        }

        return maxNestingLevel;
    }

    /**
     * Calculate the depth of UNION operations in the SQL statement.
     * This method analyzes nested UNION structures to determine complexity.
     *
     * @param sql The SQL statement
     * @return The maximum depth of UNION operations
     */
    private int calculateUnionDepth(String sql) {
        if (sql == null || sql.isEmpty()) {
            return 1;
        }

        // Count parentheses depth around UNION operations
        int maxDepth = 1;
        int currentDepth = 0;
        boolean inUnionContext = false;

        String upperSql = sql.toUpperCase();
        for (int i = 0; i < upperSql.length(); i++) {
            char c = upperSql.charAt(i);

            if (c == '(') {
                currentDepth++;
            } else if (c == ')') {
                currentDepth--;
            } else if (upperSql.substring(i).startsWith("UNION")) {
                inUnionContext = true;
                maxDepth = Math.max(maxDepth, currentDepth + 1);
            }
        }

        return Math.max(maxDepth, 1);
    }

    /**
     * Extract procedure calls and track loop context for each call.
     * This method identifies which procedure calls occur within loops.
     *
     * @param sourceCode The source code to analyze
     * @param customFunctions List of custom functions to exclude
     * @return A map of procedure names to ProcedureCallMetric objects
     */
    private Map<String, ProcedureCallMetric> extractProcedureCallsWithLoopTracking(String sourceCode, List<String> customFunctions) {
        Map<String, ProcedureCallMetric> procedureCalls = new HashMap<>();
        Set<String> customFunctionSet = new HashSet<>(customFunctions.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet()));

        Pattern procedureNamePattern = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$");

        int loopDepth = 0;
        String[] lines = sourceCode.split("\\r?\\n");

        for (String line : lines) {
            String trimmedLine = line.trim().toUpperCase();

            if (trimmedLine.startsWith("FOR ") || trimmedLine.startsWith("WHILE ") || trimmedLine.startsWith("LOOP")) {
                loopDepth++;
            }

            if (trimmedLine.startsWith("END LOOP") || trimmedLine.startsWith("END FOR") || trimmedLine.startsWith("END WHILE")) {
                loopDepth--;
                if (loopDepth < 0) loopDepth = 0;
            }

            Matcher matcher = PROCEDURE_CALL_SIMPLE.matcher(trimmedLine);
            while (matcher.find()) {
                String procedureName = matcher.group(1);

                if (customFunctionSet.contains(procedureName)) {
                    continue;
                }

                if (isBuiltInFunction(procedureName)) {
                    continue;
                }

                if (!procedureNamePattern.matcher(procedureName).matches()) {
                    continue;
                }

                boolean inLoop = loopDepth > 0;
                procedureCalls.merge(procedureName,
                        ProcedureCallMetric.builder()
                                .procedureName(procedureName)
                                .callCount(1)
                                .calledInLoop(inLoop)
                                .build(),
                        (existing, newMetric) -> {
                            int newCount = existing.getCallCount() + 1;
                            boolean newInLoop = existing.isCalledInLoop() || inLoop;
                            return ProcedureCallMetric.builder()
                                    .procedureName(procedureName)
                                    .callCount(newCount)
                                    .calledInLoop(newInLoop)
                                    .build();
                        });
            }
        }

        return procedureCalls;
    }

    /**
     * Check if a function name is a common built-in function or package.
     *
     * @param functionName The function name to check
     * @return True if it's a built-in function or package
     */
    private boolean isBuiltInFunction(String functionName) {
        // Common Hive and SQL built-in functions
        String[] builtIns = {
            "NVL", "COALESCE", "NULLIF", "ISNULL",
            "SUBSTR", "SUBSTRING", "TRIM", "LTRIM", "RTRIM", "LENGTH",
            "UPPER", "LOWER", "REPLACE", "REGEXP_REPLACE",
            "CONCAT", "CONCAT_WS", "SPLIT",
            "TO_DATE", "FROM_UNIXTIME", "UNIX_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP",
            "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND",
            "DATE_ADD", "DATE_SUB", "DATEDIFF",
            "SUM", "AVG", "COUNT", "MIN", "MAX", "STDDEV", "VARIANCE",
            "ROW_NUMBER", "RANK", "DENSE_RANK", "LEAD", "LAG", "FIRST_VALUE", "LAST_VALUE",
            "CAST", "CONVERT",
            "ROUND", "FLOOR", "CEIL", "ABS", "MOD", "POWER", "SQRT",
            "EXP", "LN", "LOG", "SIN", "COS", "TAN",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "IF", "COALESCE",
            "ARRAY", "MAP", "STRUCT", "NAMED_STRUCT",
            "GET_JSON_OBJECT", "JSON_TUPLE",
            "EXPLODE", "POSEXPLODE", "COLLECT_LIST", "COLLECT_SET",
            "INSTR", "LOCATE", "FIND_IN_SET",
            "DECODE",
            "VALUES", "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE",
            "CREATE", "ALTER", "DROP", "TRUNCATE", "GRANT", "REVOKE",
            "WHERE", "GROUP", "ORDER", "HAVING", "LIMIT",
            "JOIN", "LEFT", "RIGHT", "FULL", "INNER", "OUTER", "CROSS",
            "UNION", "INTERSECT", "EXCEPT", "MINUS",
            "WITH", "AS", "ON", "USING"
        };

        for (String builtIn : builtIns) {
            if (functionName.equals(builtIn) || functionName.startsWith(builtIn + ".")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate complexity multiplier based on statement length and line count.
     * Hive queries tend to be very long and this adds appropriate weighting.
     *
     * @param sql The SQL statement
     * @param lineCount The number of lines in the statement
     * @return The complexity multiplier to apply
     */
    private double calculateLengthComplexityMultiplier(String sql, int lineCount) {
        if (sql == null) {
            return 1.0;
        }

        double multiplier = 1.0;
        int sqlLength = sql.length();

        // Apply character length multiplier
        if (sqlLength > VERY_LONG_STATEMENT_THRESHOLD) {
            multiplier *= VERY_LONG_STATEMENT_MULTIPLIER;
        } else if (sqlLength > LONG_STATEMENT_THRESHOLD) {
            multiplier *= LONG_STATEMENT_MULTIPLIER;
        }

        // Apply line count multiplier
        if (lineCount > VERY_LARGE_LINE_COUNT_THRESHOLD) {
            multiplier *= VERY_LARGE_LINE_COUNT_MULTIPLIER;
        } else if (lineCount > LARGE_LINE_COUNT_THRESHOLD) {
            multiplier *= LARGE_LINE_COUNT_MULTIPLIER;
        }

        // Additional scaling for extremely large queries (common in Hive)
        if (sqlLength > 10000) {
            double extraMultiplier = 1.0 + Math.log10(sqlLength / 10000.0) * 0.2;
            multiplier *= extraMultiplier;
        }

        if (lineCount > 500) {
            double extraLineMultiplier = 1.0 + Math.log10(lineCount / 500.0) * 0.15;
            multiplier *= extraLineMultiplier;
        }

        return multiplier;
    }
}
