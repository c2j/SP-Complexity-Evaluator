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

    private String name;

    private String schema;

    private String sourceCode;

    private List<SqlStatement> sqlStatements;

    private String dialect;

    private int lineOffset;
    
    private String fileName;
    
    private String packageName;
    
    private String sourceFile;
}
