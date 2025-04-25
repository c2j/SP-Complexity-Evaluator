-- 使用LATERAL VIEW的Hive查询
SELECT 
    u.user_id,
    u.user_name,
    t.tag
FROM 
    users u
LATERAL VIEW 
    explode(u.tags) t AS tag
WHERE 
    u.active = true
    AND t.tag IN ('premium', 'subscriber', 'vip')
CLUSTER BY 
    t.tag;
