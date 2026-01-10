package com.sdchat.ce.sp.complexity.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a saved weight configuration template that users can save and reuse.
 */
@Data
@NoArgsConstructor
@Slf4j
public class WeightTemplate {
    private String id = java.util.UUID.randomUUID().toString();
    private String name;
    private String dialect;
    private Boolean isShared = false;
    private String createdBy;
    private Long createdAt = System.currentTimeMillis();
    private Long usageCount = 0L;

    // Contains the actual weight configuration
    private WeightConfiguration weights;

    public WeightTemplate() {
    }

    public WeightTemplate(String name, String dialect, Boolean isShared, String createdBy) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.dialect = dialect;
        this.isShared = isShared;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.weights = new WeightConfiguration();
        this.weights.setDefaultsForDialect(dialect);
    }
}