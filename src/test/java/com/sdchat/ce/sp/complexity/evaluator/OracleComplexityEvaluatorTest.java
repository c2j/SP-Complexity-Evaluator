package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.parser.OracleSqlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OracleComplexityEvaluatorTest {

    @Autowired
    private OracleComplexityEvaluator evaluator;
    
    @Autowired
    private OracleSqlParser sqlParser;
    
    private SqlStatement simpleSelectStatement;
    private SqlStatement complexSelectStatement;
    
    @BeforeEach
    void setUp() throws Exception {
        // Simple SELECT statement
        String simpleSelect = "SELECT * FROM employees WHERE department_id = 10";
        simpleSelectStatement = sqlParser.parse(simpleSelect);
        
        // Complex SELECT statement with joins, subqueries, and aggregations
        String complexSelect = "SELECT e.employee_id, e.first_name, e.last_name, " +
                "d.department_name, " +
                "(SELECT AVG(salary) FROM employees WHERE department_id = e.department_id) as avg_dept_salary, " +
                "CASE WHEN e.salary > 5000 THEN 'High' ELSE 'Low' END as salary_category " +
                "FROM employees e " +
                "JOIN departments d ON e.department_id = d.department_id " +
                "JOIN locations l ON d.location_id = l.location_id " +
                "WHERE e.hire_date > TO_DATE('2010-01-01', 'YYYY-MM-DD') " +
                "AND (e.job_id = 'IT_PROG' OR e.job_id = 'SA_REP') " +
                "ORDER BY e.department_id, e.salary DESC";
        complexSelectStatement = sqlParser.parse(complexSelect);
    }
    
    @Test
    void evaluateSimpleSqlStatement() throws Exception {
        ComplexityMetrics metrics = evaluator.evaluateSqlStatement(simpleSelectStatement);
        
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertEquals(1, metrics.getTableCount());
        assertEquals(0, metrics.getJoinCount());
        assertTrue(metrics.getWhereConditionCount() > 0);
        assertEquals(0, metrics.getSubqueryCount());
    }
    
    @Test
    void evaluateComplexSqlStatement() throws Exception {
        ComplexityMetrics metrics = evaluator.evaluateSqlStatement(complexSelectStatement);
        
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertTrue(metrics.getTableCount() >= 3);
        assertTrue(metrics.getJoinCount() >= 2);
        assertTrue(metrics.getWhereConditionCount() > 0);
        assertTrue(metrics.getSubqueryCount() > 0);
        assertTrue(metrics.getCaseExpressionCount() > 0);
    }
    
    @Test
    void compareComplexityScores() throws Exception {
        ComplexityMetrics simpleMetrics = evaluator.evaluateSqlStatement(simpleSelectStatement);
        ComplexityMetrics complexMetrics = evaluator.evaluateSqlStatement(complexSelectStatement);
        
        assertTrue(complexMetrics.getOverallScore() > simpleMetrics.getOverallScore(),
                "Complex query should have higher complexity score than simple query");
    }
}
