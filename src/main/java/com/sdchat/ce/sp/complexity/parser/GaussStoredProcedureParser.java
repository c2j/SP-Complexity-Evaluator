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
 * Gauss stored procedure parser implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GaussStoredProcedureParser implements StoredProcedureParser {

    private static final String DIALECT = "Gauss";

    // Pattern to extract SQL statements from Gauss procedure code
    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)\\b[\\s\\S]*?;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract procedure calls
    private static final Pattern PROCEDURE_CALL_PATTERN = Pattern.compile(
            "\\b([\\w\\.]+)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    private final GaussSqlParser sqlParser;

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
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Extract SQL statements from Gauss procedure code.
     *
     * @param procedureCode The procedure code
     * @return A list of SQL statements
     */
    private List<SqlStatement> extractSqlStatements(String procedureCode) {
        List<SqlStatement> statements = new ArrayList<>();

        // Extract SQL statements
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(procedureCode);
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
                log.warn("Created simple statement for unparseable SQL in Gauss stored procedure: {}", sqlText, e);
            }
        }

        // Extract procedure calls as statements
        matcher = PROCEDURE_CALL_PATTERN.matcher(procedureCode);
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
                log.warn("Failed to create statement for procedure call in Gauss stored procedure: {}", callText, e);
            }
        }

        return statements;
    }
}
