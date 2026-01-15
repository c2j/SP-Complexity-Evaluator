package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a built-in function from the gaussdb_functions.json file.
 * Used for filtering database built-in functions from complexity analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuiltInFunction {

    /**
     * Function name (e.g., "gs_index_advise")
     */
    private String name;

    /**
     * Category from JSON (e.g., "AI特性函数", "HashFunc函数")
     */
    private String category;

    /**
     * Parameter signature
     */
    private String parameters;

    /**
     * Function description
     */
    private String description;

    /**
     * Return type information
     */
    private String returnType;
}
