CREATE OR REPLACE PROCEDURE analyze_department_performance(
    p_year IN NUMBER,
    p_quarter IN NUMBER,
    p_threshold IN NUMBER DEFAULT 0.1
) AS
    v_start_date DATE;
    v_end_date DATE;
    v_department_count NUMBER := 0;
    v_employee_count NUMBER := 0;
    v_total_sales NUMBER := 0;
    v_avg_performance NUMBER := 0;
    v_performance_change NUMBER := 0;
    v_previous_quarter_performance NUMBER := 0;
    
    -- Cursor for departments
    CURSOR c_departments IS
        SELECT department_id, department_name, manager_id
        FROM departments
        WHERE department_id IN (
            SELECT DISTINCT department_id 
            FROM employees 
            WHERE job_id LIKE '%SALES%' OR job_id LIKE '%MK%'
        )
        ORDER BY department_id;
    
    -- Cursor for employees in a department
    CURSOR c_employees(p_dept_id NUMBER) IS
        SELECT employee_id, first_name, last_name, hire_date, salary, commission_pct
        FROM employees
        WHERE department_id = p_dept_id
        ORDER BY employee_id;
    
    -- Cursor for sales by employee
    CURSOR c_sales(p_emp_id NUMBER) IS
        SELECT 
            sale_id, 
            sale_date, 
            customer_id, 
            product_id, 
            quantity, 
            unit_price, 
            (quantity * unit_price) as total_amount,
            commission_amount
        FROM sales
        WHERE employee_id = p_emp_id
        AND sale_date BETWEEN v_start_date AND v_end_date
        ORDER BY sale_date;
    
    -- Record types
    TYPE r_department IS RECORD (
        department_id departments.department_id%TYPE,
        department_name departments.department_name%TYPE,
        manager_id departments.manager_id%TYPE,
        total_sales NUMBER := 0,
        employee_count NUMBER := 0,
        avg_sales_per_employee NUMBER := 0,
        performance_rating VARCHAR2(20)
    );
    
    TYPE r_employee IS RECORD (
        employee_id employees.employee_id%TYPE,
        full_name VARCHAR2(100),
        hire_date employees.hire_date%TYPE,
        salary employees.salary%TYPE,
        commission_pct employees.commission_pct%TYPE,
        total_sales NUMBER := 0,
        total_commission NUMBER := 0,
        sales_count NUMBER := 0,
        performance_rating VARCHAR2(20)
    );
    
    -- Department record
    v_dept r_department;
    
    -- Employee record
    v_emp r_employee;
    
    -- Performance thresholds
    v_high_performance_threshold NUMBER := 1.2;  -- 20% above average
    v_low_performance_threshold NUMBER := 0.8;   -- 20% below average
    
