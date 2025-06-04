package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * Represents the complexity metrics at the package level.
 * A package may contain multiple stored procedures and functions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageComplexityMetrics {
    
    /**
     * The name of the package
     */
    private String packageName;
    
    /**
     * Total number of procedures and functions in the package
     */
    private int totalProcedures;
    
    /**
     * Number of internal procedure/function calls (calls to procedures/functions within the same package)
     */
    private int internalProcedureCalls;
    
    /**
     * Number of external procedure/function calls (calls to procedures/functions outside the package)
     */
    private int externalProcedureCalls;
    
    /**
     * Average complexity score of all procedures/functions in the package
     */
    private double averageProcedureComplexity;
    
    /**
     * Maximum complexity score of any procedure/function in the package
     */
    private double maxProcedureComplexity;
    
    /**
     * Name of the procedure with the highest complexity score
     */
    private String mostComplexProcedureName;
    
    /**
     * Procedure dependency map (procedure name -> set of procedures it calls)
     */
    private Map<String, Set<String>> procedureDependencies;
    
    /**
     * Number of variables defined at package level
     */
    private int packageLevelVariables;
    
    /**
     * Number of constants defined at package level
     */
    private int packageLevelConstants;
    
    /**
     * Number of types defined at package level
     */
    private int packageLevelTypes;
    
    /**
     * Total lines of code in the package (including all procedures)
     */
    private int totalLinesOfCode;
    
    /**
     * Flag indicating whether the package includes Java stored procedures
     */
    private boolean containsJavaProcedures;
    
    /**
     * Flag indicating whether the package has a specification separate from its body
     */
    private boolean hasSpecificationAndBody;
    
    /**
     * Maximum procedure call chain depth (how deep the call hierarchy goes)
     */
    private int maxCallChainDepth;
    
    /**
     * Flag indicating whether the package uses global temporary tables
     */
    private boolean usesGlobalTemporaryTables;
    
    /**
     * Flag indicating whether the package accesses database tables or views
     */
    private boolean accessesDatabaseObjects;
    
    /**
     * Flag indicating whether the package contains direct GRANT/REVOKE privilege statements
     */
    private boolean containsPrivilegeStatements;
}