package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.*;
import com.sdchat.ce.sp.complexity.parser.OracleSqlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OracleComplexityEvaluatorTest {

    @Autowired
    private OracleComplexityEvaluator evaluator;

    @Autowired
    private OracleSqlParser sqlParser;

    private StoredProcedure testProcedure;

    @BeforeEach
    void setUp() throws Exception {
        testProcedure = StoredProcedure.builder()
                .name("process_employee_data")
                .schema("HR")
                .sourceCode("CREATE OR REPLACE PROCEDURE process_employee_data AS\n" +
                        "BEGIN\n" +
                        "  GET_DATA(1);\n" +
                        "  UPDATE_EMPLOYEE(100, 5000);\n" +
                        "  FOR i IN 1..5 LOOP\n" +
                        "    GET_DATA(i);\n" +
                        "    UPDATE_EMPLOYEE(i, i * 1000);\n" +
                        "  END LOOP;\n" +
                        "  GET_DATA(10);\n" +
                        "END;")
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("OTHER").sql("  GET_DATA(1);").dialect("Oracle").build(),
                        SqlStatement.builder().type("OTHER").sql("  UPDATE_EMPLOYEE(100, 5000);").dialect("Oracle").build(),
                        SqlStatement.builder().type("FOR").sql("  FOR i IN 1..5 LOOP").dialect("Oracle").build(),
                        SqlStatement.builder().type("OTHER").sql("    GET_DATA(i);").dialect("Oracle").build(),
                        SqlStatement.builder().type("OTHER").sql("    UPDATE_EMPLOYEE(i, i * 1000);").dialect("Oracle").build(),
                        SqlStatement.builder().type("OTHER").sql("  END LOOP;").dialect("Oracle").build(),
                        SqlStatement.builder().type("OTHER").sql("  GET_DATA(10);").dialect("Oracle").build()
                ))
                .dialect("Oracle")
                .build();
    }

    @Test
    void evaluateStoredProcedure_WithProcedureCalls() throws Exception {
        evaluator.setCustomFunctions(new ArrayList<>());
        evaluator.setHighWeightTables(new ArrayList<>());
        evaluator.setHighWeightProcedures(new ArrayList<>());

        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(testProcedure);

        assertNotNull(metrics);
        assertEquals(5, metrics.getProcedureCallCount());
        assertNotNull(metrics.getProcedureCallDetails());
        assertEquals(2, metrics.getProcedureCallDetails().size());

        boolean getDataFound = false;
        boolean updateEmployeeFound = false;

        for (ProcedureCallMetric detail : metrics.getProcedureCallDetails()) {
            if ("GET_DATA".equals(detail.getProcedureName())) {
                getDataFound = true;
                assertEquals(3, detail.getCallCount());
                assertTrue(detail.isCalledInLoop());
            }
            if ("UPDATE_EMPLOYEE".equals(detail.getProcedureName())) {
                updateEmployeeFound = true;
                assertEquals(2, detail.getCallCount());
                assertTrue(detail.isCalledInLoop());
            }
        }

        assertTrue(getDataFound);
        assertTrue(updateEmployeeFound);
    }
}
