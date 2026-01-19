package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintAnalysisSummary {
    
    private Integer totalHints;
    private Integer validCount;
    private Integer invalidCount;
    private List<HintValidationResult> validHints;
    private List<HintValidationResult> invalidHints;
    private Map<String, Integer> categoryBreakdown;
    private Map<String, Integer> errorTypeBreakdown;
    
    public int getTotalHints() {
        return totalHints != null ? totalHints : 0;
    }
    
    public int getValidCount() {
        return validCount != null ? validCount : 0;
    }
    
    public int getInvalidCount() {
        return invalidCount != null ? invalidCount : 0;
    }
}
