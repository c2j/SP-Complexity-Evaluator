package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE)\\b[\\s\\S]*?;",
            Pattern.CASE_INSENSITIVE
    );
    
    private final OracleSqlParser sqlParser;
    
    @Override
    public StoredProcedure parse(String sourceCode, String name, String schema) throws Exception {
        List<SqlStatement> sqlStatements = extractSqlStatements(sourceCode);
        
        return StoredProcedure.builder()
                .name(name)
                .schema(schema)
                .sourceCode(sourceCode)
                .sqlStatements(sqlStatements)
                .dialect(DIALECT)
                .build();
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
        
        Matcher matcher = SQL_STATEMENT_PATTERN.matcher(plsqlCode);
        while (matcher.find()) {
            String sqlText = matcher.group().trim();
            try {
                SqlStatement statement = sqlParser.parse(sqlText);
                statements.add(statement);
            } catch (Exception e) {
                log.warn("Failed to parse SQL statement in stored procedure: {}", sqlText, e);
            }
        }
        
        return statements;
    }
}
