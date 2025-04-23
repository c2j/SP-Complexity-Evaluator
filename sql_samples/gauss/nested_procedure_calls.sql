CREATE OR REPLACE PROCEDURE process_monthly_payroll(
    p_month IN NUMBER,
    p_year IN NUMBER,
    p_department_id IN NUMBER DEFAULT NULL,
    p_result OUT NUMBER
) AS
    -- Constants
    c_tax_rate CONSTANT NUMBER := 0.25;
    c_social_security_rate CONSTANT NUMBER := 0.062;
    c_medicare_rate CONSTANT NUMBER := 0.0145;
    c_retirement_rate CONSTANT NUMBER := 0.05;
    
    -- Variables
    v_start_date DATE;
    v_end_date DATE;
    v_employee_count NUMBER := 0;
    v_total_salary NUMBER := 0;
    v_total_tax NUMBER := 0;
    v_total_deductions NUMBER := 0;
    v_total_net_pay NUMBER := 0;
    v_payroll_id NUMBER;
    v_department_name VARCHAR2(100);
    
    -- Cursor for departments
    CURSOR c_departments IS
        SELECT department_id, department_name
        FROM departments
        WHERE department_id = NVL(p_department_id, department_id)
        ORDER BY department_id;
    
    -- Nested procedure to process department payroll
    PROCEDURE process_department_payroll(
        p_dept_id IN NUMBER,
        p_dept_name IN VARCHAR2,
        p_start_date IN DATE,
        p_end_date IN DATE,
        p_dept_result OUT NUMBER
    ) AS
        v_dept_employee_count NUMBER := 0;
        v_dept_total_salary NUMBER := 0;
        v_dept_total_tax NUMBER := 0;
        v_dept_total_deductions NUMBER := 0;
        v_dept_total_net_pay NUMBER := 0;
        v_dept_payroll_id NUMBER;
        
        -- Cursor for employees in department
        CURSOR c_employees IS
            SELECT employee_id, first_name, last_name, salary, commission_pct, job_id
            FROM employees
            WHERE department_id = p_dept_id
            ORDER BY employee_id;
        
        -- Nested procedure to process employee payroll
        PROCEDURE process_employee_payroll(
            p_emp_id IN NUMBER,
            p_emp_name IN VARCHAR2,
            p_salary IN NUMBER,
            p_commission_pct IN NUMBER,
            p_job_id IN VARCHAR2,
            p_dept_payroll_id IN NUMBER,
            p_emp_result OUT NUMBER
        ) AS
            v_gross_pay NUMBER;
            v_commission NUMBER := 0;
            v_tax NUMBER;
            v_social_security NUMBER;
            v_medicare NUMBER;
            v_retirement NUMBER;
            v_other_deductions NUMBER := 0;
            v_net_pay NUMBER;
            v_payslip_id NUMBER;
            
            -- Nested procedure to calculate commissions
            PROCEDURE calculate_commission(
                p_emp_id IN NUMBER,
                p_commission_pct IN NUMBER,
                p_month IN NUMBER,
                p_year IN NUMBER,
                p_commission OUT NUMBER
            ) AS
                v_sales_total NUMBER := 0;
            BEGIN
                -- Get total sales for the employee in the period
                BEGIN
                    SELECT NVL(SUM(sale_amount), 0)
                    INTO v_sales_total
                    FROM sales
                    WHERE employee_id = p_emp_id
                    AND EXTRACT(MONTH FROM sale_date) = p_month
                    AND EXTRACT(YEAR FROM sale_date) = p_year;
                EXCEPTION
                    WHEN NO_DATA_FOUND THEN
                        v_sales_total := 0;
                END;
                
                -- Calculate commission
                IF p_commission_pct IS NOT NULL AND p_commission_pct > 0 THEN
                    p_commission := v_sales_total * p_commission_pct;
                ELSE
                    p_commission := 0;
                END IF;
                
                -- Log commission calculation
                INSERT INTO commission_log (
                    employee_id,
                    calculation_date,
                    sales_total,
                    commission_pct,
                    commission_amount
                ) VALUES (
                    p_emp_id,
                    SYSDATE,
                    v_sales_total,
                    NVL(p_commission_pct, 0),
                    p_commission
                );
            END calculate_commission;
            
            -- Nested procedure to calculate deductions
            PROCEDURE calculate_deductions(
                p_emp_id IN NUMBER,
                p_job_id IN VARCHAR2,
                p_gross_pay IN NUMBER,
                p_deductions OUT NUMBER
            ) AS
                v_health_insurance NUMBER := 0;
                v_life_insurance NUMBER := 0;
                v_parking_fee NUMBER := 0;
                v_union_dues NUMBER := 0;
                v_garnishments NUMBER := 0;
            BEGIN
                -- Get employee benefits and deductions
                BEGIN
                    SELECT 
                        NVL(health_insurance, 0),
                        NVL(life_insurance, 0),
                        NVL(parking_fee, 0),
                        NVL(union_dues, 0),
                        NVL(garnishments, 0)
                    INTO 
                        v_health_insurance,
                        v_life_insurance,
                        v_parking_fee,
                        v_union_dues,
                        v_garnishments
                    FROM employee_benefits
                    WHERE employee_id = p_emp_id;
                EXCEPTION
                    WHEN NO_DATA_FOUND THEN
                        v_health_insurance := 0;
                        v_life_insurance := 0;
                        v_parking_fee := 0;
                        v_union_dues := 0;
                        v_garnishments := 0;
                END;
                
                -- Calculate total deductions
                p_deductions := v_health_insurance + v_life_insurance + 
                               v_parking_fee + v_union_dues + v_garnishments;
                
                -- Log deduction calculation
                INSERT INTO deduction_log (
                    employee_id,
                    calculation_date,
                    health_insurance,
                    life_insurance,
                    parking_fee,
                    union_dues,
                    garnishments,
                    total_deductions
                ) VALUES (
                    p_emp_id,
                    SYSDATE,
                    v_health_insurance,
                    v_life_insurance,
                    v_parking_fee,
                    v_union_dues,
                    v_garnishments,
                    p_deductions
                );
            END calculate_deductions;
            
        BEGIN
            -- Calculate commission
            calculate_commission(p_emp_id, p_commission_pct, p_month, p_year, v_commission);
            
            -- Calculate gross pay
            v_gross_pay := p_salary + v_commission;
            
            -- Calculate tax
            v_tax := v_gross_pay * c_tax_rate;
            
            -- Calculate social security
            v_social_security := v_gross_pay * c_social_security_rate;
            
            -- Calculate medicare
            v_medicare := v_gross_pay * c_medicare_rate;
            
            -- Calculate retirement contribution
            v_retirement := v_gross_pay * c_retirement_rate;
            
            -- Calculate other deductions
            calculate_deductions(p_emp_id, p_job_id, v_gross_pay, v_other_deductions);
            
            -- Calculate net pay
            v_net_pay := v_gross_pay - v_tax - v_social_security - v_medicare - v_retirement - v_other_deductions;
            
            -- Generate payslip ID
            SELECT payslip_seq.NEXTVAL INTO v_payslip_id FROM DUAL;
            
            -- Create payslip record
            INSERT INTO payslips (
                payslip_id,
                payroll_id,
                employee_id,
                employee_name,
                pay_period_start,
                pay_period_end,
                salary,
                commission,
                gross_pay,
                tax,
                social_security,
                medicare,
                retirement,
                other_deductions,
                net_pay,
                creation_date
            ) VALUES (
                v_payslip_id,
                p_dept_payroll_id,
                p_emp_id,
                p_emp_name,
                p_start_date,
                p_end_date,
                p_salary,
                v_commission,
                v_gross_pay,
                v_tax,
                v_social_security,
                v_medicare,
                v_retirement,
                v_other_deductions,
                v_net_pay,
                SYSDATE
            );
            
            -- Update employee payment history
            INSERT INTO payment_history (
                employee_id,
                payment_date,
                payment_type,
                payment_amount,
                payment_reference
            ) VALUES (
                p_emp_id,
                SYSDATE,
                'SALARY',
                v_net_pay,
                'PAYSLIP-' || v_payslip_id
            );
            
            -- Update department totals
            v_dept_total_salary := v_dept_total_salary + v_gross_pay;
            v_dept_total_tax := v_dept_total_tax + v_tax;
            v_dept_total_deductions := v_dept_total_deductions + v_social_security + v_medicare + v_retirement + v_other_deductions;
            v_dept_total_net_pay := v_dept_total_net_pay + v_net_pay;
            v_dept_employee_count := v_dept_employee_count + 1;
            
            -- Set result to payslip ID
            p_emp_result := v_payslip_id;
            
        EXCEPTION
            WHEN OTHERS THEN
                -- Log the error
                INSERT INTO error_log (
                    error_date,
                    procedure_name,
                    error_code,
                    error_message,
                    employee_id
                ) VALUES (
                    SYSDATE,
                    'PROCESS_EMPLOYEE_PAYROLL',
                    SQLCODE,
                    SQLERRM,
                    p_emp_id
                );
                
                -- Set error result
                p_emp_result := -1;
        END process_employee_payroll;
        
    BEGIN
        -- Generate department payroll ID
        SELECT dept_payroll_seq.NEXTVAL INTO v_dept_payroll_id FROM DUAL;
        
        -- Create department payroll record
        INSERT INTO department_payrolls (
            payroll_id,
            department_id,
            department_name,
            pay_period_start,
            pay_period_end,
            creation_date,
            status
        ) VALUES (
            v_dept_payroll_id,
            p_dept_id,
            p_dept_name,
            p_start_date,
            p_end_date,
            SYSDATE,
            'PROCESSING'
        );
        
        -- Process each employee in the department
        FOR emp_rec IN c_employees LOOP
            DECLARE
                v_emp_result NUMBER;
            BEGIN
                process_employee_payroll(
                    emp_rec.employee_id,
                    emp_rec.first_name || ' ' || emp_rec.last_name,
                    emp_rec.salary,
                    emp_rec.commission_pct,
                    emp_rec.job_id,
                    v_dept_payroll_id,
                    v_emp_result
                );
            EXCEPTION
                WHEN OTHERS THEN
                    -- Log the error but continue with next employee
                    INSERT INTO error_log (
                        error_date,
                        procedure_name,
                        error_code,
                        error_message,
                        employee_id
                    ) VALUES (
                        SYSDATE,
                        'PROCESS_DEPARTMENT_PAYROLL',
                        SQLCODE,
                        SQLERRM,
                        emp_rec.employee_id
                    );
            END;
        END LOOP;
        
        -- Update department payroll record with totals
        UPDATE department_payrolls
        SET 
            employee_count = v_dept_employee_count,
            total_salary = v_dept_total_salary,
            total_tax = v_dept_total_tax,
            total_deductions = v_dept_total_deductions,
            total_net_pay = v_dept_total_net_pay,
            status = 'COMPLETED'
        WHERE payroll_id = v_dept_payroll_id;
        
        -- Update company totals
        v_employee_count := v_employee_count + v_dept_employee_count;
        v_total_salary := v_total_salary + v_dept_total_salary;
        v_total_tax := v_total_tax + v_dept_total_tax;
        v_total_deductions := v_total_deductions + v_dept_total_deductions;
        v_total_net_pay := v_total_net_pay + v_dept_total_net_pay;
        
        -- Set result to department payroll ID
        p_dept_result := v_dept_payroll_id;
        
    EXCEPTION
        WHEN OTHERS THEN
            -- Log the error
            INSERT INTO error_log (
                error_date,
                procedure_name,
                error_code,
                error_message,
                department_id
            ) VALUES (
                SYSDATE,
                'PROCESS_DEPARTMENT_PAYROLL',
                SQLCODE,
                SQLERRM,
                p_dept_id
            );
            
            -- Update department payroll status to ERROR
            UPDATE department_payrolls
            SET status = 'ERROR'
            WHERE payroll_id = v_dept_payroll_id;
            
            -- Set error result
            p_dept_result := -1;
    END process_department_payroll;
    
