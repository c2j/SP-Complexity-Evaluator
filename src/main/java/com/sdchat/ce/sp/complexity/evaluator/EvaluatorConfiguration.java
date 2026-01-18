package com.sdchat.ce.sp.complexity.evaluator;

import java.util.List;

/**
 * Interface for configuring evaluators with custom weights.
 * All evaluator implementations (Oracle, Gauss, Hive) should implement this interface.
 */
public interface EvaluatorConfiguration {

    /**
     * Sets the list of custom function names that should be excluded from complexity calculation.
     *
     * @param customFunctions List of custom function names
     */
    void setCustomFunctions(List<String> customFunctions);

    /**
     * Sets the list of high-weight table names.
     *
     * @param highWeightTables List of high-weight table names
     */
    void setHighWeightTables(List<String> highWeightTables);

    /**
     * Sets the list of high-weight stored procedure names.
     *
     * @param highWeightProcedures List of high-weight procedure names
     */
    void setHighWeightProcedures(List<String> highWeightProcedures);
}
