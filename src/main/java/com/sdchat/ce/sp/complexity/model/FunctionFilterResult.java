package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the outcome of filtering operations for a single analysis result.
 * Contains information about which built-in functions were filtered out.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionFilterResult {

    /**
     * List of built-in functions that were filtered from analysis
     */
    private List<BuiltInFunction> filteredFunctions;

    /**
     * Total number of functions filtered
     */
    private int filteredCount;

    /**
     * Number of user-defined functions retained after filtering
     */
    private int retainedCount;

    /**
     * Count of filtered functions organized by category
     * (e.g., {"AI特性函数": 2, "HashFunc函数": 3})
     */
    private Map<String, Integer> categoryBreakdown;
}
