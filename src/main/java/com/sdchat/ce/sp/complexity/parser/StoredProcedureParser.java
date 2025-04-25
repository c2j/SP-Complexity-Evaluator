package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;

import java.util.List;

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
     * Parse a package body and extract all procedures.
     *
     * @param sourceCode The source code of the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @return A list of parsed stored procedure objects
     * @throws Exception If parsing fails
     */
    List<StoredProcedure> parsePackageBody(String sourceCode, String packageName, String schema) throws Exception;

    /**
     * Check if the source code represents a package body.
     *
     * @param sourceCode The source code to check
     * @return true if the source code is a package body, false otherwise
     */
    boolean isPackageBody(String sourceCode);

    /**
     * Get the dialect supported by this parser.
     *
     * @return The SQL dialect name
     */
    String getDialect();
}
