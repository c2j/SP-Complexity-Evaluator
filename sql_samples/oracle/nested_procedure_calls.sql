CREATE OR REPLACE PROCEDURE manage_employee_lifecycle(
    p_employee_id IN NUMBER,
    p_action IN VARCHAR2
) AS
    v_department_id NUMBER;
    v_manager_id NUMBER;
    v_salary NUMBER;
    v_hire_date DATE;
    v_status VARCHAR2(20);
    v_result BOOLEAN;
BEGIN
    -- Log the start of processing
    INSERT INTO process_log(process_name, start_date, status)
    VALUES ('EMPLOYEE_LIFECYCLE', SYSDATE, 'STARTED');
    
    -- Get employee details
    SELECT department_id, manager_id, salary, hire_date, status
    INTO v_department_id, v_manager_id, v_salary, v_hire_date, v_status
    FROM employees
    WHERE employee_id = p_employee_id;
    
    -- Process based on action
    CASE p_action
        WHEN 'HIRE' THEN
            -- Call onboarding procedure
            EMPLOYEE_ONBOARDING_PROCEDURE(p_employee_id, v_department_id);
            
            -- Set up payroll
            SETUP_EMPLOYEE_PAYROLL(p_employee_id, v_salary);
            
            -- Assign initial tasks
            ASSIGN_INITIAL_TASKS(p_employee_id, v_department_id);
            
            -- Send welcome email
            SEND_EMPLOYEE_EMAIL(p_employee_id, 'WELCOME');
            
        WHEN 'PROMOTE' THEN
            -- Evaluate performance first
            v_result := EVALUATE_EMPLOYEE_PERFORMANCE(p_employee_id);
            
            IF v_result THEN
                -- Process promotion
                PROCESS_EMPLOYEE_PROMOTION(p_employee_id);
                
                -- Update salary
                UPDATE_EMPLOYEE_SALARY(p_employee_id, v_salary * 1.1);
                
                -- Send congratulation email
                SEND_EMPLOYEE_EMAIL(p_employee_id, 'PROMOTION');
            ELSE
                -- Log rejection
                INSERT INTO employee_action_log(
                    employee_id,
                    action,
                    result,
                    action_date
                ) VALUES (
                    p_employee_id,
                    'PROMOTION_ATTEMPT',
                    'REJECTED',
                    SYSDATE
                );
            END IF;
            
        WHEN 'TRANSFER' THEN
            -- Process department transfer
            TRANSFER_EMPLOYEE_DEPARTMENT(p_employee_id, v_department_id, p_new_department_id => 20);
            
            -- Update reporting structure
            UPDATE_REPORTING_STRUCTURE(p_employee_id);
            
            -- Reassign tasks
            REASSIGN_EMPLOYEE_TASKS(p_employee_id);
            
            -- Send transfer notification
            SEND_EMPLOYEE_EMAIL(p_employee_id, 'TRANSFER');
            
            -- Notify managers
            NOTIFY_DEPARTMENT_MANAGERS(v_department_id, 20, p_employee_id);
            
        WHEN 'TERMINATE' THEN
            -- Process termination
            PROCESS_EMPLOYEE_TERMINATION(p_employee_id);
            
            -- Handle benefits termination
            TERMINATE_EMPLOYEE_BENEFITS(p_employee_id);
            
            -- Process final paycheck
            PROCESS_FINAL_PAYCHECK(p_employee_id);
            
            -- Revoke system access
            REVOKE_SYSTEM_ACCESS(p_employee_id);
            
            -- Exit interview
            SCHEDULE_EXIT_INTERVIEW(p_employee_id);
            
            -- Send farewell email
            SEND_EMPLOYEE_EMAIL(p_employee_id, 'FAREWELL');
            
        ELSE
            -- Invalid action
            RAISE_APPLICATION_ERROR(-20001, 'Invalid action: ' || p_action);
    END CASE;
    
    -- Call audit procedure for all actions
    AUDIT_EMPLOYEE_ACTION(p_employee_id, p_action, SYSDATE);
    
    -- Update employee history
    UPDATE_EMPLOYEE_HISTORY(
        p_employee_id, 
        p_action, 
        SYSDATE, 
        USER, 
        'Completed successfully'
    );
    
    -- Update process log with completion status
    UPDATE process_log
    SET end_date = SYSDATE,
        status = 'COMPLETED'
    WHERE process_name = 'EMPLOYEE_LIFECYCLE'
    AND start_date = TRUNC(SYSDATE);
    
    COMMIT;
    
    -- Send notification to HR
    SEND_HR_NOTIFICATION(p_employee_id, p_action);
    
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
            'EMPLOYEE_LIFECYCLE_ERROR',
            p_employee_id,
            SUBSTR(SQLERRM, 1, 200)
        );
        
        -- Update process log with failure status
        UPDATE process_log
        SET end_date = SYSDATE,
            status = 'FAILED',
            error_message = SUBSTR(SQLERRM, 1, 200)
        WHERE process_name = 'EMPLOYEE_LIFECYCLE'
        AND start_date = TRUNC(SYSDATE);
        
        -- Send error notification
        SEND_ERROR_NOTIFICATION('EMPLOYEE_LIFECYCLE', p_employee_id, SQLERRM);
        
        ROLLBACK;
        RAISE;
END manage_employee_lifecycle;
