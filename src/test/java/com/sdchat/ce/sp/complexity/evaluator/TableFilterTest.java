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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TableFilterTest {
    @Autowired
    private GaussComplexityEvaluator evaluator;

    private StoredProcedure procedure;

    @BeforeEach
    void setUp() throws Exception {
        String sql = "CREATE OR REPLACE PROCEDURE test_proc AS\n" +
                "BEGIN\n" +
                "  delete from facc_fiact_tmp t where t.workdate = '2024-01-01';\n" +
                "  insert into facc_fiact_tmp (currtype) values ('TEST');\n" +
                "  select 1 from facc_fiact_tmp where exists (select 1 from facc_fiact v);\n" +
                "END;";
        
        procedure = StoredProcedure.builder()
                .name("test_proc")
                .schema("HR")
                .sourceCode(sql)
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("OTHER").sql("  delete from facc_fiact_tmp t where t.workdate = '2024-01-01';").dialect("Gauss").build(),
                        SqlStatement.builder().type("OTHER").sql("  insert into facc_fiact_tmp (currtype) values ('TEST');").dialect("Gauss").build(),
                        SqlStatement.builder().type("OTHER").sql("  select 1 from facc_fiact_tmp where exists (select 1 from facc_fiact v);").dialect("Gauss").build()
                ))
                .dialect("Gauss")
                .build();
    }

    @Test
    void testTableNamesNotDetectedAsProcedureCalls() throws Exception {
        evaluator.setCustomFunctions(new ArrayList<>());
        evaluator.setHighWeightTables(new ArrayList<>());
        evaluator.setHighWeightProcedures(new ArrayList<>());

        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedure);

        assertNotNull(metrics);
        System.out.println("Procedure call count: " + metrics.getProcedureCallCount());
        System.out.println("Table list: " + metrics.getTableList());
        
        assertEquals(0, metrics.getProcedureCallCount(), 
            "Table names should not be detected as procedure calls");
        assertTrue(metrics.getTableList().contains("FACC_FIACT_TMP"), 
            "Table should be in table list");
    }
}
