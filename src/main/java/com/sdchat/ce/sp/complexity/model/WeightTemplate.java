package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a saved weight configuration template that users can save and reuse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class WeightTemplate {
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();
    private String name;
    private String dialect;
    @Builder.Default
    private Boolean isShared = false;
    private String createdBy;
    @Builder.Default
    private Long createdAt = System.currentTimeMillis();
    @Builder.Default
    private Long usageCount = 0L;

    private WeightConfiguration weights;

    public WeightTemplate(String name, String dialect, Boolean isShared, String createdBy) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.dialect = dialect;
        this.isShared = isShared;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.weights = WeightConfiguration.builder().dialect(dialect).build();
        this.weights.setDefaultsForDialect(dialect);
    }

    public WeightTemplate(String name, String dialect, Boolean isShared, WeightConfiguration weights, String createdBy) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.dialect = dialect;
        this.isShared = isShared;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.weights = weights;
    }
}