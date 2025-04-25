package com.sdchat.ce.sp.complexity.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to collect and store SQL statements that failed during parsing.
 * Uses ThreadLocal to ensure thread safety.
 */
public class SqlParserExceptionCollector {
    
    private static final ThreadLocal<List<String>> FAILED_STATEMENTS = ThreadLocal.withInitial(ArrayList::new);
    
    /**
     * Add a list of failed SQL statements to the collector.
     *
     * @param statements The list of failed SQL statements
     */
    public static void addFailedStatements(List<String> statements) {
        FAILED_STATEMENTS.get().addAll(statements);
    }
    
    /**
     * Add a single failed SQL statement to the collector.
     *
     * @param statement The failed SQL statement
     */
    public static void addFailedStatement(String statement) {
        FAILED_STATEMENTS.get().add(statement);
    }
    
    /**
     * Get all failed SQL statements collected so far.
     *
     * @return The list of failed SQL statements
     */
    public static List<String> getFailedStatements() {
        return new ArrayList<>(FAILED_STATEMENTS.get());
    }
    
    /**
     * Check if there are any failed SQL statements.
     *
     * @return True if there are failed SQL statements, false otherwise
     */
    public static boolean hasFailedStatements() {
        return !FAILED_STATEMENTS.get().isEmpty();
    }
    
    /**
     * Clear all failed SQL statements.
     */
    public static void clear() {
        FAILED_STATEMENTS.get().clear();
    }
    
    /**
     * Remove the ThreadLocal value to prevent memory leaks.
     * Should be called when processing is complete.
     */
    public static void remove() {
        FAILED_STATEMENTS.remove();
    }
}
