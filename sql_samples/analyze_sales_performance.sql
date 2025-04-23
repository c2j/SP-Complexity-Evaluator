CREATE OR REPLACE PROCEDURE analyze_sales_performance(
    p_year IN NUMBER,
    p_quarter IN NUMBER,
    p_result OUT SYS_REFCURSOR
) AS
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    -- Calculate date range for the specified quarter
    v_start_date := TO_DATE(p_year || '-' || (p_quarter * 3 - 2) || '-01', 'YYYY-MM-DD');
    v_end_date := ADD_MONTHS(v_start_date, 3) - 1;
    
    -- Open cursor with complex query
    OPEN p_result FOR
        SELECT 
            r.region_name,
            c.country_name,
            p.product_name,
            p.category,
            SUM(s.sales_amount) as total_sales,
            COUNT(DISTINCT s.customer_id) as customer_count,
            SUM(s.sales_amount) / COUNT(DISTINCT s.customer_id) as avg_sales_per_customer,
            SUM(s.sales_amount) / SUM(s.quantity) as avg_unit_price,
            SUM(s.sales_amount) / 
                (SELECT SUM(s2.sales_amount) 
                 FROM sales s2 
                 WHERE s2.sale_date BETWEEN v_start_date AND v_end_date) * 100 as pct_of_total_sales,
            (SELECT AVG(s3.sales_amount) 
             FROM sales s3 
             WHERE s3.product_id = p.product_id 
             AND s3.sale_date BETWEEN ADD_MONTHS(v_start_date, -12) AND ADD_MONTHS(v_end_date, -12)) as prev_year_avg_sales,
            CASE 
                WHEN SUM(s.sales_amount) > 
                    (SELECT AVG(total_sales) * 1.5 
                     FROM (SELECT SUM(s4.sales_amount) as total_sales 
                           FROM sales s4 
                           JOIN products p4 ON s4.product_id = p4.product_id 
                           WHERE s4.sale_date BETWEEN v_start_date AND v_end_date 
                           AND p4.category = p.category 
                           GROUP BY p4.product_id)) 
                THEN 'High Performer' 
                WHEN SUM(s.sales_amount) < 
                    (SELECT AVG(total_sales) * 0.5 
                     FROM (SELECT SUM(s5.sales_amount) as total_sales 
                           FROM sales s5 
                           JOIN products p5 ON s5.product_id = p5.product_id 
                           WHERE s5.sale_date BETWEEN v_start_date AND v_end_date 
                           AND p5.category = p.category 
                           GROUP BY p5.product_id)) 
                THEN 'Underperformer' 
                ELSE 'Average' 
            END as performance_category,
            (SELECT COUNT(*) 
             FROM (
                 SELECT s6.customer_id 
                 FROM sales s6 
                 WHERE s6.product_id = p.product_id 
                 AND s6.sale_date BETWEEN v_start_date AND v_end_date 
                 INTERSECT 
                 SELECT s7.customer_id 
                 FROM sales s7 
                 WHERE s7.product_id != p.product_id 
                 AND s7.sale_date BETWEEN v_start_date AND v_end_date 
             )) as cross_selling_customers
        FROM 
            sales s
            JOIN products p ON s.product_id = p.product_id
            JOIN customers cust ON s.customer_id = cust.customer_id
            JOIN locations l ON cust.location_id = l.location_id
            JOIN countries c ON l.country_id = c.country_id
            JOIN regions r ON c.region_id = r.region_id
        WHERE 
            s.sale_date BETWEEN v_start_date AND v_end_date
            AND s.status = 'COMPLETED'
            AND EXISTS (
                SELECT 1 
                FROM product_inventory pi 
                WHERE pi.product_id = p.product_id 
                AND pi.quantity_available > 0
            )
        GROUP BY 
            r.region_name,
            c.country_name,
            p.product_id,
            p.product_name,
            p.category
        HAVING 
            SUM(s.sales_amount) > (
                SELECT AVG(sales_amount) * 0.1 
                FROM sales 
                WHERE sale_date BETWEEN v_start_date AND v_end_date
            )
        ORDER BY 
            total_sales DESC;
EXCEPTION
    WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20001, 'Error analyzing sales performance: ' || SQLERRM);
END analyze_sales_performance;
