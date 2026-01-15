package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RealFileTest {
    @Autowired
    private GaussComplexityEvaluator evaluator;

    @Test
    void testRealSQLFile() throws IOException, Exception {
        String sql = new String(Files.readAllBytes(Paths.get("sql_samples/gauss/b.sql")));
        
        StoredProcedure procedure = StoredProcedure.builder()
                .name("PROC_UPDATE_BALANCE111")
                .schema("HR")
                .sourceCode(sql)
                .sqlStatements(Arrays.asList(
                        SqlStatement.builder().type("OTHER").sql("BEGIN").dialect("Gauss").build()
                ))
                .dialect("Gauss")
                .build();
        
        evaluator.setCustomFunctions(new ArrayList<>());
        evaluator.setHighWeightTables(new ArrayList<>());
        evaluator.setHighWeightProcedures(new ArrayList<>());

        ComplexityMetrics metrics = evaluator.evaluateStoredProcedure(procedure);

        System.out.println("Procedure call count: " + metrics.getProcedureCallCount());
        System.out.println("Procedure call details: " + metrics.getProcedureCallDetails());
        System.out.println("Nested procedure list: " + metrics.getNestedProcedureList());
        System.out.println("Nested procedure count: " + metrics.getNestedProcedureCount());
    }
}
