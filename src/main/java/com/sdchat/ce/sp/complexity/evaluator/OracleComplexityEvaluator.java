package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oracle complexity evaluator implementation.
 */
@Slf4j
@Component
public class OracleComplexityEvaluator implements ComplexityEvaluator {

    private static final String DIALECT = "Oracle";

    // Weights for different complexity factors
    private static final double TABLE_WEIGHT = 1.0;
    private static final double JOIN_WEIGHT = 2.0;
    private static final double WHERE_CONDITION_WEIGHT = 1.5;
    private static final double SUBQUERY_WEIGHT = 3.0;
    private static final double AGGREGATE_FUNCTION_WEIGHT = 1.0;
    private static final double CASE_EXPRESSION_WEIGHT = 1.0;
    private static final double SET_OPERATION_WEIGHT = 2.0;
    private static final double LOOP_WEIGHT = 2.5;
    private static final double NESTED_LOOP_WEIGHT = 1.5; // Multiplier for each nesting level
    private static final double CUSTOM_FUNCTION_WEIGHT = 2.0; // Weight for custom function calls

    // Regex patterns for SQL analysis
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_TABLE_PATTERN = Pattern.compile("\\bJOIN\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_TABLE_PATTERN = Pattern.compile("\\bINSERT\\s+INTO\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("\\bUPDATE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile("\\bDELETE\\s+FROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_CONDITION_PATTERN = Pattern.compile("\\bAND\\b|\\bOR\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("\\(\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGGREGATE_FUNCTION_PATTERN = Pattern.compile("\\b(SUM|AVG|COUNT|MAX|MIN)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASE_EXPRESSION_PATTERN = Pattern.compile("\\bCASE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_OPERATION_PATTERN = Pattern.compile("\\b(UNION|INTERSECT|MINUS)\\b", Pattern.CASE_INSENSITIVE);

    // Regex patterns for PL/SQL loop analysis
    private static final Pattern FOR_LOOP_PATTERN = Pattern.compile("\\bFOR\\b[^;]*\\bLOOP\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHILE_LOOP_PATTERN = Pattern.compile("\\bWHILE\\b[^;]*\\bLOOP\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE_LOOP_PATTERN = Pattern.compile("\\bLOOP\\b(?!\\s*\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_LOOP_PATTERN = Pattern.compile("\\bEND\\s+LOOP\\b", Pattern.CASE_INSENSITIVE);

    // Regex patterns for nested stored procedure calls
    private static final Pattern PROCEDURE_CALL_PATTERN = Pattern.compile("\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*;", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXECUTE_IMMEDIATE_PATTERN = Pattern.compile("\\bEXECUTE\\s+IMMEDIATE\\b", Pattern.CASE_INSENSITIVE);

    // List of custom function names
    private List<String> customFunctions = new ArrayList<>();

    // Pattern for function calls
    private Pattern customFunctionPattern;

    // List of high-weight table names
    private List<String> highWeightTables = new ArrayList<>();

    // Additional weight for high-weight tables
    private static final double HIGH_WEIGHT_TABLE_MULTIPLIER = 2.0;

    // Weight for nested stored procedure calls
    private static final double NESTED_PROCEDURE_WEIGHT = 3.0;

    @Override
    public ComplexityMetrics evaluateSqlStatement(SqlStatement statement) throws Exception {
        if (!"SELECT".equals(statement.getType())) {
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
                log.warn("Failed to evaluate statement in stored procedure: {}", statement.getSql(), e);
            }
        }

        // Calculate aggregate metrics
        int tableCount = 0;
        int joinCount = 0;
        int whereConditionCount = 0;
        int subqueryCount = 0;
        int aggregateFunctionCount = 0;
        int caseExpressionCount = 0;
        int setOperationCount = 0;
        int queryDepth = 0;
        double totalScore = 0;

        // Collect all table names from all statements
        Set<String> allTableNames = new HashSet<>();

        for (ComplexityMetrics metrics : statementMetrics) {
            tableCount += metrics.getTableCount();
            joinCount += metrics.getJoinCount();
            whereConditionCount += metrics.getWhereConditionCount();
            subqueryCount += metrics.getSubqueryCount();
            aggregateFunctionCount += metrics.getAggregateFunctionCount();
            caseExpressionCount += metrics.getCaseExpressionCount();
            setOperationCount += metrics.getSetOperationCount();
            queryDepth = Math.max(queryDepth, metrics.getQueryDepth());
            totalScore += metrics.getOverallScore();

            // Collect table names
            if (metrics.getTableList() != null) {
                allTableNames.addAll(metrics.getTableList());
            }
        }

        // Additional complexity factors for stored procedures
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("statementCount", statements.size());

        // Calculate line count
        int lineCount = 0;
        int loopCount = 0;
        int maxLoopNestingLevel = 0;
        int customFunctionCount = 0;
        List<String> customFunctionList = new ArrayList<>();

        if (procedure.getSourceCode() != null) {
            // 计算换行符的数量
            String sourceCode = procedure.getSourceCode();
            for (int i = 0; i < sourceCode.length(); i++) {
                if (sourceCode.charAt(i) == '\n') {
                    lineCount++;
                }
            }
            // 如果没有换行符或最后一行没有换行符，加1
            if (lineCount == 0 || sourceCode.charAt(sourceCode.length() - 1) != '\n') {
                lineCount++;
            }

            // Count loops and determine nesting level
            loopCount += countMatches(FOR_LOOP_PATTERN, sourceCode);
            loopCount += countMatches(WHILE_LOOP_PATTERN, sourceCode);
            loopCount += countMatches(SIMPLE_LOOP_PATTERN, sourceCode);

            // Calculate maximum loop nesting level
            maxLoopNestingLevel = calculateMaxLoopNestingLevel(sourceCode);

            // Count custom function calls if pattern is available
            if (customFunctionPattern != null) {
                // Map to store function call counts
                Map<String, Integer> functionCallCounts = new HashMap<>();

                Matcher matcher = customFunctionPattern.matcher(sourceCode);
                while (matcher.find()) {
                    customFunctionCount++;
                    String functionName = matcher.group(1);

                    // Add to function list if not already present
                    if (!customFunctionList.contains(functionName)) {
                        customFunctionList.add(functionName);
                    }

                    // Increment call count for this function
                    functionCallCounts.put(functionName,
                        functionCallCounts.getOrDefault(functionName, 0) + 1);
                }

                // Add custom function metrics to additionalMetrics
                additionalMetrics.put("customFunctionCount", customFunctionCount);
                additionalMetrics.put("customFunctionList", customFunctionList);
                additionalMetrics.put("customFunctionCallCounts", functionCallCounts);
            }
        }

        // Calculate overall complexity score
        // Add loop complexity to the overall score
        double loopComplexity = loopCount * LOOP_WEIGHT;
        if (maxLoopNestingLevel > 1) {
            // Apply additional complexity for nested loops
            loopComplexity *= (1 + (maxLoopNestingLevel - 1) * NESTED_LOOP_WEIGHT);
        }

        // Add custom function complexity to the overall score
        double customFunctionComplexity = customFunctionCount * CUSTOM_FUNCTION_WEIGHT;

        double overallScore = totalScore * (1 + 0.1 * statements.size()) + loopComplexity + customFunctionComplexity;

        // Convert table names set to list
        List<String> tableList = new ArrayList<>(allTableNames);
        // Sort table names for consistent output
        Collections.sort(tableList);

        // Process high-weight tables
        List<String> highWeightTableList = new ArrayList<>();
        Map<String, Integer> highWeightTableCounts = new HashMap<>();
        int highWeightTableCount = 0;

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

            // Add high-weight table metrics to additionalMetrics
            additionalMetrics.put("highWeightTableCount", highWeightTableCount);
            additionalMetrics.put("highWeightTableList", highWeightTableList);
            additionalMetrics.put("highWeightTableCounts", highWeightTableCounts);

            // Apply additional weight to the overall score
            if (highWeightTableCount > 0) {
                overallScore *= (1 + (highWeightTableCount * 0.1 * HIGH_WEIGHT_TABLE_MULTIPLIER));
            }
        }

        // Process nested stored procedure calls
        List<String> nestedProcedureList = new ArrayList<>();
        Map<String, Integer> nestedProcedureCounts = new HashMap<>();
        int nestedProcedureCount = 0;

        if (procedure.getSourceCode() != null) {
            // Extract nested procedure calls
            Map<String, Integer> procedureCalls = extractNestedProcedureCalls(
                    procedure.getSourceCode(),
                    customFunctions != null ? customFunctions : new ArrayList<>());

            if (!procedureCalls.isEmpty()) {
                nestedProcedureCount = procedureCalls.values().stream().mapToInt(Integer::intValue).sum();
                nestedProcedureList.addAll(procedureCalls.keySet());
                nestedProcedureCounts.putAll(procedureCalls);

                // Add nested procedure metrics to additionalMetrics
                additionalMetrics.put("nestedProcedureCount", nestedProcedureCount);
                additionalMetrics.put("nestedProcedureList", nestedProcedureList);
                additionalMetrics.put("nestedProcedureCounts", nestedProcedureCounts);

                // Apply additional weight to the overall score
                overallScore += (nestedProcedureCount * NESTED_PROCEDURE_WEIGHT);
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
                .loopCount(loopCount)
                .maxLoopNestingLevel(maxLoopNestingLevel)
                .customFunctionCount(customFunctionCount)
                .customFunctionList(customFunctionList)
                .highWeightTableCount(highWeightTableCount)
                .highWeightTableList(highWeightTableList)
                .nestedProcedureCount(nestedProcedureCount)
                .nestedProcedureList(nestedProcedureList)
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
     * Set the list of custom function names.
     * This will also update the pattern used to match custom function calls.
     *
     * @param customFunctions The list of custom function names
     */
    public void setCustomFunctions(List<String> customFunctions) {
        if (customFunctions == null || customFunctions.isEmpty()) {
            this.customFunctions = new ArrayList<>();
            this.customFunctionPattern = null;
            return;
        }

        this.customFunctions = new ArrayList<>(customFunctions);

        // Create a pattern to match any of the custom functions
        // The pattern will match function calls like: FUNCTION_NAME(...)
        StringBuilder patternBuilder = new StringBuilder("\\b(");
        for (int i = 0; i < customFunctions.size(); i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(customFunctions.get(i).toUpperCase()));
        }
        patternBuilder.append(")\\s*\\(");

        this.customFunctionPattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Get the list of custom function names.
     *
     * @return The list of custom function names
     */
    public List<String> getCustomFunctions() {
        return new ArrayList<>(customFunctions);
    }

    /**
     * Set the list of high-weight table names.
     *
     * @param highWeightTables The list of high-weight table names
     */
    public void setHighWeightTables(List<String> highWeightTables) {
        if (highWeightTables == null || highWeightTables.isEmpty()) {
            this.highWeightTables = new ArrayList<>();
            return;
        }

        // Convert all table names to uppercase for case-insensitive comparison
        this.highWeightTables = highWeightTables.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /**
     * Get the list of high-weight table names.
     *
     * @return The list of high-weight table names
     */
    public List<String> getHighWeightTables() {
        return new ArrayList<>(highWeightTables);
    }

    /**
     * Evaluate the complexity of a SELECT statement.
     *
     * @param sql The SQL statement text
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateSelectStatement(String sql) {
        // Extract table names
        List<String> tableList = getAllTableNames(sql);
        int tableCount = tableList.size();
        if (tableCount == 0) {
            tableCount = 1; // At least one table
            tableList.add("UNKNOWN"); // Add a placeholder for unknown tables
        }

        // Count joins
        int joinCount = countMatches(JOIN_PATTERN, sql);

        // Count where conditions
        int whereConditionCount = countMatches(WHERE_CONDITION_PATTERN, sql) + 1;
        if (!sql.toUpperCase().contains("WHERE")) whereConditionCount = 0;

        // Count subqueries
        int subqueryCount = countMatches(SUBQUERY_PATTERN, sql);

        // Count aggregate functions
        int aggregateFunctionCount = countMatches(AGGREGATE_FUNCTION_PATTERN, sql);

        // Count case expressions
        int caseExpressionCount = countMatches(CASE_EXPRESSION_PATTERN, sql);

        // Count set operations
        int setOperationCount = countMatches(SET_OPERATION_PATTERN, sql);

        // Estimate query depth based on subquery count
        int queryDepth = subqueryCount > 0 ? 2 : 1;

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
            // 计算换行符的数量
            for (int i = 0; i < sql.length(); i++) {
                if (sql.charAt(i) == '\n') {
                    lineCount++;
                }
            }
            // 如果没有换行符或最后一行没有换行符，加1
            if (lineCount == 0 || sql.charAt(sql.length() - 1) != '\n') {
                lineCount++;
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
     * Evaluate the complexity of a non-SELECT statement.
     *
     * @param statement The non-SELECT statement to evaluate
     * @return The complexity metrics
     */
    private ComplexityMetrics evaluateNonSelectStatement(SqlStatement statement) {
        // For non-SELECT statements, use a simplified evaluation
        // based on the statement length and type

        String sql = statement.getSql();
        int length = sql.length();

        // Estimate complexity based on statement length
        double baseScore = Math.log10(length) * 5;

        // Adjust based on statement type
        double typeMultiplier = 1.0;
        switch (statement.getType()) {
            case "INSERT":
                typeMultiplier = 1.0;
                break;
            case "UPDATE":
                typeMultiplier = 1.2;
                break;
            case "DELETE":
                typeMultiplier = 1.1;
                break;
            case "MERGE":
                typeMultiplier = 1.5;
                break;
            default:
                typeMultiplier = 1.0;
        }

        double overallScore = baseScore * typeMultiplier;

        // Extract table names
        List<String> tableList = getAllTableNames(sql);
        int tableCount = tableList.size();
        if (tableCount == 0) {
            tableCount = 1; // At least one table
            tableList.add("UNKNOWN"); // Add a placeholder for unknown tables
        }

        int whereConditionCount = sql.toUpperCase().contains("WHERE") ? 1 : 0;

        // Calculate line count
        int lineCount = 0;
        if (sql != null) {
            // 计算换行符的数量
            for (int i = 0; i < sql.length(); i++) {
                if (sql.charAt(i) == '\n') {
                    lineCount++;
                }
            }
            // 如果没有换行符或最后一行没有换行符，加1
            if (lineCount == 0 || sql.charAt(sql.length() - 1) != '\n') {
                lineCount++;
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
     * Count the number of matches for a pattern in a string.
     *
     * @param pattern The pattern to match
     * @param text The text to search in
     * @return The number of matches
     */
    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Extract table names from SQL text using a pattern.
     *
     * @param pattern The pattern with a capturing group for the table name
     * @param text The SQL text to search in
     * @return A list of table names
     */
    private List<String> extractTableNames(Pattern pattern, String text) {
        List<String> tableNames = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                String tableName = matcher.group(1).trim();
                tableNames.add(tableName);
            }
        }
        return tableNames;
    }

    /**
     * Get all table names from a SQL statement.
     *
     * @param sql The SQL statement text
     * @return A list of unique table names
     */
    private List<String> getAllTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();

        // Extract tables from FROM clauses
        tableNames.addAll(extractTableNames(TABLE_PATTERN, sql));

        // Extract tables from JOIN clauses
        tableNames.addAll(extractTableNames(JOIN_TABLE_PATTERN, sql));

        // Extract tables from INSERT statements
        tableNames.addAll(extractTableNames(INSERT_TABLE_PATTERN, sql));

        // Extract tables from UPDATE statements
        tableNames.addAll(extractTableNames(UPDATE_TABLE_PATTERN, sql));

        // Extract tables from DELETE statements
        tableNames.addAll(extractTableNames(DELETE_TABLE_PATTERN, sql));

        return new ArrayList<>(tableNames);
    }

    /**
     * Calculate the maximum nesting level of loops in the source code.
     * This is a simplified approach that counts the maximum number of unclosed loops at any point.
     *
     * @param sourceCode The source code to analyze
     * @return The maximum nesting level of loops
     */
    private int calculateMaxLoopNestingLevel(String sourceCode) {
        // Convert to uppercase for case-insensitive matching
        String upperSource = sourceCode.toUpperCase();

        int currentNestingLevel = 0;
        int maxNestingLevel = 0;

        // Split the source code into tokens
        String[] tokens = upperSource.split("[\\s\\n\\r\\t\\(\\)\\{\\}\\[\\]\\;\\,]+");

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            // Check for loop start
            if (i < tokens.length - 1 &&
                (token.equals("FOR") || token.equals("WHILE")) &&
                containsWithinNext(tokens, i, "LOOP", 10)) {
                currentNestingLevel++;
                maxNestingLevel = Math.max(maxNestingLevel, currentNestingLevel);
            }
            else if (token.equals("LOOP") &&
                    (i == 0 || !(tokens[i-1].equals("END")))) {
                currentNestingLevel++;
                maxNestingLevel = Math.max(maxNestingLevel, currentNestingLevel);
            }
            // Check for loop end
            else if (token.equals("END") &&
                    i < tokens.length - 1 &&
                    tokens[i+1].equals("LOOP")) {
                currentNestingLevel--;
            }
        }

        return maxNestingLevel;
    }

    /**
     * Check if a target token appears within the next n tokens.
     *
     * @param tokens The array of tokens
     * @param startIndex The starting index
     * @param target The target token to find
     * @param maxDistance The maximum number of tokens to check
     * @return True if the target is found within the specified distance
     */
    private boolean containsWithinNext(String[] tokens, int startIndex, String target, int maxDistance) {
        int endIndex = Math.min(startIndex + maxDistance, tokens.length);
        for (int i = startIndex + 1; i < endIndex; i++) {
            if (tokens[i].equals(target)) {
                return true;
            }
        }
        return false;
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
            "UPDATE\\s+\\w+\\." + tableName + "\\b"         // UPDATE schema.table
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
     * Extract nested stored procedure calls from the source code.
     * This method identifies direct procedure calls and excludes custom functions.
     *
     * @param sourceCode The source code to analyze
     * @param customFunctions List of custom functions to exclude
     * @return A map of procedure names to call counts
     */
    private Map<String, Integer> extractNestedProcedureCalls(String sourceCode, List<String> customFunctions) {
        Map<String, Integer> procedureCalls = new HashMap<>();
        Set<String> customFunctionSet = new HashSet<>(customFunctions.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet()));

        // Find all procedure calls
        Matcher matcher = PROCEDURE_CALL_PATTERN.matcher(sourceCode.toUpperCase());
        while (matcher.find()) {
            String procedureName = matcher.group(1);

            // Skip if this is a custom function
            if (customFunctionSet.contains(procedureName)) {
                continue;
            }

            // Skip common built-in functions and packages
            if (isBuiltInFunction(procedureName)) {
                continue;
            }

            // Increment call count for this procedure
            procedureCalls.put(procedureName,
                procedureCalls.getOrDefault(procedureName, 0) + 1);
        }

        // Also check for EXECUTE IMMEDIATE statements which might contain dynamic SQL
        int executeImmediateCount = 0;
        matcher = EXECUTE_IMMEDIATE_PATTERN.matcher(sourceCode.toUpperCase());
        while (matcher.find()) {
            executeImmediateCount++;
        }

        if (executeImmediateCount > 0) {
            procedureCalls.put("EXECUTE_IMMEDIATE", executeImmediateCount);
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
        // Common Oracle built-in functions and packages
        String[] builtIns = {
            "NVL", "SUBSTR", "TO_CHAR", "TO_DATE", "TO_NUMBER", "SYSDATE",
            "DBMS_OUTPUT.PUT_LINE", "DBMS_OUTPUT", "DBMS_SQL", "UTL_FILE",
            "TRUNC", "ROUND", "LENGTH", "INSTR", "UPPER", "LOWER", "TRIM",
            "ADD_MONTHS", "MONTHS_BETWEEN", "NEXT_DAY", "LAST_DAY",
            "DECODE", "REPLACE", "LPAD", "RPAD", "LTRIM", "RTRIM",
            "GREATEST", "LEAST", "ABS", "SIGN", "MOD", "FLOOR", "CEIL",
            "POWER", "SQRT", "EXP", "LN", "LOG", "SIN", "COS", "TAN",
            "ASIN", "ACOS", "ATAN", "ATAN2", "SINH", "COSH", "TANH",
            // SQL keywords that might be mistaken for procedures
            "VALUES", "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE",
            "CREATE", "ALTER", "DROP", "TRUNCATE", "GRANT", "REVOKE",
            // PL/SQL keywords
            "DECLARE", "BEGIN", "EXCEPTION", "END", "IF", "THEN", "ELSE", "ELSIF",
            "LOOP", "WHILE", "FOR", "EXIT", "CONTINUE", "RETURN", "GOTO",
            // Data types
            "VARCHAR", "VARCHAR2", "CHAR", "NUMBER", "DATE", "TIMESTAMP", "BOOLEAN",
            "INTEGER", "FLOAT", "DOUBLE", "DECIMAL", "BINARY", "BLOB", "CLOB", "NCLOB",
            "RAW", "LONG", "LONG RAW", "ROWID", "UROWID", "REF", "CURSOR"
        };

        for (String builtIn : builtIns) {
            if (functionName.equals(builtIn) || functionName.startsWith(builtIn + ".")) {
                return true;
            }
        }

        return false;
    }
}
