package com.sdchat.ce.sp.complexity.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a complete set of weight factors for complexity evaluation.
 * Contains both SQL and stored procedure weights.
 */
@Data
@NoArgsConstructor
@Slf4j
public class WeightConfiguration {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String dialect;
    private Boolean isDefault = true;
    private Long createdAt = System.currentTimeMillis();

    // SQL weights - for statement evaluation
    private Double tableCount = 1.0;
    private Double joinCount = 2.0;
    private Double whereConditionCount = 1.5;
    private Double subqueryCount = 3.0;
    private Double aggregateFunctionCount = 1.0;
    private Double caseExpressionCount = 1.0;
    private Double setOperationCount = 2.0;
    private Double groupByCount = 1.5;
    private Double orderByCount = 1.0;

    // Hive-specific weights
    private Double lateralViewCount = 0.0;
    private Double distributeByCount = 0.0;
    private Double clusterByCount = 0.0;
    private Double sortByCount = 0.0;
    private Double partitionByCount = 0.0;
    private Double windowFunctionCount = 0.0;

    // Stored procedure weights
    private Double loopCount = 2.5;
    private Double nestedLoopLevel = 1.5;
    private Double customFunctionCount = 2.0;
    private Double highWeightTableCount = 2.0;
    private Double nestedProcedureCount = 3.0;
    private Double highWeightProcedureCount = 2.5;
    private Double cursorCount = 2.0;

    // Lists for tracking custom values
    private List<String> customFunctions = new ArrayList<>();
    private List<String> highWeightTables = new ArrayList<>();
    private List<String> highWeightProcedures = new ArrayList<>();

    public WeightConfiguration() {
    }

    public WeightConfiguration(String dialect) {
        this.dialect = dialect;
        setDefaultsForDialect(dialect);
    }

    public void setDefaultsForDialect(String dialect) {
        switch (dialect.toUpperCase()) {
            case "ORACLE":
                setOracleDefaults();
                break;
            case "GAUSS":
                setGaussDefaults();
                break;
            case "HIVE":
                setHiveDefaults();
                break;
            default:
                setOracleDefaults(); // Default to Oracle
        }
    }

    private void setOracleDefaults() {
        this.tableCount = 1.0;
        this.joinCount = 2.0;
        this.whereConditionCount = 1.5;
        this.subqueryCount = 3.0;
        this.aggregateFunctionCount = 1.0;
        this.caseExpressionCount = 1.0;
        this.setOperationCount = 2.0;
        this.groupByCount = 1.5;
        this.orderByCount = 1.0;

        // Reset Hive-specific weights for Oracle
        this.lateralViewCount = 0.0;
        this.distributeByCount = 0.0;
        this.clusterByCount = 0.0;
        this.sortByCount = 0.0;
        this.partitionByCount = 0.0;
        this.windowFunctionCount = 0.0;
    }

    private void setGaussDefaults() {
        this.tableCount = 1.0;
        this.joinCount = 2.0;
        this.whereConditionCount = 1.0;  // Different from Oracle
        this.subqueryCount = 3.0;
        this.aggregateFunctionCount = 1.5;  // Different from Oracle
        this.caseExpressionCount = 1.5;
        this.setOperationCount = 2.0;
        this.groupByCount = 1.5;
        this.orderByCount = 1.0;

        // Reset Hive-specific weights for Gauss
        this.lateralViewCount = 0.0;
        this.distributeByCount = 0.0;
        this.clusterByCount = 0.0;
        this.sortByCount = 0.0;
        this.partitionByCount = 0.0;
        this.windowFunctionCount = 0.0;
    }

    private void setHiveDefaults() {
        this.tableCount = 1.0;
        this.joinCount = 2.0;
        this.whereConditionCount = 1.5;
        this.subqueryCount = 3.0;
        this.aggregateFunctionCount = 1.0;
        this.caseExpressionCount = 1.5;
        this.setOperationCount = 2.0;
        this.groupByCount = 1.5;
        this.orderByCount = 1.0;

        // Set Hive-specific weights
        this.lateralViewCount = 2.0;
        this.distributeByCount = 1.5;
        this.clusterByCount = 1.5;
        this.sortByCount = 1.0;
        this.partitionByCount = 1.5;
        this.windowFunctionCount = 2.5;

        // Reset SQL weights for Hive (same as Oracle)
        this.loopCount = 2.5;
        this.nestedLoopLevel = 1.5;
        this.customFunctionCount = 2.0;
        this.highWeightTableCount = 2.0;
        this.nestedProcedureCount = 3.0;
        this.highWeightProcedureCount = 2.5;
        this.cursorCount = 2.0;
    }

    // Getters and setters for all weights
    public Map<String, Double> getAllSqlWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("tableCount", tableCount);
        weights.put("joinCount", joinCount);
        weights.put("whereConditionCount", whereConditionCount);
        weights.put("subqueryCount", subqueryCount);
        weights.put("aggregateFunctionCount", aggregateFunctionCount);
        weights.put("caseExpressionCount", caseExpressionCount);
        weights.put("setOperationCount", setOperationCount);
        weights.put("groupByCount", groupByCount);
        weights.put("orderByCount", orderByCount);

        // Add Hive-specific weights if applicable
        if (lateralViewCount > 0) {
            weights.put("lateralViewCount", lateralViewCount);
        }
        if (distributeByCount > 0) {
            weights.put("distributeByCount", distributeByCount);
        }
        if (clusterByCount > 0) {
            weights.put("clusterByCount", clusterByCount);
        }
        if (sortByCount > 0) {
            weights.put("sortByCount", sortByCount);
        }
        if (partitionByCount > 0) {
            weights.put("partitionByCount", partitionByCount);
        }
        if (windowFunctionCount > 0) {
            weights.put("windowFunctionCount", windowFunctionCount);
        }

        return weights;
    }

    public Map<String, Double> getAllProcedureWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("loopCount", loopCount);
        weights.put("nestedLoopLevel", nestedLoopLevel);
        weights.put("customFunctionCount", customFunctionCount);
        weights.put("highWeightTableCount", highWeightTableCount);
        weights.put("nestedProcedureCount", nestedProcedureCount);
        weights.put("highWeightProcedureCount", highWeightProcedureCount);
        weights.put("cursorCount", cursorCount);

        return weights;
    }

    // Custom lists management
    public void addCustomFunction(String functionName) {
        if (!customFunctions.contains(functionName)) {
            customFunctions.add(functionName);
        }
    }

    public void removeCustomFunction(String functionName) {
        customFunctions.remove(functionName);
    }

    public void addHighWeightTable(String tableName) {
        if (!highWeightTables.contains(tableName)) {
            highWeightTables.add(tableName);
        }
    }

    public void removeHighWeightTable(String tableName) {
        highWeightTables.remove(tableName);
    }

    public void addHighWeightProcedure(String procedureName) {
        if (!highWeightProcedures.contains(procedureName)) {
            highWeightProcedures.add(procedureName);
        }
    }

    public void removeHighWeightProcedure(String procedureName) {
        highWeightProcedures.remove(procedureName);
    }
    }

    public List<String> getCustomFunctions() {
        return new ArrayList<>(customFunctions);
    }

    public List<String> getHighWeightTables() {
        return new ArrayList<>(highWeightTables);
    }

    public List<String> getHighWeightProcedures() {
        return new ArrayList<>(highWeightProcedures);
    }
}