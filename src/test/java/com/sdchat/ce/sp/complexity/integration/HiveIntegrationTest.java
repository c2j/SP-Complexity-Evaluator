package com.sdchat.ce.sp.complexity.integration;

import com.sdchat.ce.sp.complexity.evaluator.HiveComplexityEvaluator;
import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.parser.HiveSqlParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HiveIntegrationTest {

    @Autowired
    private HiveSqlParser hiveSqlParser;

    @Autowired
    private HiveComplexityEvaluator hiveComplexityEvaluator;

    @Test
    public void testHiveSimpleQueryIntegration() throws Exception {
        String sql = "-- 简单的Hive查询\n" +
                "SELECT \n" +
                "    customer_id,\n" +
                "    customer_name,\n" +
                "    city,\n" +
                "    state\n" +
                "FROM \n" +
                "    customers\n" +
                "WHERE \n" +
                "    state = 'CA'\n" +
                "ORDER BY \n" +
                "    customer_name;";

        // Test parser
        SqlStatement statement = hiveSqlParser.parse(sql);
        assertNotNull(statement);
        assertEquals("SELECT", statement.getType());
        assertEquals("Hive", statement.getDialect());
        assertNotNull(statement.getTableList());
        assertEquals(1, statement.getTableList().size());
        assertEquals("customers", statement.getTableList().get(0));

        // Test evaluator
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertEquals(1, metrics.getTableCount());
        assertNotNull(metrics.getTableList());
        assertEquals(1, metrics.getTableList().size());
        assertEquals("customers", metrics.getTableList().get(0));
        assertEquals(1, metrics.getWhereConditionCount());
        assertEquals(1, metrics.getOrderByCount());
    }

    @Test
    public void testHiveComplexQueryIntegration() throws Exception {
        String sql = "WITH daily_sales AS (\n" +
                "    SELECT \n" +
                "        product_id,\n" +
                "        order_date,\n" +
                "        SUM(quantity) AS total_quantity,\n" +
                "        SUM(price * quantity) AS total_sales\n" +
                "    FROM \n" +
                "        order_items\n" +
                "    WHERE \n" +
                "        order_date BETWEEN '2023-01-01' AND '2023-12-31'\n" +
                "    GROUP BY \n" +
                "        product_id, order_date\n" +
                ")\n" +
                "SELECT \n" +
                "    p.product_name,\n" +
                "    p.category,\n" +
                "    ds.order_date,\n" +
                "    ds.total_quantity,\n" +
                "    ds.total_sales,\n" +
                "    AVG(ds.total_sales) OVER (PARTITION BY p.category ORDER BY ds.order_date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS moving_avg_7day\n" +
                "FROM \n" +
                "    daily_sales ds\n" +
                "JOIN \n" +
                "    products p ON ds.product_id = p.product_id\n" +
                "WHERE \n" +
                "    p.category IN ('Electronics', 'Clothing', 'Home Goods')\n" +
                "DISTRIBUTE BY \n" +
                "    p.category\n" +
                "SORT BY \n" +
                "    p.category, ds.order_date;";

        // Test parser
        SqlStatement statement = hiveSqlParser.parse(sql);
        assertNotNull(statement);
        assertEquals("SELECT", statement.getType());
        assertEquals("Hive", statement.getDialect());
        assertNotNull(statement.getTableList());
        assertTrue(statement.getTableList().size() >= 3); // daily_sales, order_items, products

        // Test evaluator
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 5); // Should be significantly higher than simple query
        assertTrue(metrics.getTableCount() >= 3);
        assertTrue(metrics.getJoinCount() >= 1);
        assertTrue(metrics.getAggregateFunctionCount() >= 2); // SUM, AVG
        assertTrue(metrics.getGroupByCount() >= 1);
        assertTrue(metrics.getWhereConditionCount() >= 2);
    }

    @Test
    public void testHiveLateralViewIntegration() throws Exception {
        String sql = "SELECT \n" +
                "    user_id,\n" +
                "    tag\n" +
                "FROM \n" +
                "    user_tags\n" +
                "LATERAL VIEW explode(tags) tag_table AS tag\n" +
                "WHERE \n" +
                "    tag IS NOT NULL;";

        // Test parser
        SqlStatement statement = hiveSqlParser.parse(sql);
        assertNotNull(statement);
        assertEquals("SELECT", statement.getType());
        assertEquals("Hive", statement.getDialect());
        assertNotNull(statement.getTableList());
        assertEquals(1, statement.getTableList().size());
        assertEquals("user_tags", statement.getTableList().get(0));

        // Test evaluator
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 0);
        assertEquals(1, metrics.getTableCount());
        assertEquals(1, metrics.getWhereConditionCount());
        // Should detect LATERAL VIEW as a complexity factor
    }

    @Test
    public void testHiveUnionComplexityIntegration() throws Exception {
        String sql = "SELECT product_id, sales FROM q1_sales " +
                "UNION ALL " +
                "SELECT product_id, sales FROM q2_sales " +
                "UNION ALL " +
                "SELECT product_id, sales FROM q3_sales " +
                "UNION " +
                "SELECT product_id, sales FROM q4_sales;";

        // Test parser
        SqlStatement statement = hiveSqlParser.parse(sql);
        assertNotNull(statement);
        assertEquals("SELECT", statement.getType());
        assertEquals("Hive", statement.getDialect());

        // Test evaluator
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);
        assertNotNull(metrics);
        assertTrue(metrics.getOverallScore() > 10); // Should be significantly higher due to multiple UNIONs
        assertTrue(metrics.getTableCount() >= 4);
        assertTrue(metrics.getSetOperationCount() >= 3); // 3 UNION operations
    }

    @Test
    public void testHiveLongStatementComplexity() throws Exception {
        // Create a very long SQL statement to test length-based complexity
        StringBuilder longSql = new StringBuilder();
        longSql.append("-- This is a very long Hive query to test length-based complexity\n");
        longSql.append("WITH base_data AS (\n");
        longSql.append("    SELECT \n");
        for (int i = 1; i <= 50; i++) {
            longSql.append("        column").append(i).append(",\n");
        }
        longSql.append("        SUM(sales_amount) AS total_sales\n");
        longSql.append("    FROM very_large_table\n");
        longSql.append("    WHERE date_column >= '2023-01-01'\n");
        longSql.append("    GROUP BY ");
        for (int i = 1; i <= 50; i++) {
            longSql.append("column").append(i);
            if (i < 50) longSql.append(", ");
        }
        longSql.append("\n)\n");
        longSql.append("SELECT * FROM base_data ORDER BY total_sales DESC;");

        // Test parser
        SqlStatement statement = hiveSqlParser.parse(longSql.toString());
        assertNotNull(statement);
        assertEquals("SELECT", statement.getType());

        // Test evaluator
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);
        assertNotNull(metrics);

        // Should have higher complexity due to length
        assertTrue(metrics.getOverallScore() > 5);
        assertTrue(metrics.getLineCount() > 50);

        // Compare with a simple query to ensure length multiplier is applied
        String simpleQuery = "SELECT * FROM simple_table;";
        SqlStatement simpleStatement = hiveSqlParser.parse(simpleQuery);
        ComplexityMetrics simpleMetrics = hiveComplexityEvaluator.evaluateSqlStatement(simpleStatement);

        assertTrue(metrics.getOverallScore() > simpleMetrics.getOverallScore() * 2);
    }

    @Test
    public void testHiveSpecificJsonFields() throws Exception {
        String sql = "SELECT product_id, sales FROM q1_sales " +
                "UNION ALL " +
                "SELECT product_id, sales FROM q2_sales " +
                "UNION " +
                "SELECT product_id, sales FROM q3_sales;";

        // Test parser and evaluator
        SqlStatement statement = hiveSqlParser.parse(sql);
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);

        // Verify Hive-specific JSON fields are populated
        assertNotNull(metrics);

        // UNION metrics
        assertTrue(metrics.getUnionCount() >= 1); // Should have at least 1 UNION
        assertTrue(metrics.getUnionAllCount() >= 1); // Should have at least 1 UNION ALL
        assertEquals(metrics.getUnionCount() + metrics.getUnionAllCount(), metrics.getTotalUnionCount());
        assertTrue(metrics.getUnionDepth() >= 1);

        // Multipliers
        assertTrue(metrics.getLengthComplexityMultiplier() >= 1.0);
        assertTrue(metrics.getUnionNestingMultiplier() >= 1.0);

        // Character count and flags
        assertTrue(metrics.getCharacterCount() > 0);
        assertEquals(sql.length(), metrics.getCharacterCount());

        // Length flags should be consistent
        if (metrics.isLongStatement()) {
            assertTrue(metrics.getCharacterCount() > 1000);
        }
        if (metrics.isVeryLongStatement()) {
            assertTrue(metrics.getCharacterCount() > 5000);
        }
        if (metrics.isHasLargeLineCount()) {
            assertTrue(metrics.getLineCount() > 50);
        }
        if (metrics.isHasVeryLargeLineCount()) {
            assertTrue(metrics.getLineCount() > 200);
        }
    }

    @Test
    public void testHiveWithClauseJsonFields() throws Exception {
        String sql = "WITH sales_data AS (" +
                "    SELECT product_id, SUM(amount) as total " +
                "    FROM sales " +
                "    GROUP BY product_id" +
                ") " +
                "SELECT * FROM sales_data " +
                "WHERE total > 1000;";

        SqlStatement statement = hiveSqlParser.parse(sql);
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);

        // Verify WITH clause metrics
        assertNotNull(metrics);
        assertTrue(metrics.getWithClauseCount() >= 1);
        assertTrue(metrics.getNestedWithCount() >= 0); // May or may not have nested WITH
    }

    @Test
    public void testTableNameDeduplication() throws Exception {
        // Create a query that would naturally have duplicate table names
        String sql = "SELECT a.id, b.name, c.value " +
                "FROM test_table a " +
                "JOIN test_table b ON a.id = b.parent_id " +
                "LEFT JOIN other_table c ON a.id = c.ref_id " +
                "WHERE a.status = 'ACTIVE' " +
                "UNION ALL " +
                "SELECT x.id, y.name, z.value " +
                "FROM test_table x " +
                "JOIN test_table y ON x.id = y.parent_id " +
                "LEFT JOIN other_table z ON x.id = z.ref_id " +
                "WHERE x.status = 'INACTIVE';";

        SqlStatement statement = hiveSqlParser.parse(sql);
        ComplexityMetrics metrics = hiveComplexityEvaluator.evaluateSqlStatement(statement);

        // Verify table deduplication
        assertNotNull(metrics);
        assertNotNull(metrics.getTableList());

        // Should only have unique table names
        Set<String> uniqueTableNames = new HashSet<>(metrics.getTableList());
        assertEquals(uniqueTableNames.size(), metrics.getTableList().size(),
                    "Table list should not contain duplicates");

        // Should contain exactly 2 unique tables: test_table and other_table
        assertEquals(2, metrics.getTableCount());
        assertTrue(metrics.getTableList().contains("test_table"));
        assertTrue(metrics.getTableList().contains("other_table"));

        // Verify no duplicate entries in the list
        long testTableCount = metrics.getTableList().stream()
                .filter(table -> "test_table".equals(table))
                .count();
        long otherTableCount = metrics.getTableList().stream()
                .filter(table -> "other_table".equals(table))
                .count();

        assertEquals(1, testTableCount, "test_table should appear only once in the list");
        assertEquals(1, otherTableCount, "other_table should appear only once in the list");
    }
}
