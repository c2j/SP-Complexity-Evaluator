CREATE OR REPLACE PROCEDURE insert_data(
    p_employee_id IN NUMBER,
    p_salary_increase IN NUMBER,
    p_effective_date IN DATE DEFAULT SYSDATE,
    p_update_history IN BOOLEAN DEFAULT TRUE,
    p_result OUT NUMBER) IS
    v_current_salary NUMBER;
    v_new_salary NUMBER;
    v_department_id NUMBER;
    v_job_id VARCHAR2(10);
    v_min_salary NUMBER;
    v_max_salary NUMBER;
    v_manager_id NUMBER;
    v_update_count NUMBER := 0;
    
    -- Custom exception for salary range validation
    e_salary_out_of_range EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_salary_out_of_range, -20001);
    
BEGIN -- 测试
    -- Get current employee information
    SELECT salary, department_id, job_id, manager_id
    INTO v_current_salary, v_department_id, v_job_id, v_manager_id
    FROM employees
    WHERE employee_id = p_employee_id;
    
    -- Get job salary range
    SELECT min_salary, max_salary
    INTO v_min_salary, v_max_salary
    FROM jobs
    WHERE job_id = v_job_id;
    
    -- Calculate new salary
    v_new_salary := v_current_salary + p_salary_increase;
    
    -- Validate new salary against job salary range
    IF v_new_salary < v_min_salary OR v_new_salary > v_max_salary THEN
        RAISE e_salary_out_of_range;
    END IF;
    
    -- Update employee salary
    UPDATE employees
    SET salary = v_new_salary,
        last_update_date = p_effective_date
    WHERE employee_id = p_employee_id;
    
    v_update_count := SQL%ROWCOUNT;
    
    -- Log salary change in history table if requested
    IF p_update_history = TRUE THEN
        INSERT INTO salary_history (
            employee_id,
            change_date,
            old_salary,
            new_salary,
            change_amount,
            change_percent,
            changed_by
        ) VALUES (
            p_employee_id,
            p_effective_date,
            v_current_salary,
            v_new_salary,
            p_salary_increase,
            ROUND((p_salary_increase / v_current_salary) * 100, 2),
            USER
        );
    END IF;
    
    -- Update department budget if necessary
    IF p_salary_increase > 0 THEN
        UPDATE department_budgets
        SET salary_budget = salary_budget + p_salary_increase,
            last_updated = p_effective_date
        WHERE department_id = v_department_id;
    END IF;
    
    -- Notify manager of salary change
    INSERT INTO notifications (
        recipient_id,
        notification_date,
        notification_type,
        notification_text
    ) VALUES (
        v_manager_id,
        SYSDATE,
        'SALARY_CHANGE',
        'Employee ID ' || p_employee_id || ' salary updated from ' || 
        v_current_salary || ' to ' || v_new_salary || ' effective ' || 
        TO_CHAR(p_effective_date, 'YYYY-MM-DD')
    );
    
    -- Set output parameter
    p_result := v_update_count;
    
    -- Commit the transaction
    COMMIT;
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- Employee or job not found
        p_result := 0;
        ROLLBACK;
        
    WHEN e_salary_out_of_range THEN
        -- Salary out of range for job
        p_result := -1;
        ROLLBACK;
        
    WHEN OTHERS THEN
        -- Other errors
        p_result := -2;
        ROLLBACK;
END ;