BEGIN
    -- Calculate date range for the specified quarter
    v_start_date := TO_DATE(p_year || '-' || (p_quarter * 3 - 2) || '-01', 'YYYY-MM-DD');
    v_end_date := ADD_MONTHS(v_start_date, 3) - 1;
    
    -- Log the analysis start
    INSERT INTO performance_analysis_log (
        analysis_date, 
        period_start, 
        period_end, 
        analysis_type, 
        initiated_by
    ) VALUES (
        SYSDATE, 
        v_start_date, 
        v_end_date, 
        'QUARTERLY', 
        USER
    );
    
    -- Get previous quarter performance for comparison
    BEGIN
        SELECT AVG(total_amount)
        INTO v_previous_quarter_performance
        FROM sales
        WHERE sale_date BETWEEN ADD_MONTHS(v_start_date, -3) AND ADD_MONTHS(v_end_date, -3);
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            v_previous_quarter_performance := 0;
    END;
    
    -- Process each department
    FOR dept_rec IN c_departments LOOP
        -- Initialize department record
        v_dept.department_id := dept_rec.department_id;
        v_dept.department_name := dept_rec.department_name;
        v_dept.manager_id := dept_rec.manager_id;
        v_dept.total_sales := 0;
        v_dept.employee_count := 0;
        
        -- Process each employee in the department
        FOR emp_rec IN c_employees(v_dept.department_id) LOOP
            -- Initialize employee record
            v_emp.employee_id := emp_rec.employee_id;
            v_emp.full_name := emp_rec.first_name || ' ' || emp_rec.last_name;
            v_emp.hire_date := emp_rec.hire_date;
            v_emp.salary := emp_rec.salary;
            v_emp.commission_pct := NVL(emp_rec.commission_pct, 0);
            v_emp.total_sales := 0;
            v_emp.total_commission := 0;
            v_emp.sales_count := 0;
            
            -- Process each sale by the employee
            FOR sale_rec IN c_sales(v_emp.employee_id) LOOP
                -- Accumulate sales data
                v_emp.total_sales := v_emp.total_sales + sale_rec.total_amount;
                v_emp.total_commission := v_emp.total_commission + NVL(sale_rec.commission_amount, 0);
                v_emp.sales_count := v_emp.sales_count + 1;
                
                -- Log detailed sale analysis if it's a large sale
                IF sale_rec.total_amount > 10000 THEN
                    INSERT INTO large_sale_analysis (
                        sale_id,
                        analysis_date,
                        employee_id,
                        department_id,
                        sale_amount,
                        commission_amount,
                        sale_date
                    ) VALUES (
                        sale_rec.sale_id,
                        SYSDATE,
                        v_emp.employee_id,
                        v_dept.department_id,
                        sale_rec.total_amount,
                        sale_rec.commission_amount,
                        sale_rec.sale_date
                    );
                END IF;
            END LOOP;
            
            -- Update department totals
            v_dept.total_sales := v_dept.total_sales + v_emp.total_sales;
            v_dept.employee_count := v_dept.employee_count + 1;
            
            -- Calculate employee performance rating
            IF v_emp.sales_count > 0 THEN
                -- Record employee performance
                INSERT INTO employee_performance (
                    employee_id,
                    department_id,
                    period_start,
                    period_end,
                    total_sales,
                    sales_count,
                    total_commission,
                    analysis_date
                ) VALUES (
                    v_emp.employee_id,
                    v_dept.department_id,
                    v_start_date,
                    v_end_date,
                    v_emp.total_sales,
                    v_emp.sales_count,
                    v_emp.total_commission,
                    SYSDATE
                );
            END IF;
            
            -- Increment total employee count
            v_employee_count := v_employee_count + 1;
        END LOOP;
        
        -- Calculate department averages
        IF v_dept.employee_count > 0 THEN
            v_dept.avg_sales_per_employee := v_dept.total_sales / v_dept.employee_count;
        ELSE
            v_dept.avg_sales_per_employee := 0;
        END IF;
        
        -- Record department performance
        INSERT INTO department_performance (
            department_id,
            period_start,
            period_end,
            total_sales,
            employee_count,
            avg_sales_per_employee,
            analysis_date
        ) VALUES (
            v_dept.department_id,
            v_start_date,
            v_end_date,
            v_dept.total_sales,
            v_dept.employee_count,
            v_dept.avg_sales_per_employee,
            SYSDATE
        );
        
        -- Accumulate totals for overall analysis
        v_total_sales := v_total_sales + v_dept.total_sales;
        v_department_count := v_department_count + 1;
    END LOOP;
    
    -- Calculate overall performance metrics
    IF v_employee_count > 0 THEN
        v_avg_performance := v_total_sales / v_employee_count;
    END IF;
    
    -- Calculate performance change from previous quarter
    IF v_previous_quarter_performance > 0 THEN
        v_performance_change := (v_avg_performance - v_previous_quarter_performance) / v_previous_quarter_performance;
    END IF;
    
    -- Record overall performance
    INSERT INTO overall_performance (
        period_start,
        period_end,
        total_sales,
        department_count,
        employee_count,
        avg_performance,
        performance_change,
        analysis_date
    ) VALUES (
        v_start_date,
        v_end_date,
        v_total_sales,
        v_department_count,
        v_employee_count,
        v_avg_performance,
        v_performance_change,
        SYSDATE
    );
    
    -- Generate performance alerts if change exceeds threshold
    IF ABS(v_performance_change) > p_threshold THEN
        INSERT INTO performance_alerts (
            alert_date,
            period_start,
            period_end,
            alert_type,
            alert_message,
            performance_change
        ) VALUES (
            SYSDATE,
            v_start_date,
            v_end_date,
            CASE 
                WHEN v_performance_change > 0 THEN 'PERFORMANCE_IMPROVEMENT'
                ELSE 'PERFORMANCE_DECLINE'
            END,
            'Overall performance changed by ' || 
            TO_CHAR(ABS(v_performance_change) * 100, '999.99') || 
            '% compared to previous quarter',
            v_performance_change
        );
    END IF;
    
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
            'ANALYZE_DEPARTMENT_PERFORMANCE',
            SQLCODE,
            SQLERRM,
            USER
        );
        
        -- Rollback any changes
        ROLLBACK;
        
        -- Re-raise the exception
        RAISE;
END;
