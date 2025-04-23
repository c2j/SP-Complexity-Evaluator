package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ComplexityEvaluationServiceTest {

    @Autowired
    private ComplexityEvaluationService service;
    
    @Test
    void evaluateSqlStatement() throws Exception {
        String sql = "SELECT e.employee_id, e.first_name, e.last_name, d.department_name " +
                "FROM employees e " +
                "JOIN departments d ON e.department_id = d.department_id " +
                "WHERE e.salary > 5000";
        
        ComplexityMetrics metrics = service.evaluateSqlStatement(sql, "Oracle");
        
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertEquals(2, metrics.getTableCount());
        assertEquals(1, metrics.getJoinCount());
        assertTrue(metrics.getWhereConditionCount() > 0);
    }
    
    @Test
    void evaluateStoredProcedure() throws Exception {
        String sourceCode = "CREATE OR REPLACE PROCEDURE get_employee_details(\n" +
                "    p_employee_id IN NUMBER,\n" +
                "    p_result OUT SYS_REFCURSOR\n" +
                ") AS\n" +
                "BEGIN\n" +
                "    OPEN p_result FOR\n" +
                "    SELECT e.employee_id, e.first_name, e.last_name, d.department_name\n" +
                "    FROM employees e\n" +
                "    JOIN departments d ON e.department_id = d.department_id\n" +
                "    WHERE e.employee_id = p_employee_id;\n" +
                "    \n" +
                "    -- Update employee access timestamp\n" +
                "    UPDATE employee_access_log\n" +
                "    SET last_accessed = SYSDATE\n" +
                "    WHERE employee_id = p_employee_id;\n" +
                "    \n" +
                "    -- If no record exists, insert one\n" +
                "    IF SQL%ROWCOUNT = 0 THEN\n" +
                "        INSERT INTO employee_access_log (employee_id, last_accessed)\n" +
                "        VALUES (p_employee_id, SYSDATE);\n" +
                "    END IF;\n" +
                "    \n" +
                "    COMMIT;\n" +
                "EXCEPTION\n" +
                "    WHEN OTHERS THEN\n" +
                "        ROLLBACK;\n" +
                "        RAISE;\n" +
                "END get_employee_details;";
        
        ComplexityMetrics metrics = service.evaluateStoredProcedure(sourceCode, "get_employee_details", "HR", "Oracle");
        
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertTrue(metrics.getTableCount() >= 3); // employees, departments, employee_access_log
    }
    
    @Test
    void evaluateInvalidSqlStatement() {
        String invalidSql = "SELECT * FROM";
        
        Exception exception = assertThrows(Exception.class, () -> {
            service.evaluateSqlStatement(invalidSql, "Oracle");
        });
        
        assertTrue(exception.getMessage().contains("Failed to parse SQL statement"));
    }
}
