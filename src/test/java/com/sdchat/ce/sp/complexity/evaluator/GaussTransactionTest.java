package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.parser.GaussStoredProcedureParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GaussTransactionTest {

    @Autowired
    private GaussComplexityEvaluator evaluator;

    @Autowired
    private GaussStoredProcedureParser parser;

    @BeforeEach
    void setUp() {
        evaluator.setHighWeightTables(new ArrayList<>());
        evaluator.setCustomFunctions(new ArrayList<>());
        evaluator.setHighWeightProcedures(new ArrayList<>());
    }

    @Test
    void testNestedProcedureCallsFile() throws Exception {
        // Read the actual SQL file
        String sqlContent = Files.readString(Paths.get("sql_samples/gauss/nested_procedure_calls.sql"));
        
        // Parse the stored procedure
        StoredProcedure procedure = parser.parse(sqlContent, "process_monthly_payroll", "test_schema");
        
        // Evaluate the procedure
        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedure);
        
        assertNotNull(metrics);
        
        System.out.println("=== Transaction Analysis Results ===");
        System.out.println("Transaction Control Count: " + metrics.getTransactionControlCount());
        System.out.println("Transaction Nesting Level: " + metrics.getTransactionNestingLevel());
        System.out.println("Uses Autonomous Transactions: " + metrics.isUsesAutonomousTransactions());
        System.out.println("Subtransaction Count: " + metrics.getSubtransactionCount());
        System.out.println("Max Subtransaction Nesting Level: " + metrics.getMaxSubtransactionNestingLevel());
        System.out.println("Subtransaction Details: " + metrics.getSubtransactionDetails());
        
        // The file should have transaction control statements
        assertTrue(metrics.getTransactionControlCount() > 0, 
                  "Should detect COMMIT and ROLLBACK statements");
        
        // Should have some transaction nesting level
        assertTrue(metrics.getTransactionNestingLevel() > 0, 
                  "Should have transaction nesting level > 0");
        
        // Should detect implicit subtransactions in exception blocks
        assertTrue(metrics.getSubtransactionCount() > 0, 
                  "Should detect implicit subtransactions in exception blocks");
    }

    @Test
    void testSimpleTransactionControlDetection() throws Exception {
        String simpleTransactionCode = 
            "CREATE PROCEDURE test_transaction AS\n" +
            "BEGIN\n" +
            "  INSERT INTO test_table VALUES (1, 'test');\n" +
            "  COMMIT;\n" +
            "  \n" +
            "  BEGIN\n" +
            "    UPDATE test_table SET name = 'updated';\n" +
            "  EXCEPTION\n" +
            "    WHEN OTHERS THEN\n" +
            "      ROLLBACK;\n" +
            "  END;\n" +
            "END;";

        StoredProcedure procedure = parser.parse(simpleTransactionCode, "test_transaction", "test_schema");
        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedure);

        assertNotNull(metrics);
        
        System.out.println("=== Simple Transaction Test Results ===");
        System.out.println("Transaction Control Count: " + metrics.getTransactionControlCount());
        System.out.println("Transaction Nesting Level: " + metrics.getTransactionNestingLevel());
        System.out.println("Subtransaction Count: " + metrics.getSubtransactionCount());
        
        // Should detect COMMIT and ROLLBACK
        assertTrue(metrics.getTransactionControlCount() >= 2, 
                  "Should detect at least COMMIT and ROLLBACK");
        
        // Should detect implicit subtransaction in exception block
        assertTrue(metrics.getSubtransactionCount() >= 1, 
                  "Should detect implicit subtransaction in exception block");
    }
}