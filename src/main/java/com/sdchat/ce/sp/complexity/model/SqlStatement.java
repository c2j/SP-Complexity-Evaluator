package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a SQL statement with its type and content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatement {
    
    /**
     * The type of SQL statement (SELECT, INSERT, UPDATE, DELETE, etc.)
     */
    private String type;
    
    /**
     * The SQL statement text
     */
    private String sql;
    
    /**
     * The database dialect (Oracle, MySQL, PostgreSQL, etc.)
     */
    private String dialect;
}
