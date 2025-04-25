-- Hive数据处理脚本（模拟存储过程）
-- 在Hive中没有真正的存储过程，但可以使用脚本来执行一系列操作

-- 创建临时表存储处理后的数据
DROP TABLE IF EXISTS temp_processed_sales;
CREATE TABLE temp_processed_sales AS
SELECT 
    s.sale_id,
    s.product_id,
    p.product_name,
    s.customer_id,
    c.customer_name,
    s.sale_date,
    s.quantity,
    s.unit_price,
    s.quantity * s.unit_price AS total_amount,
    CASE 
        WHEN s.quantity * s.unit_price > 1000 THEN 'High Value'
        WHEN s.quantity * s.unit_price > 500 THEN 'Medium Value'
        ELSE 'Low Value'
    END AS sale_category
FROM 
    sales s
JOIN 
    products p ON s.product_id = p.product_id
JOIN 
    customers c ON s.customer_id = c.customer_id
WHERE 
    s.sale_date >= '2023-01-01';

-- 计算每个客户的销售统计
DROP TABLE IF EXISTS customer_sales_stats;
CREATE TABLE customer_sales_stats AS
SELECT 
    customer_id,
    customer_name,
    COUNT(*) AS total_transactions,
    SUM(total_amount) AS total_spent,
    AVG(total_amount) AS avg_transaction_value,
    MAX(sale_date) AS last_purchase_date
FROM 
    temp_processed_sales
GROUP BY 
    customer_id, customer_name;

-- 计算每个产品的销售统计
DROP TABLE IF EXISTS product_sales_stats;
CREATE TABLE product_sales_stats AS
SELECT 
    product_id,
    product_name,
    COUNT(*) AS total_sales,
    SUM(quantity) AS total_quantity_sold,
    SUM(total_amount) AS total_revenue
FROM 
    temp_processed_sales
GROUP BY 
    product_id, product_name;

-- 识别高价值客户
DROP TABLE IF EXISTS high_value_customers;
CREATE TABLE high_value_customers AS
SELECT 
    customer_id,
    customer_name,
    total_spent,
    total_transactions,
    avg_transaction_value,
    last_purchase_date,
    CASE
        WHEN total_spent > 10000 THEN 'Platinum'
        WHEN total_spent > 5000 THEN 'Gold'
        WHEN total_spent > 2000 THEN 'Silver'
        ELSE 'Bronze'
    END AS customer_tier
FROM 
    customer_sales_stats
WHERE 
    total_spent > 1000
ORDER BY 
    total_spent DESC;

-- 清理临时表
DROP TABLE temp_processed_sales;
