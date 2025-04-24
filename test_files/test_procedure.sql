CREATE OR REPLACE PROCEDURE test_procedure
AS
  v_count NUMBER;
BEGIN
  -- Count employees
  SELECT COUNT(*) INTO v_count FROM employees;
  
  -- Loop through departments
  FOR dept_rec IN (SELECT department_id, department_name FROM departments) LOOP
    -- Nested loop
    FOR emp_rec IN (SELECT employee_id, salary FROM employees WHERE department_id = dept_rec.department_id) LOOP
      -- Update salary with 10% increase
      UPDATE employees 
      SET salary = salary * 1.1
      WHERE employee_id = emp_rec.employee_id;
    END LOOP;
  END LOOP;
  
  -- Call another procedure
  calculate_department_stats(10);
  
  -- Call high-weight procedure
  process_payroll();
END;
/
