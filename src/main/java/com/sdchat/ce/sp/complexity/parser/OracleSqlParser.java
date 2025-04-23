package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oracle SQL parser implementation using regex patterns.
 */
@Slf4j
@Component
public class OracleSqlParser implements SqlParser {

    private static final String DIALECT = "Oracle";

    // Pattern to split multiple SQL statements (simplified)
    private static final Pattern SQL_DELIMITER_PATTERN = Pattern.compile(";\\s*$", Pattern.MULTILINE);

    // Patterns to identify SQL statement types
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERGE_PATTERN = Pattern.compile("^\\s*MERGE\\s", Pattern.CASE_INSENSITIVE);

    @Override
    public SqlStatement parse(String sql) throws Exception {
        try {
            String type = determineStatementType(sql);

            return SqlStatement.builder()
                    .sql(sql)
                    .type(type)
                    .dialect(DIALECT)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse SQL statement: {}", sql, e);
            throw new Exception("Failed to parse SQL statement: " + e.getMessage(), e);
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
                    log.warn("Skipping unparseable SQL statement: {}", trimmedSql, e);
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
        } else {
            return "UNKNOWN";
        }
    }
}
