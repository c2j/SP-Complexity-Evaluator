-- 使用自定义函数的Hive脚本
-- 注意：这里假设这些UDF已经在Hive中注册

-- 创建一个包含用户行为数据的表
DROP TABLE IF EXISTS user_behavior_analysis;
CREATE TABLE user_behavior_analysis AS
SELECT 
    user_id,
    session_id,
    page_url,
    calculate_session_duration(start_time, end_time) AS session_duration,
    parse_user_agent(user_agent) AS device_info,
    extract_page_category(page_url) AS page_category,
    calculate_engagement_score(click_count, scroll_depth, time_on_page) AS engagement_score
FROM 
    raw_user_sessions
WHERE 
    session_date >= '2023-01-01';

-- 分析用户行为模式
DROP TABLE IF EXISTS user_behavior_patterns;
CREATE TABLE user_behavior_patterns AS
SELECT 
    user_id,
    COUNT(DISTINCT session_id) AS total_sessions,
    AVG(session_duration) AS avg_session_duration,
    format_duration(AVG(session_duration)) AS formatted_avg_duration,
    get_most_visited_category(user_id) AS favorite_category,
    calculate_user_persona(user_id) AS user_persona,
    predict_churn_probability(user_id) AS churn_probability
FROM 
    user_behavior_analysis
GROUP BY 
    user_id;

-- 识别高风险流失用户
DROP TABLE IF EXISTS churn_risk_users;
CREATE TABLE churn_risk_users AS
SELECT 
    user_id,
    user_persona,
    churn_probability,
    get_recommended_actions(user_persona, churn_probability) AS recommended_actions,
    calculate_retention_offer(user_id, churn_probability) AS retention_offer_type,
    format_date(CURRENT_DATE) AS analysis_date
FROM 
    user_behavior_patterns
WHERE 
    churn_probability > 0.7
ORDER BY 
    churn_probability DESC;
