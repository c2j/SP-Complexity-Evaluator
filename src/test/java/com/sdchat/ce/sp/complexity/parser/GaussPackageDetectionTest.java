package com.sdchat.ce.sp.complexity.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GaussPackageDetectionTest {

    @Autowired
    private GaussStoredProcedureParser parser;

    @Test
    void testNestedProcedureCallsIsNotPackageBody() throws Exception {
        // Read the actual file content
        String sourceCode = Files.readString(Paths.get("sql_samples/gauss/nested_procedure_calls.sql"));
        
        // Test that it's NOT identified as a package body
        boolean isPackageBody = parser.isPackageBody(sourceCode);
        
        System.out.println("Source code length: " + sourceCode.length());
        System.out.println("First 200 chars: " + sourceCode.substring(0, Math.min(200, sourceCode.length())));
        System.out.println("Is package body: " + isPackageBody);
        
        assertFalse(isPackageBody, "nested_procedure_calls.sql should NOT be identified as a package body");
    }
    
    @Test
    void testSimpleNestedProcedureIsNotPackageBody() {
        String sourceCode = "CREATE OR REPLACE PROCEDURE process_monthly_payroll(\n" +
                "    p_month IN NUMBER,\n" +
                "    p_year IN NUMBER,\n" +
                "    p_department_id IN NUMBER DEFAULT NULL,\n" +
                "    p_result OUT NUMBER\n" +
                ") AS\n" +
                "    -- Constants\n" +
                "    c_tax_rate CONSTANT NUMBER := 0.25;\n" +
                "    \n" +
                "    -- Nested procedure to process department payroll\n" +
                "    PROCEDURE process_department_payroll(\n" +
                "        p_dept_id IN NUMBER,\n" +
                "        p_dept_name IN VARCHAR2,\n" +
                "        p_start_date IN DATE,\n" +
                "        p_end_date IN DATE,\n" +
                "        p_dept_result OUT NUMBER\n" +
                "    ) AS\n" +
                "        -- Nested procedure to process employee payroll\n" +
                "        PROCEDURE process_employee_payroll(\n" +
                "            p_emp_id IN NUMBER\n" +
                "        ) AS\n" +
                "        BEGIN\n" +
                "            NULL;\n" +
                "        END process_employee_payroll;\n" +
                "    BEGIN\n" +
                "        NULL;\n" +
                "    END process_department_payroll;\n" +
                "    \n" +
                "BEGIN\n" +
                "    -- Main procedure logic\n" +
                "    COMMIT;\n" +
                "END;";
        
        boolean isPackageBody = parser.isPackageBody(sourceCode);
        
        System.out.println("Simple nested procedure is package body: " + isPackageBody);
        
        assertFalse(isPackageBody, "Simple nested procedure should NOT be identified as a package body");
    }
    
    @Test
    void testActualPackageBodyIsPackageBody() {
        String sourceCode = "CREATE OR REPLACE PACKAGE BODY test_pkg AS\n" +
                "    PROCEDURE proc1 AS\n" +
                "    BEGIN\n" +
                "        NULL;\n" +
                "    END;\n" +
                "    \n" +
                "    PROCEDURE proc2 AS\n" +
                "    BEGIN\n" +
                "        NULL;\n" +
                "    END;\n" +
                "END test_pkg;";
        
        boolean isPackageBody = parser.isPackageBody(sourceCode);
        
        System.out.println("Actual package body is package body: " + isPackageBody);
        
        assertTrue(isPackageBody, "Actual package body should be identified as a package body");
    }
}