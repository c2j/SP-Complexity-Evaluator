CREATE OR REPLACE PROCEDURE process_department_data(
    p_department_id IN NUMBER,
    p_start_date IN DATE,
    p_end_date IN DATE
) AS
    v_department_name VARCHAR2(100);
    v_manager_id NUMBER;
    v_location_id NUMBER;
    v_employee_count NUMBER := 0;
    v_total_salary NUMBER := 0;
    v_avg_salary NUMBER := 0;
    v_department_valid BOOLEAN;
    
    -- Cursor for employees in the department
    CURSOR c_employees IS
        SELECT employee_id, first_name, last_name, salary, hire_date
        FROM employees
        WHERE department_id = p_department_id
        AND hire_date BETWEEN p_start_date AND p_end_date;
BEGIN
    -- Log the start of processing
    INSERT INTO process_log(process_name, start_date, status)
    VALUES ('DEPARTMENT_DATA', SYSDATE, 'STARTED');
    
    -- First validation of department
    v_department_valid := VALIDATE_DEPARTMENT(p_department_id);
    
    IF NOT v_department_valid THEN
        RAISE_APPLICATION_ERROR(-20001, 'Invalid department ID: ' || p_department_id);
    END IF;
    
    -- Get department details
    SELECT department_name, manager_id, location_id
    INTO v_department_name, v_manager_id, v_location_id
    FROM departments
    WHERE department_id = p_department_id;
    
    -- Second validation with additional parameters
    v_department_valid := VALIDATE_DEPARTMENT(p_department_id, v_manager_id);
    
    IF NOT v_department_valid THEN
        RAISE_APPLICATION_ERROR(-20002, 'Department has invalid manager: ' || v_manager_id);
    END IF;
    
    -- Process manager data
    IF v_manager_id IS NOT NULL THEN
        DECLARE
            v_manager_details SYS_REFCURSOR;
            v_manager_name VARCHAR2(200);
            v_manager_title VARCHAR2(100);
            v_manager_salary NUMBER;
        BEGIN
            -- First call to get manager details
            v_manager_details := GET_EMPLOYEE_DETAILS(v_manager_id);
            FETCH v_manager_details INTO v_manager_name, v_manager_title, v_manager_salary;
            CLOSE v_manager_details;
            
            -- Log manager information
            INSERT INTO manager_log(
                department_id,
                manager_id,
                manager_name,
                process_date
            ) VALUES (
                p_department_id,
                v_manager_id,
                v_manager_name,
                SYSDATE
            );
            
            -- Calculate manager's new salary
            DECLARE
                v_new_salary NUMBER;
            BEGIN
                -- First salary calculation for manager
                v_new_salary := CALCULATE_SALARY(v_manager_id, v_manager_salary, p_start_date, p_end_date);
                
                -- Process the transaction for manager
                IF PROCESS_TRANSACTION('MANAGER_SALARY', v_manager_id, v_new_salary, SYSDATE) THEN
                    -- Update manager status
                    UPDATE_EMPLOYEE_STATUS(v_manager_id, 'ACTIVE', 'Manager salary updated');
                END IF;
            END;
        END;
    END IF;
    
    -- Process each employee
    FOR emp_rec IN c_employees LOOP
        v_employee_count := v_employee_count + 1;
        v_total_salary := v_total_salary + NVL(emp_rec.salary, 0);
        
        -- Skip processing for the manager (already processed)
        IF emp_rec.employee_id <> v_manager_id THEN
            BEGIN
                -- Get detailed employee information (second call to this function)
                DECLARE
                    v_employee_details SYS_REFCURSOR;
                    v_job_title VARCHAR2(100);
                    v_manager_name VARCHAR2(200);
                    v_department_name VARCHAR2(100);
                BEGIN
                    -- Second call to get employee details
                    v_employee_details := GET_EMPLOYEE_DETAILS(emp_rec.employee_id);
                    FETCH v_employee_details INTO v_job_title, v_manager_name, v_department_name;
                    CLOSE v_employee_details;
                    
                    -- Third validation for employee's department assignment
                    v_department_valid := VALIDATE_DEPARTMENT(p_department_id, v_manager_id, emp_rec.employee_id);
                    
                    IF NOT v_department_valid THEN
                        -- Log warning but continue processing
                        INSERT INTO warning_log(
                            warning_date,
                            warning_type,
                            employee_id,
                            warning_message
                        ) VALUES (
                            SYSDATE,
                            'DEPARTMENT_ASSIGNMENT',
                            emp_rec.employee_id,
                            'Employee may be incorrectly assigned to department'
                        );
                    END IF;
                    
                    -- Calculate new salary based on performance (second call to this function)
                    DECLARE
                        v_new_salary NUMBER;
                        v_increase_percent NUMBER;
                    BEGIN
                        -- Second salary calculation for regular employee
                        v_new_salary := CALCULATE_SALARY(
                            emp_rec.employee_id, 
                            emp_rec.salary, 
                            emp_rec.hire_date, 
                            p_end_date
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
                            'Department Review'
                        );
                        
                        -- Second call to process transaction
                        IF PROCESS_TRANSACTION(
                            'SALARY_UPDATE',
                            emp_rec.employee_id,
                            v_new_salary,
                            SYSDATE
                        ) THEN
                            -- Second call to update employee status
                            UPDATE_EMPLOYEE_STATUS(
                                emp_rec.employee_id,
                                'ACTIVE',
                                'Salary updated on ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD')
                            );
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
            END;
        END IF;
    END LOOP;
    
    -- Calculate average salary
    IF v_employee_count > 0 THEN
        v_avg_salary := v_total_salary / v_employee_count;
    END IF;
    
    -- Update department statistics
    UPDATE department_stats
    SET avg_salary = v_avg_salary,
        employee_count = v_employee_count,
        last_update_date = SYSDATE
    WHERE department_id = p_department_id;
    
    -- If no record exists, insert one
    IF SQL%ROWCOUNT = 0 THEN
        INSERT INTO department_stats(
            department_id,
            department_name,
            avg_salary,
            employee_count,
            last_update_date
        ) VALUES (
            p_department_id,
            v_department_name,
            v_avg_salary,
            v_employee_count,
            SYSDATE
        );
    END IF;
    
    -- Final validation of department after processing
    v_department_valid := VALIDATE_DEPARTMENT(p_department_id, v_manager_id, NULL, SYSDATE);
    
    -- Update process log with completion status
    UPDATE process_log
    SET end_date = SYSDATE,
        status = 'COMPLETED',
        records_processed = v_employee_count
    WHERE process_name = 'DEPARTMENT_DATA'
    AND start_date = TRUNC(SYSDATE);
    
    COMMIT;
    
    -- Return summary as output
    DBMS_OUTPUT.PUT_LINE('Department data processing completed.');
    DBMS_OUTPUT.PUT_LINE('Department: ' || v_department_name);
    DBMS_OUTPUT.PUT_LINE('Total employees: ' || v_employee_count);
    DBMS_OUTPUT.PUT_LINE('Average salary: ' || v_avg_salary);
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
        WHERE process_name = 'DEPARTMENT_DATA'
        AND start_date = TRUNC(SYSDATE);
        
        ROLLBACK;
        RAISE;
END process_department_data;
