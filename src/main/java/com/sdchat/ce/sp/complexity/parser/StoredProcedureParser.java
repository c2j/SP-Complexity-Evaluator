package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;

/**
 * Interface for stored procedure parsers.
 */
public interface StoredProcedureParser {
    
    /**
     * Parse a stored procedure from its source code.
     *
     * @param sourceCode The source code of the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @return The parsed stored procedure object
     * @throws Exception If parsing fails
     */
    StoredProcedure parse(String sourceCode, String name, String schema) throws Exception;
    
    /**
     * Get the dialect supported by this parser.
     *
     * @return The SQL dialect name
     */
    String getDialect();
}