BEGIN
    -- Calculate pay period dates
    v_start_date := TO_DATE(p_year || '-' || p_month || '-01', 'YYYY-MM-DD');
    v_end_date := LAST_DAY(v_start_date);
    
    -- Generate payroll ID
    SELECT payroll_seq.NEXTVAL INTO v_payroll_id FROM DUAL;
    
    -- Create company payroll record
    INSERT INTO company_payrolls (
        payroll_id,
        pay_period_start,
        pay_period_end,
        creation_date,
        status,
        created_by
    ) VALUES (
        v_payroll_id,
        v_start_date,
        v_end_date,
        SYSDATE,
        'PROCESSING',
        USER
    );
    
    -- Process each department
    FOR dept_rec IN c_departments LOOP
        DECLARE
            v_dept_result NUMBER;
        BEGIN
            process_department_payroll(
                dept_rec.department_id,
                dept_rec.department_name,
                v_start_date,
                v_end_date,
                v_dept_result
            );
        EXCEPTION
            WHEN OTHERS THEN
                -- Log the error but continue with next department
                INSERT INTO error_log (
                    error_date,
                    procedure_name,
                    error_code,
                    error_message,
                    department_id
                ) VALUES (
                    SYSDATE,
                    'PROCESS_MONTHLY_PAYROLL',
                    SQLCODE,
                    SQLERRM,
                    dept_rec.department_id
                );
        END;
    END LOOP;
    
    -- Update company payroll record with totals
    UPDATE company_payrolls
    SET 
        employee_count = v_employee_count,
        total_salary = v_total_salary,
        total_tax = v_total_tax,
        total_deductions = v_total_deductions,
        total_net_pay = v_total_net_pay,
        completion_date = SYSDATE,
        status = 'COMPLETED'
    WHERE payroll_id = v_payroll_id;
    
    -- Create payroll summary report
    INSERT INTO payroll_reports (
        report_id,
        payroll_id,
        report_date,
        report_type,
        employee_count,
        total_salary,
        total_tax,
        total_deductions,
        total_net_pay,
        report_period
    ) VALUES (
        report_seq.NEXTVAL,
        v_payroll_id,
        SYSDATE,
        'MONTHLY',
        v_employee_count,
        v_total_salary,
        v_total_tax,
        v_total_deductions,
        v_total_net_pay,
        TO_CHAR(v_start_date, 'YYYY-MM')
    );
    
    -- Set result to payroll ID
    p_result := v_payroll_id;
    
    -- Commit all changes
    COMMIT;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Log the error
        INSERT INTO error_log (
            error_date,
            procedure_name,
            error_code,
            error_message,
            user_id
        ) VALUES (
            SYSDATE,
            'PROCESS_MONTHLY_PAYROLL',
            SQLCODE,
            SQLERRM,
            USER
        );
        
        -- Update company payroll status to ERROR
        UPDATE company_payrolls
        SET 
            status = 'ERROR',
            completion_date = SYSDATE
        WHERE payroll_id = v_payroll_id;
        
        -- Rollback all changes
        ROLLBACK;
        
        -- Set error result
        p_result := -1;
END;
