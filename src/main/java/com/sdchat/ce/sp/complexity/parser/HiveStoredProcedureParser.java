package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.util.SqlCommentRemover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hive stored procedure parser implementation.
 * Note: Hive doesn't have traditional stored procedures like Oracle or Gauss,
 * but it supports UDFs (User-Defined Functions) and scripts.
 * This parser treats Hive scripts as stored procedures for complexity evaluation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HiveStoredProcedureParser implements StoredProcedureParser {

    private static final String DIALECT = "Hive";

    // Pattern to extract SQL statements from Hive script
    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE|LOAD|EXPORT|IMPORT|MSCK|ANALYZE|EXPLAIN|SHOW|DESCRIBE|DESC|USE|SET|RESET|ADD)\\b[\\s\\S]*?\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract function calls
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
            "\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect package body (not applicable for Hive, but kept for interface compatibility)
    private static final Pattern PACKAGE_BODY_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract package name from package body declaration (not applicable for Hive)
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\s+([\\w\\.]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract function definitions
    private static final Pattern FUNCTION_DEFINITION_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:TEMPORARY\\s+)?\\bFUNCTION\\s+([\\w\\.]+)\\s+AS\\s+[\\s\\S]*?;",
            Pattern.CASE_INSENSITIVE
    );

    private final HiveSqlParser sqlParser;

    @Override
    public StoredProcedure parse(String sourceCode, String name, String schema) throws Exception {
        // Remove comments from source code before extracting SQL statements
        String cleanSourceCode = SqlCommentRemover.removeComments(sourceCode);
        List<SqlStatement> sqlStatements = extractSqlStatements(cleanSourceCode);

        return StoredProcedure.builder()
                .name(name)
                .schema(schema)
                .sourceCode(sourceCode) // Keep the original source code with comments
                .sqlStatements(sqlStatements)
                .dialect(DIALECT)
                .build();
    }

    @Override
    public boolean isPackageBody(String sourceCode) {
        // Hive doesn't have package bodies, but we need to implement this method
        // for interface compatibility. Always return false.
        return false;
    }

    @Override
    public List<StoredProcedure> parsePackageBody(String sourceCode, String packageName, String schema) throws Exception {
        // Hive doesn't have package bodies, but we can extract UDFs as separate procedures
        List<StoredProcedure> procedures = new ArrayList<>();
        String cleanSourceCode = SqlCommentRemover.removeComments(sourceCode);

        // Extract function definitions
        Matcher matcher = FUNCTION_DEFINITION_PATTERN.matcher(cleanSourceCode);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            String functionCode = matcher.group(0); // The entire function definition

            // Parse the function
            List<SqlStatement> sqlStatements = extractSqlStatements(functionCode);

            // Create a StoredProcedure object
            StoredProcedure procedure = StoredProcedure.builder()
                    .name(functionName)
                    .schema(schema)
                    .sourceCode(functionCode)
                    .sqlStatements(sqlStatements)
                    .dialect(DIALECT)
                    .build();

            procedures.add(procedure);
        }

        // If no functions were found, treat the entire script as a single procedure
        if (procedures.isEmpty()) {
            StoredProcedure procedure = StoredProcedure.builder()
                    .name(packageName)
                    .schema(schema)
                    .sourceCode(sourceCode)
                    .sqlStatements(extractSqlStatements(cleanSourceCode))
                    .dialect(DIALECT)
                    .build();

            procedures.add(procedure);
        }

        return procedures;
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Extract SQL statements from Hive script.
     *
     * @param scriptCode The script code
     * @return A list of SQL statements
     */
    private List<SqlStatement> extractSqlStatements(String scriptCode) {
        List<SqlStatement> statements = new ArrayList<>();

        // Extract SQL statements
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(scriptCode);
        while (matcher.find()) {
            String sqlText = matcher.group().trim();
            try {
                SqlStatement statement = sqlParser.parse(sqlText);
                statements.add(statement);
            } catch (Exception e) {
                // Create a simple statement for unparseable SQL
                SqlStatement statement = SqlStatement.builder()
                        .sql(sqlText)
                        .type("UNKNOWN")
                        .dialect(DIALECT)
                        .build();
                statements.add(statement);
                log.warn("Created simple statement for unparseable SQL in Hive script: {}", sqlText, e);
            }
        }

        // Extract function calls as statements
        matcher = FUNCTION_CALL_PATTERN.matcher(scriptCode);
        while (matcher.find()) {
            String callText = matcher.group().trim();
            // Skip if already added as SQL statement
            if (statements.stream().anyMatch(s -> s.getSql().equals(callText))) {
                continue;
            }

            try {
                // Create a statement for the function call
                SqlStatement statement = SqlStatement.builder()
                        .sql(callText)
                        .type("CALL")
                        .dialect(DIALECT)
                        .build();
                statements.add(statement);
            } catch (Exception e) {
                log.warn("Failed to create statement for function call in Hive script: {}", callText, e);
            }
        }

        return statements;
    }
}
