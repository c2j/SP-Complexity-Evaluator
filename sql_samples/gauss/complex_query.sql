SELECT e.employee_id, e.first_name, e.last_name, d.department_name, j.job_title, 
       (SELECT AVG(salary) FROM employees WHERE department_id = e.department_id) as avg_dept_salary, 
       CASE WHEN e.salary > 10000 THEN 'High' WHEN e.salary > 5000 THEN 'Medium' ELSE 'Low' END as salary_category,
       TO_CHAR(e.hire_date, 'YYYY-MM-DD') as hire_date_str
FROM employees e 
JOIN departments d ON e.department_id = d.department_id 
JOIN jobs j ON e.job_id = j.job_id 
JOIN locations l ON d.location_id = l.location_id 
WHERE e.hire_date > TO_DATE('2010-01-01', 'YYYY-MM-DD') 
AND (e.job_id = 'IT_PROG' OR e.job_id = 'SA_REP') 
AND EXISTS (SELECT 1 FROM job_history jh WHERE jh.employee_id = e.employee_id) 
ORDER BY e.department_id, e.salary DESC
