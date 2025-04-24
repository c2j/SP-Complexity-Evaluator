package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a collection of complexity metrics, typically for procedures in a package body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityMetricsCollection {
    
    /**
     * The list of complexity metrics for individual procedures
     */
    private List<ComplexityMetrics> procedures;
    
    /**
     * The name of the package
     */
    private String packageName;
    
    /**
     * The schema/owner of the package
     */
    private String schema;
    
    /**
     * The database dialect (Oracle, Gauss, etc.)
     */
    private String dialect;
}
