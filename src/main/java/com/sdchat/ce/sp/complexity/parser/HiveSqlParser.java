package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hive SQL parser implementation using regex patterns.
 */
@Slf4j
@Component
public class HiveSqlParser implements SqlParser {

    private static final String DIALECT = "Hive";

    // Pattern to split multiple SQL statements (simplified)
    private static final Pattern SQL_DELIMITER_PATTERN = Pattern.compile(";\\s*$", Pattern.MULTILINE);

    // Patterns to identify statement types (allow comments and multiline)
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?:^|\\n)\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("(?:^|\\n)\\s*INSERT\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("(?:^|\\n)\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("(?:^|\\n)\\s*DELETE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern MERGE_PATTERN = Pattern.compile("(?:^|\\n)\\s*MERGE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern COMMIT_PATTERN = Pattern.compile("(?:^|\\n)\\s*COMMIT\\s*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ROLLBACK_PATTERN = Pattern.compile("(?:^|\\n)\\s*ROLLBACK\\s*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern CREATE_PATTERN = Pattern.compile("(?:^|\\n)\\s*CREATE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ALTER_PATTERN = Pattern.compile("(?:^|\\n)\\s*ALTER\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern DROP_PATTERN = Pattern.compile("(?:^|\\n)\\s*DROP\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile("(?:^|\\n)\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern GRANT_PATTERN = Pattern.compile("(?:^|\\n)\\s*GRANT\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern REVOKE_PATTERN = Pattern.compile("(?:^|\\n)\\s*REVOKE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern CALL_PATTERN = Pattern.compile("(?:^|\\n)\\s*([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Hive-specific patterns (allow comments and multiline)
    private static final Pattern LOAD_PATTERN = Pattern.compile("(?:^|\\n)\\s*LOAD\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern EXPORT_PATTERN = Pattern.compile("(?:^|\\n)\\s*EXPORT\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?:^|\\n)\\s*IMPORT\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern MSCK_PATTERN = Pattern.compile("(?:^|\\n)\\s*MSCK\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ANALYZE_PATTERN = Pattern.compile("(?:^|\\n)\\s*ANALYZE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern EXPLAIN_PATTERN = Pattern.compile("(?:^|\\n)\\s*EXPLAIN\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern SHOW_PATTERN = Pattern.compile("(?:^|\\n)\\s*SHOW\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern DESCRIBE_PATTERN = Pattern.compile("(?:^|\\n)\\s*DESCRIBE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern DESC_PATTERN = Pattern.compile("(?:^|\\n)\\s*DESC\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern USE_PATTERN = Pattern.compile("(?:^|\\n)\\s*USE\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern SET_PATTERN = Pattern.compile("(?:^|\\n)\\s*SET\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern RESET_PATTERN = Pattern.compile("(?:^|\\n)\\s*RESET\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ADD_PATTERN = Pattern.compile("(?:^|\\n)\\s*ADD\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Pattern to detect dynamic SQL string concatenation
    private static final Pattern DYNAMIC_SQL_PATTERN = Pattern.compile("'\\s*\\|\\|\\s*'", Pattern.CASE_INSENSITIVE);

    @Override
    public SqlStatement parse(String sql) throws Exception {
        try {
            // Determine statement type first
            String type = determineStatementType(sql);

            // Check if this is a dynamic SQL string concatenation
            boolean isDynamicSql = isDynamicSqlConcatenation(sql);

            // Skip validation for certain statement types or dynamic SQL
            if (!"CALL".equals(type) && !"COMMIT".equals(type) && !"ROLLBACK".equals(type) &&
                !"SHOW".equals(type) && !"DESCRIBE".equals(type) && !"DESC".equals(type) &&
                !"USE".equals(type) && !"SET".equals(type) && !"RESET".equals(type) &&
                !"EXPLAIN".equals(type) && !isDynamicSql) {
                validateSqlStatement(sql, type);
            }

            // Extract table names based on statement type
            List<String> tableList = new ArrayList<>();
            if (isDynamicSql) {
                type = "DYNAMIC_SQL";
                tableList = extractTablesFromDynamicSql(sql);
            } else if ("DELETE".equals(type)) {
                log.info("Processing DELETE statement: {}", sql);
                // First try to find table with FROM clause
                Pattern deleteTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = deleteTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    String tableName = tableMatcher.group(1);
                    log.info("Found table with FROM clause: {}", tableName);
                    tableList.add(tableName);
                } else {
                    log.info("No FROM clause found, trying to find table directly after DELETE");
                    // Try to find table without FROM clause (directly after DELETE)
                    Pattern deleteNoFromPattern = Pattern.compile("\\bDELETE\\s+([\\w\\.]+)\\s+WHERE\\b|\\bDELETE\\s+([\\w\\.]+)\\s*;", Pattern.CASE_INSENSITIVE);
                    log.info("DELETE pattern: {}", deleteNoFromPattern.pattern());
                    Matcher noFromMatcher = deleteNoFromPattern.matcher(sql);
                    if (noFromMatcher.find()) {
                        // Group 1 is for the pattern with WHERE, Group 2 is for the pattern with semicolon
                        String tableName = noFromMatcher.group(1) != null ? noFromMatcher.group(1) : noFromMatcher.group(2);
                        log.info("Found table directly after DELETE: {}", tableName);
                        if (tableName != null) {
                            tableList.add(tableName.trim());
                        }
                    } else {
                        log.info("No table found directly after DELETE");
                        log.info("Trying simpler pattern");
                        // Try an even simpler pattern
                        Pattern simpleDeletePattern = Pattern.compile("\\bDELETE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                        Matcher simpleMatcher = simpleDeletePattern.matcher(sql);
                        if (simpleMatcher.find()) {
                            String tableName = simpleMatcher.group(1);
                            log.info("Found table with simple pattern: {}", tableName);
                            tableList.add(tableName.trim());
                        } else {
                            log.info("Still no table found. SQL: {}", sql);
                        }
                    }
                }
            } else if ("INSERT".equals(type)) {
                // Handle both INSERT INTO and INSERT OVERWRITE
                Pattern insertTablePattern = Pattern.compile("\\b(?:INTO|OVERWRITE\\s+TABLE)\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = insertTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }

                // Also extract tables from the SELECT part of INSERT
                Pattern fromTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher fromMatcher = fromTablePattern.matcher(sql);
                while (fromMatcher.find()) {
                    tableList.add(fromMatcher.group(1));
                }
            } else if ("UPDATE".equals(type)) {
                Pattern updateTablePattern = Pattern.compile("\\bUPDATE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = updateTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("SELECT".equals(type)) {
                // Extract tables from FROM clauses (including WITH clauses)
                Pattern withTablePattern = Pattern.compile("\\bWITH\\s+([\\w\\.]+)\\s+AS\\s*\\(", Pattern.CASE_INSENSITIVE);
                Matcher withMatcher = withTablePattern.matcher(sql);
                while (withMatcher.find()) {
                    tableList.add(withMatcher.group(1));
                }

                Pattern fromTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = fromTablePattern.matcher(sql);
                while (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }

                // Extract tables from various JOIN types
                Pattern joinTablePattern = Pattern.compile("\\b(?:INNER\\s+|LEFT\\s+|RIGHT\\s+|FULL\\s+|CROSS\\s+|LEFT\\s+OUTER\\s+|RIGHT\\s+OUTER\\s+|FULL\\s+OUTER\\s+)?JOIN\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher joinMatcher = joinTablePattern.matcher(sql);
                while (joinMatcher.find()) {
                    tableList.add(joinMatcher.group(1));
                }

                // Handle Hive-specific LATERAL VIEW
                Pattern lateralViewPattern = Pattern.compile("\\bLATERAL\\s+VIEW\\s+\\w+\\s*\\([^)]*\\)\\s+\\w+\\s+AS\\s+\\w+", Pattern.CASE_INSENSITIVE);
                Matcher lateralViewMatcher = lateralViewPattern.matcher(sql);
                if (lateralViewMatcher.find()) {
                    // This is a complexity factor but doesn't add a table
                    log.debug("Found LATERAL VIEW in Hive SQL");
                }
            } else if ("LOAD".equals(type)) {
                Pattern loadTablePattern = Pattern.compile("\\bINTO\\s+TABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = loadTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("EXPORT".equals(type)) {
                Pattern exportTablePattern = Pattern.compile("\\bTABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = exportTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("IMPORT".equals(type)) {
                Pattern importTablePattern = Pattern.compile("\\bTABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = importTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("MSCK".equals(type)) {
                Pattern msckTablePattern = Pattern.compile("\\bREPAIR\\s+TABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = msckTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("ANALYZE".equals(type)) {
                Pattern analyzeTablePattern = Pattern.compile("\\bTABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = analyzeTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("CREATE".equals(type)) {
                // Handle CREATE TABLE
                Pattern createTablePattern = Pattern.compile("\\bCREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = createTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }

                // Handle CREATE TABLE AS SELECT
                if (sql.toUpperCase().contains(" AS ")) {
                    Pattern fromTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                    Matcher fromMatcher = fromTablePattern.matcher(sql);
                    while (fromMatcher.find()) {
                        tableList.add(fromMatcher.group(1));
                    }
                }
            } else if ("ALTER".equals(type)) {
                Pattern alterTablePattern = Pattern.compile("\\bALTER\\s+TABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = alterTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("DROP".equals(type)) {
                Pattern dropTablePattern = Pattern.compile("\\bDROP\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = dropTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("TRUNCATE".equals(type)) {
                Pattern truncateTablePattern = Pattern.compile("\\bTRUNCATE\\s+TABLE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = truncateTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("DESCRIBE".equals(type) || "DESC".equals(type)) {
                Pattern describeTablePattern = Pattern.compile("\\b(?:DESCRIBE|DESC)\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = describeTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            }

            return SqlStatement.builder()
                    .sql(sql)
                    .type(type)
                    .dialect(DIALECT)
                    .tableList(tableList)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Hive SQL statement: {}", sql, e);
            throw new Exception("Failed to parse Hive SQL statement: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SqlStatement> parseMultiple(String sqlText) throws Exception {
        List<SqlStatement> statements = new ArrayList<>();
        List<String> failedStatements = new ArrayList<>();

        // Split the SQL text into individual statements
        String[] sqlParts = SQL_DELIMITER_PATTERN.split(sqlText);

        for (String sqlPart : sqlParts) {
            String trimmedSql = sqlPart.trim();
            if (!trimmedSql.isEmpty()) {
                try {
                    statements.add(parse(trimmedSql));
                } catch (Exception e) {
                    log.warn("Found unparseable Hive SQL statement: {}", trimmedSql, e);
                    // Create a simple statement for unparseable SQL
                    SqlStatement statement = SqlStatement.builder()
                            .sql(trimmedSql)
                            .type("UNKNOWN")
                            .dialect(DIALECT)
                            .build();
                    statements.add(statement);
                    failedStatements.add(trimmedSql);
                }
            }
        }

        // Store the failed statements in the thread local for later retrieval
        if (!failedStatements.isEmpty()) {
            SqlParserExceptionCollector.addFailedStatements(failedStatements);
        }

        return statements;
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Determine the type of SQL statement.
     *
     * @param sql The SQL statement text
     * @return The statement type as a string
     */
    private String determineStatementType(String sql) {
        if (SELECT_PATTERN.matcher(sql).find()) {
            return "SELECT";
        } else if (INSERT_PATTERN.matcher(sql).find()) {
            return "INSERT";
        } else if (UPDATE_PATTERN.matcher(sql).find()) {
            return "UPDATE";
        } else if (DELETE_PATTERN.matcher(sql).find()) {
            return "DELETE";
        } else if (MERGE_PATTERN.matcher(sql).find()) {
            return "MERGE";
        } else if (COMMIT_PATTERN.matcher(sql).find()) {
            return "COMMIT";
        } else if (ROLLBACK_PATTERN.matcher(sql).find()) {
            return "ROLLBACK";
        } else if (CREATE_PATTERN.matcher(sql).find()) {
            return "CREATE";
        } else if (ALTER_PATTERN.matcher(sql).find()) {
            return "ALTER";
        } else if (DROP_PATTERN.matcher(sql).find()) {
            return "DROP";
        } else if (TRUNCATE_PATTERN.matcher(sql).find()) {
            return "TRUNCATE";
        } else if (GRANT_PATTERN.matcher(sql).find()) {
            return "GRANT";
        } else if (REVOKE_PATTERN.matcher(sql).find()) {
            return "REVOKE";
        } else if (CALL_PATTERN.matcher(sql).find()) {
            return "CALL";
        } else if (LOAD_PATTERN.matcher(sql).find()) {
            return "LOAD";
        } else if (EXPORT_PATTERN.matcher(sql).find()) {
            return "EXPORT";
        } else if (IMPORT_PATTERN.matcher(sql).find()) {
            return "IMPORT";
        } else if (MSCK_PATTERN.matcher(sql).find()) {
            return "MSCK";
        } else if (ANALYZE_PATTERN.matcher(sql).find()) {
            return "ANALYZE";
        } else if (EXPLAIN_PATTERN.matcher(sql).find()) {
            return "EXPLAIN";
        } else if (SHOW_PATTERN.matcher(sql).find()) {
            return "SHOW";
        } else if (DESCRIBE_PATTERN.matcher(sql).find()) {
            return "DESCRIBE";
        } else if (DESC_PATTERN.matcher(sql).find()) {
            return "DESC";
        } else if (USE_PATTERN.matcher(sql).find()) {
            return "USE";
        } else if (SET_PATTERN.matcher(sql).find()) {
            return "SET";
        } else if (RESET_PATTERN.matcher(sql).find()) {
            return "RESET";
        } else if (ADD_PATTERN.matcher(sql).find()) {
            return "ADD";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Validate that the SQL statement is well-formed.
     *
     * @param sql The SQL statement text
     * @param type The determined statement type
     * @throws Exception If the SQL statement is invalid
     */
    private void validateSqlStatement(String sql, String type) throws Exception {
        // Check if SQL is empty
        if (sql == null || sql.trim().isEmpty()) {
            throw new Exception("SQL statement is empty");
        }

        // For very short statements, only validate if they are core DML statements
        if (sql.trim().length() < 10) {
            if ("SELECT".equals(type) || "INSERT".equals(type) || "UPDATE".equals(type) || "DELETE".equals(type)) {
                throw new Exception("SQL statement is too short");
            }
            return;
        }

        // For SELECT statements, check if they have a FROM clause (but allow LATERAL VIEW)
        if ("SELECT".equals(type)) {
            String upperSql = sql.toUpperCase().replaceAll("\\s+", " ");
            if (!upperSql.contains(" FROM ") && !upperSql.contains("LATERAL VIEW")) {
                throw new Exception("Invalid SELECT statement: missing FROM clause");
            }
        }

        // For INSERT statements, check if they have an INTO or OVERWRITE clause
        if ("INSERT".equals(type) && !sql.toUpperCase().contains(" INTO ") && !sql.toUpperCase().contains(" OVERWRITE ")) {
            throw new Exception("Invalid INSERT statement: missing INTO or OVERWRITE clause");
        }

        // For UPDATE statements, check if they have a SET clause
        if ("UPDATE".equals(type) && !sql.toUpperCase().contains(" SET ")) {
            throw new Exception("Invalid UPDATE statement: missing SET clause");
        }

        // For LOAD statements, check if they have an INTO TABLE clause
        if ("LOAD".equals(type) && !sql.toUpperCase().contains(" INTO TABLE ")) {
            throw new Exception("Invalid LOAD statement: missing INTO TABLE clause");
        }
    }

    /**
     * Check if the SQL statement is a dynamic SQL string concatenation.
     * This method looks for patterns like "'||'" which indicate string concatenation.
     *
     * @param sql The SQL statement text
     * @return True if the SQL statement is a dynamic SQL string concatenation
     */
    private boolean isDynamicSqlConcatenation(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }

        // Check for string concatenation pattern
        return DYNAMIC_SQL_PATTERN.matcher(sql).find() || sql.contains("||'") || sql.contains("'||");
    }

    /**
     * Extract table names from a dynamic SQL string.
     * This method attempts to identify table names in a dynamic SQL string by looking for
     * common SQL patterns like FROM, JOIN, etc.
     *
     * @param sql The dynamic SQL string
     * @return A list of table names found in the dynamic SQL string
     */
    private List<String> extractTablesFromDynamicSql(String sql) {
        List<String> tableNames = new ArrayList<>();

        // Split the SQL string by the concatenation operator
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

            if (part.toUpperCase().contains(" JOIN ")) {
                // Extract table name after JOIN
                int joinIndex = part.toUpperCase().indexOf(" JOIN ");
                String afterJoin = part.substring(joinIndex + 6).trim();

                // Extract the table name (until the next space or end of string)
                int spaceIndex = afterJoin.indexOf(" ");
                String tableName = spaceIndex > 0 ? afterJoin.substring(0, spaceIndex) : afterJoin;

                if (!tableName.isEmpty()) {
                    tableNames.add(tableName);
                }
            }

            // Add more patterns as needed (INTO, UPDATE, etc.)
        }

        return tableNames;
    }
}
