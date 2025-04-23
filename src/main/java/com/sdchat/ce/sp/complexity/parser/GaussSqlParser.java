package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gauss SQL parser implementation using regex patterns.
 */
@Slf4j
@Component
public class GaussSqlParser implements SqlParser {

    private static final String DIALECT = "Gauss";

    // Pattern to split multiple SQL statements (simplified)
    private static final Pattern SQL_DELIMITER_PATTERN = Pattern.compile(";\\s*$", Pattern.MULTILINE);

    // Patterns to identify statement types
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERGE_PATTERN = Pattern.compile("^\\s*MERGE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMIT_PATTERN = Pattern.compile("^\\s*COMMIT\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLLBACK_PATTERN = Pattern.compile("^\\s*ROLLBACK\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_PATTERN = Pattern.compile("^\\s*CREATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_PATTERN = Pattern.compile("^\\s*ALTER\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_PATTERN = Pattern.compile("^\\s*DROP\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile("^\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRANT_PATTERN = Pattern.compile("^\\s*GRANT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REVOKE_PATTERN = Pattern.compile("^\\s*REVOKE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL_PATTERN = Pattern.compile("^\\s*([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE);

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
            if (!"CALL".equals(type) && !"COMMIT".equals(type) && !"ROLLBACK".equals(type) && !isDynamicSql) {
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
                Pattern insertTablePattern = Pattern.compile("\\bINTO\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = insertTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("UPDATE".equals(type)) {
                Pattern updateTablePattern = Pattern.compile("\\bUPDATE\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = updateTablePattern.matcher(sql);
                if (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }
            } else if ("SELECT".equals(type)) {
                Pattern fromTablePattern = Pattern.compile("\\bFROM\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = fromTablePattern.matcher(sql);
                while (tableMatcher.find()) {
                    tableList.add(tableMatcher.group(1));
                }

                Pattern joinTablePattern = Pattern.compile("\\bJOIN\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
                Matcher joinMatcher = joinTablePattern.matcher(sql);
                while (joinMatcher.find()) {
                    tableList.add(joinMatcher.group(1));
                }
            }

            return SqlStatement.builder()
                    .sql(sql)
                    .type(type)
                    .dialect(DIALECT)
                    .tableList(tableList)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gauss SQL statement: {}", sql, e);
            throw new Exception("Failed to parse Gauss SQL statement: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SqlStatement> parseMultiple(String sqlText) throws Exception {
        List<SqlStatement> statements = new ArrayList<>();

        // Split the SQL text into individual statements
        String[] sqlParts = SQL_DELIMITER_PATTERN.split(sqlText);

        for (String sqlPart : sqlParts) {
            String trimmedSql = sqlPart.trim();
            if (!trimmedSql.isEmpty()) {
                try {
                    statements.add(parse(trimmedSql));
                } catch (Exception e) {
                    log.warn("Skipping unparseable Gauss SQL statement: {}", trimmedSql, e);
                }
            }
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

        // For SELECT statements, check if they have a FROM clause
        if ("SELECT".equals(type) && !sql.toUpperCase().contains(" FROM ")) {
            throw new Exception("Invalid SELECT statement: missing FROM clause");
        }

        // For INSERT statements, check if they have an INTO clause
        if ("INSERT".equals(type) && !sql.toUpperCase().contains(" INTO ")) {
            throw new Exception("Invalid INSERT statement: missing INTO clause");
        }

        // For UPDATE statements, check if they have a SET clause
        if ("UPDATE".equals(type) && !sql.toUpperCase().contains(" SET ")) {
            throw new Exception("Invalid UPDATE statement: missing SET clause");
        }

        // Note: We no longer validate that DELETE statements must have a FROM clause
        // as it's valid in Gauss SQL to have DELETE statements without a FROM clause
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
