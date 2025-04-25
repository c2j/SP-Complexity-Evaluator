-- 复杂的Hive查询，包含Hive特有功能
WITH daily_sales AS (
    SELECT 
        product_id,
        order_date,
        SUM(quantity) AS total_quantity,
        SUM(price * quantity) AS total_sales
    FROM 
        order_items
    WHERE 
        order_date BETWEEN '2023-01-01' AND '2023-12-31'
    GROUP BY 
        product_id, order_date
)
SELECT 
    p.product_name,
    p.category,
    ds.order_date,
    ds.total_quantity,
    ds.total_sales,
    AVG(ds.total_sales) OVER (PARTITION BY p.category ORDER BY ds.order_date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS moving_avg_7day
FROM 
    daily_sales ds
JOIN 
    products p ON ds.product_id = p.product_id
WHERE 
    p.category IN ('Electronics', 'Clothing', 'Home Goods')
DISTRIBUTE BY 
    p.category
SORT BY 
    p.category, ds.order_date;
