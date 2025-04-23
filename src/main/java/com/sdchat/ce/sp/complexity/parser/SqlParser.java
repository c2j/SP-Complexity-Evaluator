package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;

import java.util.List;

/**
 * Interface for SQL parsers that can parse SQL statements from text.
 */
public interface SqlParser {
    
    /**
     * Parse a single SQL statement.
     *
     * @param sql The SQL statement to parse
     * @return The parsed SQL statement object
     * @throws Exception If parsing fails
     */
    SqlStatement parse(String sql) throws Exception;
    
    /**
     * Parse multiple SQL statements from a text that may contain multiple statements.
     *
     * @param sqlText The text containing SQL statements
     * @return A list of parsed SQL statement objects
     * @throws Exception If parsing fails
     */
    List<SqlStatement> parseMultiple(String sqlText) throws Exception;
    
    /**
     * Get the dialect supported by this parser.
     *
     * @return The SQL dialect name
     */
    String getDialect();
}
