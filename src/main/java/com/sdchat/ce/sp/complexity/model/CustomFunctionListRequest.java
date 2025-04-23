package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for custom function list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFunctionListRequest {
    
    /**
     * List of custom function names
     */
    private List<String> functionNames;
}
