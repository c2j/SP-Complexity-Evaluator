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
import java.util.stream.Collectors;

/**
 * Oracle stored procedure parser implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OracleStoredProcedureParser implements StoredProcedureParser {

    private static final String DIALECT = "Oracle";

    // Pattern to extract SQL statements from PL/SQL code
    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)\\b[\\s\\S]*?\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract procedure calls
    private static final Pattern PROCEDURE_CALL_PATTERN = Pattern.compile(
            "\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect package body
    private static final Pattern PACKAGE_BODY_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract package name from package body declaration
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\s+([\\w\\.]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract procedure definitions from package body
    private static final Pattern PROCEDURE_DEFINITION_PATTERN = Pattern.compile(
            "\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)\\s+(?:IS|AS)[\\s\\S]*?\\bEND\\s*(?:\\1)?\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    private final OracleSqlParser sqlParser;

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
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return false;
        }
        return PACKAGE_BODY_PATTERN.matcher(sourceCode.toUpperCase()).find();
    }

    @Override
    public List<StoredProcedure> parsePackageBody(String sourceCode, String packageName, String schema) throws Exception {
        if (!isPackageBody(sourceCode)) {
            throw new IllegalArgumentException("The provided source code is not a package body");
        }

        // Extract the actual package name from the source code
        String actualPackageName = packageName;
        Matcher packageNameMatcher = PACKAGE_NAME_PATTERN.matcher(sourceCode);
        if (packageNameMatcher.find()) {
            actualPackageName = packageNameMatcher.group(1);
            log.debug("Extracted package name: {}", actualPackageName);
        } else {
            log.warn("Could not extract package name from source code, using provided name: {}", packageName);
        }

        List<StoredProcedure> procedures = new ArrayList<>();
        String cleanSourceCode = SqlCommentRemover.removeComments(sourceCode);

        // Extract procedure definitions
        Matcher matcher = PROCEDURE_DEFINITION_PATTERN.matcher(cleanSourceCode);
        while (matcher.find()) {
            String procedureName = matcher.group(1);
            String procedureCode = matcher.group(0); // The entire procedure definition

            // Parse the procedure
            List<SqlStatement> sqlStatements = extractSqlStatements(procedureCode);

            // Create a StoredProcedure object with the fully qualified name
            String fullyQualifiedName = actualPackageName + "." + procedureName;
            StoredProcedure procedure = StoredProcedure.builder()
                    .name(fullyQualifiedName)
                    .schema(schema)
                    .sourceCode(procedureCode)
                    .sqlStatements(sqlStatements)
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
     * Extract SQL statements from PL/SQL code.
     *
     * @param plsqlCode The PL/SQL code
     * @return A list of SQL statements
     */
    private List<SqlStatement> extractSqlStatements(String plsqlCode) {
        List<SqlStatement> statements = new ArrayList<>();

        // Extract SQL statements
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(plsqlCode);
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
                log.warn("Created simple statement for unparseable SQL in stored procedure: {}", sqlText, e);
            }
        }

        // Extract procedure calls as statements
        matcher = PROCEDURE_CALL_PATTERN.matcher(plsqlCode);
        while (matcher.find()) {
            String callText = matcher.group().trim();
            // Skip if already added as SQL statement
            if (statements.stream().anyMatch(s -> s.getSql().equals(callText))) {
                continue;
            }

            try {
                // Create a statement for the procedure call
                SqlStatement statement = SqlStatement.builder()
                        .sql(callText)
                        .type("CALL")
                        .dialect(DIALECT)
                        .build();
                statements.add(statement);
            } catch (Exception e) {
                log.warn("Failed to create statement for procedure call in stored procedure: {}", callText, e);
            }
        }

        return statements;
    }
}
