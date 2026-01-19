package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureCallMetric {
    private String procedureName;
    private int callCount;
    private boolean calledInLoop;
    
    /**
     * Whether this is an explicit procedure call (using CALL/EXECUTE IMMEDIATE keywords)
     * Explicit calls are unambiguous procedure invocations
     */
    private boolean isExplicit;
    
    /**
     * Whether this is an internal procedure call (defined in the same package)
     * Internal calls are within the same package, external calls are to different packages/schemas
     */
    private boolean isInternal;
    
    /**
     * Whether the called procedure is defined in the uploaded ZIP file
     * Only applicable for multi-file ZIP batch processing
     */
    private boolean isDefinedInZip;
    
    /**
     * The file name where the called procedure is defined (if tracked)
     * Only available when procedure definitions are indexed from ZIP files
     */
    private String definitionFile;
}