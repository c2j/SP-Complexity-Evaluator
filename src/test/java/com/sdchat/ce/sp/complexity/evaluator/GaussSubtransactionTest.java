package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GaussSubtransactionTest {

    @Autowired
    private GaussComplexityEvaluator evaluator;

    private StoredProcedure procedureWithExplicitSubtransactions;
    private StoredProcedure procedureWithImplicitSubtransactions;
    private StoredProcedure procedureWithNestedSubtransactions;

    @BeforeEach
    void setUp() throws Exception {
        // Set up the evaluator with the necessary lists
        evaluator.setHighWeightTables(new ArrayList<>());
        evaluator.setCustomFunctions(new ArrayList<>());
        evaluator.setHighWeightProcedures(new ArrayList<>());

        // Procedure with explicit subtransactions (SAVEPOINT/ROLLBACK TO SAVEPOINT)
        procedureWithExplicitSubtransactions = StoredProcedure.builder()
                .name("test_schema.proc_with_explicit_subtrans")
                .schema("test_schema")
                .sourceCode("CREATE PROCEDURE proc_with_explicit_subtrans AS\n" +
                        "BEGIN\n" +
                        "  SAVEPOINT sp1;\n" +
                        "  INSERT INTO test_table (id, name) VALUES (1, 'test1');\n" +
                        "  UPDATE test_table SET name = 'updated' WHERE id = 1;\n" +
                        "  IF error_condition THEN\n" +
                        "    ROLLBACK TO SAVEPOINT sp1;\n" +
                        "  END IF;\n" +
                        "  \n" +
                        "  SAVEPOINT sp2;\n" +
                        "  DELETE FROM test_table WHERE id = 2;\n" +
                        "  IF another_error THEN\n" +
                        "    ROLLBACK TO SAVEPOINT sp2;\n" +
                        "  END IF;\n" +
                        "  \n" +
                        "  COMMIT;\n" +
                        "END;")
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("INSERT").sql("INSERT INTO test_table (id, name) VALUES (1, 'test1')").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("UPDATE").sql("UPDATE test_table SET name = 'updated' WHERE id = 1").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("DELETE").sql("DELETE FROM test_table WHERE id = 2").tableList(Arrays.asList("test_table")).build()
                ))
                .dialect("Gauss")
                .build();

        // Procedure with implicit subtransactions (exception blocks)
        procedureWithImplicitSubtransactions = StoredProcedure.builder()
                .name("test_schema.proc_with_implicit_subtrans")
                .schema("test_schema")
                .sourceCode("CREATE PROCEDURE proc_with_implicit_subtrans AS\n" +
                        "BEGIN\n" +
                        "  BEGIN\n" +
                        "    INSERT INTO test_table (id, name) VALUES (1, 'test1');\n" +
                        "    UPDATE test_table SET name = 'updated' WHERE id = 1;\n" +
                        "  EXCEPTION\n" +
                        "    WHEN OTHERS THEN\n" +
                        "      INSERT INTO error_log (message) VALUES ('Error occurred');\n" +
                        "      ROLLBACK;\n" +
                        "  END;\n" +
                        "  \n" +
                        "  BEGIN\n" +
                        "    DELETE FROM test_table WHERE id = 2;\n" +
                        "  EXCEPTION\n" +
                        "    WHEN OTHERS THEN\n" +
                        "      UPDATE error_log SET count = count + 1;\n" +
                        "  END;\n" +
                        "END;")
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("INSERT").sql("INSERT INTO test_table (id, name) VALUES (1, 'test1')").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("UPDATE").sql("UPDATE test_table SET name = 'updated' WHERE id = 1").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("DELETE").sql("DELETE FROM test_table WHERE id = 2").tableList(Arrays.asList("test_table")).build()
                ))
                .dialect("Gauss")
                .build();

        // Procedure with nested subtransactions
        procedureWithNestedSubtransactions = StoredProcedure.builder()
                .name("test_schema.proc_with_nested_subtrans")
                .schema("test_schema")
                .sourceCode("CREATE PROCEDURE proc_with_nested_subtrans AS\n" +
                        "BEGIN\n" +
                        "  SAVEPOINT outer_sp;\n" +
                        "  INSERT INTO test_table (id, name) VALUES (1, 'test1');\n" +
                        "  \n" +
                        "  SAVEPOINT inner_sp;\n" +
                        "  UPDATE test_table SET name = 'updated' WHERE id = 1;\n" +
                        "  \n" +
                        "  IF inner_error THEN\n" +
                        "    ROLLBACK TO SAVEPOINT inner_sp;\n" +
                        "  END IF;\n" +
                        "  \n" +
                        "  IF outer_error THEN\n" +
                        "    ROLLBACK TO SAVEPOINT outer_sp;\n" +
                        "  END IF;\n" +
                        "  \n" +
                        "  COMMIT;\n" +
                        "END;")
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("INSERT").sql("INSERT INTO test_table (id, name) VALUES (1, 'test1')").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("UPDATE").sql("UPDATE test_table SET name = 'updated' WHERE id = 1").tableList(Arrays.asList("test_table")).build()
                ))
                .dialect("Gauss")
                .build();
    }

    @Test
    void evaluateStoredProcedure_WithExplicitSubtransactions() throws Exception {
        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedureWithExplicitSubtransactions);

        assertNotNull(metrics);
        assertNotNull(metrics.getSubtransactionCount());
        assertNotNull(metrics.getSubtransactionDetails());
        assertNotNull(metrics.getMaxSubtransactionNestingLevel());

        // Should detect 2 explicit subtransactions (sp1 and sp2)
        assertEquals(2, metrics.getSubtransactionCount().intValue());
        
        // Should have some nesting level
        assertTrue(metrics.getMaxSubtransactionNestingLevel() >= 1);
        
        // Should have subtransaction details in JSON format
        assertNotNull(metrics.getSubtransactionDetails());
        assertFalse(metrics.getSubtransactionDetails().isEmpty());
        assertTrue(metrics.getSubtransactionDetails().startsWith("["));
        assertTrue(metrics.getSubtransactionDetails().endsWith("]"));

        System.out.println("Explicit Subtransactions - Count: " + metrics.getSubtransactionCount() + 
                          ", Max Nesting: " + metrics.getMaxSubtransactionNestingLevel() +
                          ", Details: " + metrics.getSubtransactionDetails());
    }

    @Test
    void evaluateStoredProcedure_WithImplicitSubtransactions() throws Exception {
        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedureWithImplicitSubtransactions);

        assertNotNull(metrics);
        assertNotNull(metrics.getSubtransactionCount());
        assertNotNull(metrics.getSubtransactionDetails());
        assertNotNull(metrics.getMaxSubtransactionNestingLevel());

        // Should detect implicit subtransactions in exception blocks
        assertTrue(metrics.getSubtransactionCount() >= 1);
        
        // Should have subtransaction details
        assertNotNull(metrics.getSubtransactionDetails());
        assertFalse(metrics.getSubtransactionDetails().isEmpty());

        System.out.println("Implicit Subtransactions - Count: " + metrics.getSubtransactionCount() + 
                          ", Max Nesting: " + metrics.getMaxSubtransactionNestingLevel() +
                          ", Details: " + metrics.getSubtransactionDetails());
    }

    @Test
    void evaluateStoredProcedure_WithNestedSubtransactions() throws Exception {
        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedureWithNestedSubtransactions);

        assertNotNull(metrics);
        assertNotNull(metrics.getSubtransactionCount());
        assertNotNull(metrics.getSubtransactionDetails());
        assertNotNull(metrics.getMaxSubtransactionNestingLevel());

        // Should detect nested subtransactions
        assertEquals(2, metrics.getSubtransactionCount().intValue());
        
        // Should have nesting level of at least 2 (nested savepoints)
        assertTrue(metrics.getMaxSubtransactionNestingLevel() >= 2);

        System.out.println("Nested Subtransactions - Count: " + metrics.getSubtransactionCount() + 
                          ", Max Nesting: " + metrics.getMaxSubtransactionNestingLevel() +
                          ", Details: " + metrics.getSubtransactionDetails());
    }

    @Test
    void evaluateStoredProcedure_WithoutSubtransactions() throws Exception {
        StoredProcedure simpleProc = StoredProcedure.builder()
                .name("test_schema.simple_proc")
                .schema("test_schema")
                .sourceCode("CREATE PROCEDURE simple_proc AS\n" +
                        "BEGIN\n" +
                        "  INSERT INTO test_table (id, name) VALUES (1, 'test1');\n" +
                        "  UPDATE test_table SET name = 'updated' WHERE id = 1;\n" +
                        "  COMMIT;\n" +
                        "END;")
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("INSERT").sql("INSERT INTO test_table (id, name) VALUES (1, 'test1')").tableList(Arrays.asList("test_table")).build(),
                        SqlStatement.builder().type("UPDATE").sql("UPDATE test_table SET name = 'updated' WHERE id = 1").tableList(Arrays.asList("test_table")).build()
                ))
                .dialect("Gauss")
                .build();

        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(simpleProc);

        assertNotNull(metrics);
        
        // Should have 0 subtransactions
        assertEquals(0, metrics.getSubtransactionCount().intValue());
        assertEquals(0, metrics.getMaxSubtransactionNestingLevel().intValue());
        assertEquals("[]", metrics.getSubtransactionDetails());

        System.out.println("No Subtransactions - Count: " + metrics.getSubtransactionCount() + 
                          ", Max Nesting: " + metrics.getMaxSubtransactionNestingLevel() +
                          ", Details: " + metrics.getSubtransactionDetails());
    }
}