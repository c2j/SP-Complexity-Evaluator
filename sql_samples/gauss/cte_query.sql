-- Gauss SQL with Common Table Expressions (CTEs)
WITH 
dept_stats AS (
    SELECT 
        department_id,
        COUNT(*) as employee_count,
        AVG(salary) as avg_salary,
        MAX(salary) as max_salary,
        MIN(salary) as min_salary
    FROM 
        employees
    GROUP BY 
        department_id
),
high_salary_employees AS (
    SELECT 
        e.employee_id,
        e.first_name,
        e.last_name,
        e.department_id,
        e.salary,
        e.job_id
    FROM 
        employees e
    WHERE 
        e.salary > 10000
),
department_managers AS (
    SELECT 
        d.department_id,
        d.department_name,
        e.first_name || ' ' || e.last_name as manager_name
    FROM 
        departments d
    JOIN 
        employees e ON d.manager_id = e.employee_id
)
SELECT 
    hse.employee_id,
    hse.first_name,
    hse.last_name,
    dm.department_name,
    hse.salary,
    ds.avg_salary as dept_avg_salary,
    ds.employee_count as dept_employee_count,
    dm.manager_name,
    j.job_title
FROM 
    high_salary_employees hse
JOIN 
    dept_stats ds ON hse.department_id = ds.department_id
JOIN 
    department_managers dm ON hse.department_id = dm.department_id
JOIN 
    jobs j ON hse.job_id = j.job_id
WHERE 
    hse.salary > ds.avg_salary * 1.5
ORDER BY 
    hse.salary DESC;
