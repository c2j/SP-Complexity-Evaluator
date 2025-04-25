-- 简单的Hive查询
SELECT 
    customer_id,
    customer_name,
    city,
    state
FROM 
    customers
WHERE 
    state = 'CA'
ORDER BY 
    customer_name;
