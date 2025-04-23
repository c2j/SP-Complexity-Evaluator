CREATE OR REPLACE PROCEDURE process_employee_updates(
    p_department_id IN NUMBER,
    p_update_date IN DATE
) AS
    v_employee_count NUMBER := 0;
    v_processed_count NUMBER := 0;
    v_error_count NUMBER := 0;
    v_department_valid BOOLEAN;
    
    -- Cursor for employees in the department
    CURSOR c_employees IS
        SELECT employee_id, first_name, last_name, salary, hire_date
        FROM employees
        WHERE department_id = p_department_id
        AND last_update_date < p_update_date;
BEGIN
    -- Log the start of processing
    INSERT INTO process_log(process_name, start_date, status)
    VALUES ('EMPLOYEE_UPDATE', SYSDATE, 'STARTED');
    
    -- Validate department
    v_department_valid := VALIDATE_DEPARTMENT(p_department_id);
    
    IF NOT v_department_valid THEN
        RAISE_APPLICATION_ERROR(-20001, 'Invalid department ID: ' || p_department_id);
    END IF;
    
    -- Process each employee
    FOR emp_rec IN c_employees LOOP
        v_employee_count := v_employee_count + 1;
        
        BEGIN
            -- Get detailed employee information
            DECLARE
                v_employee_details SYS_REFCURSOR;
                v_job_title VARCHAR2(100);
                v_manager_name VARCHAR2(200);
                v_department_name VARCHAR2(100);
            BEGIN
                v_employee_details := GET_EMPLOYEE_DETAILS(emp_rec.employee_id);
                
                -- Process the cursor to get employee details
                FETCH v_employee_details INTO v_job_title, v_manager_name, v_department_name;
                CLOSE v_employee_details;
                
                -- Calculate new salary based on performance and other factors
                DECLARE
                    v_new_salary NUMBER;
                    v_increase_percent NUMBER;
                BEGIN
                    v_new_salary := CALCULATE_SALARY(
                        emp_rec.employee_id, 
                        emp_rec.salary, 
                        emp_rec.hire_date, 
                        p_update_date
                    );
                    
                    -- Calculate increase percentage
                    v_increase_percent := ((v_new_salary - emp_rec.salary) / emp_rec.salary) * 100;
                    
                    -- Update employee record with new salary
                    UPDATE employees
                    SET salary = v_new_salary,
                        last_update_date = SYSDATE
                    WHERE employee_id = emp_rec.employee_id;
                    
                    -- Log the salary change
                    INSERT INTO salary_history(
                        employee_id,
                        old_salary,
                        new_salary,
                        change_percent,
                        change_date,
                        change_reason
                    ) VALUES (
                        emp_rec.employee_id,
                        emp_rec.salary,
                        v_new_salary,
                        v_increase_percent,
                        SYSDATE,
                        'Annual Review'
                    );
                    
                    -- Process the transaction
                    IF PROCESS_TRANSACTION(
                        'SALARY_UPDATE',
                        emp_rec.employee_id,
                        v_new_salary,
                        SYSDATE
                    ) THEN
                        -- Update employee status
                        UPDATE_EMPLOYEE_STATUS(
                            emp_rec.employee_id,
                            'ACTIVE',
                            'Salary updated on ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD')
                        );
                        
                        v_processed_count := v_processed_count + 1;
                    ELSE
                        -- Log error
                        INSERT INTO error_log(
                            error_date,
                            error_type,
                            employee_id,
                            error_message
                        ) VALUES (
                            SYSDATE,
                            'TRANSACTION_FAILED',
                            emp_rec.employee_id,
                            'Failed to process salary update transaction'
                        );
                        
                        v_error_count := v_error_count + 1;
                    END IF;
                END;
            END;
        EXCEPTION
            WHEN OTHERS THEN
                -- Log error
                INSERT INTO error_log(
                    error_date,
                    error_type,
                    employee_id,
                    error_message
                ) VALUES (
                    SYSDATE,
                    'PROCESSING_ERROR',
                    emp_rec.employee_id,
                    SUBSTR(SQLERRM, 1, 200)
                );
                
                v_error_count := v_error_count + 1;
        END;
    END LOOP;
    
    -- Update process log with completion status
    UPDATE process_log
    SET end_date = SYSDATE,
        status = CASE 
                    WHEN v_error_count > 0 THEN 'COMPLETED_WITH_ERRORS'
                    ELSE 'COMPLETED'
                 END,
        records_processed = v_processed_count,
        records_failed = v_error_count
    WHERE process_name = 'EMPLOYEE_UPDATE'
    AND start_date = TRUNC(SYSDATE);
    
    COMMIT;
    
    -- Return summary as output parameters
    DBMS_OUTPUT.PUT_LINE('Employee update process completed.');
    DBMS_OUTPUT.PUT_LINE('Total employees: ' || v_employee_count);
    DBMS_OUTPUT.PUT_LINE('Successfully processed: ' || v_processed_count);
    DBMS_OUTPUT.PUT_LINE('Errors: ' || v_error_count);
EXCEPTION
    WHEN OTHERS THEN
        -- Log fatal error
        INSERT INTO error_log(
            error_date,
            error_type,
            error_message
        ) VALUES (
            SYSDATE,
            'FATAL_ERROR',
            SUBSTR(SQLERRM, 1, 200)
        );
        
        -- Update process log with failure status
        UPDATE process_log
        SET end_date = SYSDATE,
            status = 'FAILED',
            error_message = SUBSTR(SQLERRM, 1, 200)
        WHERE process_name = 'EMPLOYEE_UPDATE'
        AND start_date = TRUNC(SYSDATE);
        
        ROLLBACK;
        RAISE;
END process_employee_updates;
