CREATE OR REPLACE PROCEDURE process_data_with_loops(
    p_start_date IN DATE,
    p_end_date IN DATE
) AS
    v_current_date DATE;
    v_department_id NUMBER;
    v_employee_id NUMBER;
    v_total_processed NUMBER := 0;
    v_error_count NUMBER := 0;
    
    -- Cursor for departments
    CURSOR c_departments IS
        SELECT department_id, department_name
        FROM departments
        WHERE status = 'ACTIVE';
        
    -- Cursor for employees in a department
    CURSOR c_employees(p_dept_id NUMBER) IS
        SELECT employee_id, first_name, last_name
        FROM employees
        WHERE department_id = p_dept_id
        AND hire_date <= p_end_date
        AND (termination_date IS NULL OR termination_date >= p_start_date);
        
    -- Cursor for tasks assigned to an employee
    CURSOR c_tasks(p_emp_id NUMBER) IS
        SELECT task_id, task_name, priority
        FROM employee_tasks
        WHERE employee_id = p_emp_id
        AND due_date BETWEEN p_start_date AND p_end_date
        ORDER BY priority DESC;
BEGIN
    -- Initialize processing log
    INSERT INTO processing_log(process_date, start_date, end_date, status)
    VALUES (SYSDATE, p_start_date, p_end_date, 'STARTED');
    
    -- Set current date to start date
    v_current_date := p_start_date;
    
    -- Loop through each day in the date range
    WHILE v_current_date <= p_end_date LOOP
        -- Log daily processing
        INSERT INTO daily_processing_log(process_date, status)
        VALUES (v_current_date, 'PROCESSING');
        
        -- Process each department
        FOR dept_rec IN c_departments LOOP
            v_department_id := dept_rec.department_id;
            
            -- Process employees in department
            FOR emp_rec IN c_employees(v_department_id) LOOP
                v_employee_id := emp_rec.employee_id;
                v_total_processed := v_total_processed + 1;
                
                BEGIN
                    -- Process tasks for employee
                    FOR task_rec IN c_tasks(v_employee_id) LOOP
                        -- Process each task based on priority
                        CASE task_rec.priority
                            WHEN 'HIGH' THEN
                                -- Process high priority tasks immediately
                                UPDATE employee_tasks
                                SET status = 'IN_PROGRESS',
                                    assigned_date = v_current_date
                                WHERE task_id = task_rec.task_id;
                                
                                -- Additional processing for high priority tasks
                                FOR i IN 1..3 LOOP
                                    -- Simulate multiple processing steps
                                    INSERT INTO task_processing_steps(
                                        task_id,
                                        step_number,
                                        process_date,
                                        status
                                    ) VALUES (
                                        task_rec.task_id,
                                        i,
                                        v_current_date + (i-1)/24, -- Add hours
                                        'COMPLETED'
                                    );
                                END LOOP;
                                
                            WHEN 'MEDIUM' THEN
                                -- Process medium priority tasks
                                UPDATE employee_tasks
                                SET status = 'SCHEDULED',
                                    assigned_date = v_current_date + 1
                                WHERE task_id = task_rec.task_id;
                                
                            ELSE
                                -- Process low priority tasks
                                UPDATE employee_tasks
                                SET status = 'QUEUED',
                                    assigned_date = v_current_date + 3
                                WHERE task_id = task_rec.task_id;
                        END CASE;
                    END LOOP; -- End of tasks loop
                    
                    -- Update employee processing status
                    UPDATE employee_processing
                    SET last_processed_date = v_current_date,
                        processed_count = NVL(processed_count, 0) + 1
                    WHERE employee_id = v_employee_id;
                    
                EXCEPTION
                    WHEN OTHERS THEN
                        v_error_count := v_error_count + 1;
                        
                        -- Log error
                        INSERT INTO processing_errors(
                            process_date,
                            employee_id,
                            error_message
                        ) VALUES (
                            v_current_date,
                            v_employee_id,
                            SUBSTR(SQLERRM, 1, 200)
                        );
                END;
            END LOOP; -- End of employees loop
            
            -- Update department processing status
            UPDATE department_processing
            SET last_processed_date = v_current_date,
                processed_count = NVL(processed_count, 0) + 1
            WHERE department_id = v_department_id;
            
        END LOOP; -- End of departments loop
        
        -- Update daily processing log
        UPDATE daily_processing_log
        SET status = 'COMPLETED',
            records_processed = v_total_processed,
            error_count = v_error_count
        WHERE process_date = v_current_date;
        
        -- Move to next day
        v_current_date := v_current_date + 1;
    END LOOP; -- End of date range loop
    
    -- Update final processing status
    UPDATE processing_log
    SET status = CASE WHEN v_error_count > 0 THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,
        total_processed = v_total_processed,
        error_count = v_error_count,
        completion_date = SYSDATE
    WHERE process_date = SYSDATE
    AND start_date = p_start_date
    AND end_date = p_end_date;
    
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        -- Log fatal error
        INSERT INTO processing_errors(
            process_date,
            error_message,
            error_type
        ) VALUES (
            SYSDATE,
            SUBSTR(SQLERRM, 1, 200),
            'FATAL'
        );
        
        -- Update processing status to failed
        UPDATE processing_log
        SET status = 'FAILED',
            error_message = SUBSTR(SQLERRM, 1, 200),
            completion_date = SYSDATE
        WHERE process_date = SYSDATE
        AND start_date = p_start_date
        AND end_date = p_end_date;
        
        ROLLBACK;
        RAISE;
END process_data_with_loops;
