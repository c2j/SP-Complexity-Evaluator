-- Gauss SQL with window functions
SELECT 
    e.employee_id, 
    e.first_name, 
    e.last_name, 
    e.department_id, 
    e.salary,
    -- Window functions
    ROW_NUMBER() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) as dept_salary_rank,
    RANK() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) as dept_salary_rank_with_ties,
    DENSE_RANK() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) as dept_salary_dense_rank,
    PERCENT_RANK() OVER (PARTITION BY e.department_id ORDER BY e.salary) as salary_percent_rank,
    -- Aggregate window functions
    AVG(e.salary) OVER (PARTITION BY e.department_id) as dept_avg_salary,
    SUM(e.salary) OVER (PARTITION BY e.department_id) as dept_total_salary,
    COUNT(*) OVER (PARTITION BY e.department_id) as dept_employee_count,
    -- Moving window calculations
    AVG(e.salary) OVER (PARTITION BY e.department_id ORDER BY e.hire_date ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING) as moving_avg_salary,
    -- Running totals
    SUM(e.salary) OVER (PARTITION BY e.department_id ORDER BY e.hire_date) as running_total_salary
FROM 
    employees e
WHERE 
    e.department_id IS NOT NULL
ORDER BY 
    e.department_id, 
    e.salary DESC;
