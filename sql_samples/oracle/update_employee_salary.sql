CREATE OR REPLACE PROCEDURE update_employee_salary(
    p_employee_id IN NUMBER,
    p_new_salary IN NUMBER,
    p_effective_date IN DATE
) AS
    v_old_salary NUMBER;
    v_department_id NUMBER;
    v_job_id VARCHAR2(10);
BEGIN
    -- Get current employee data
    SELECT salary, department_id, job_id INTO v_old_salary, v_department_id, v_job_id
    FROM employees
    WHERE employee_id = p_employee_id;
    
    -- Update employee salary
    UPDATE employees
    SET salary = p_new_salary
    WHERE employee_id = p_employee_id;
    
    -- Log salary change
    INSERT INTO salary_history(
        employee_id,
        old_salary,
        new_salary,
        change_date,
        change_reason
    ) VALUES (
        p_employee_id,
        v_old_salary,
        p_new_salary,
        p_effective_date,
        'Annual Review'
    );
    
    -- Update department budget if necessary
    UPDATE department_budgets
    SET salary_budget = salary_budget + (p_new_salary - v_old_salary)
    WHERE department_id = v_department_id;
    
    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20001, 'Employee not found');
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END update_employee_salary;
