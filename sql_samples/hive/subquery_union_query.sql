-- 包含子查询和UNION ALL的Hive查询
SELECT 
    'Current Year' AS period,
    category,
    SUM(sales_amount) AS total_sales
FROM 
    sales
WHERE 
    sale_date BETWEEN '2023-01-01' AND '2023-12-31'
    AND category IN (
        SELECT DISTINCT category 
        FROM top_categories 
        WHERE year = 2023
    )
GROUP BY 
    category

UNION ALL

SELECT 
    'Previous Year' AS period,
    category,
    SUM(sales_amount) AS total_sales
FROM 
    sales
WHERE 
    sale_date BETWEEN '2022-01-01' AND '2022-12-31'
    AND category IN (
        SELECT DISTINCT category 
        FROM top_categories 
        WHERE year = 2022
    )
GROUP BY 
    category
ORDER BY 
    category, period;
