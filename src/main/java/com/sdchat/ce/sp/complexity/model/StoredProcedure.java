package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a stored procedure with its name, parameters, and SQL statements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredProcedure {
    
    /**
     * The name of the stored procedure
     */
    private String name;
    
    /**
     * The schema/owner of the stored procedure
     */
    private String schema;
    
    /**
     * The source code of the stored procedure
     */
    private String sourceCode;
    
    /**
     * The list of SQL statements in the stored procedure
     */
    private List<SqlStatement> sqlStatements;
    
    /**
     * The database dialect (Oracle, MySQL, PostgreSQL, etc.)
     */
    private String dialect;
}
