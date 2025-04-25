-- 包含JOIN的Hive查询
SELECT 
    o.order_id,
    c.customer_name,
    o.order_date,
    o.total_amount
FROM 
    orders o
JOIN 
    customers c ON o.customer_id = c.customer_id
WHERE 
    o.order_date >= '2023-01-01'
    AND o.total_amount > 100
ORDER BY 
    o.order_date DESC;
