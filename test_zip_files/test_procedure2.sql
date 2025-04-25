CREATE OR REPLACE PROCEDURE test_procedure2 AS
BEGIN
  -- 调用高权重存储过程
  CALL important_procedure3();
  
  -- 使用高权重表
  SELECT * FROM important_table2
  WHERE status = 'ACTIVE';
  
  -- 使用自定义函数
  v_formatted_date := format_date(SYSDATE);
  
  -- 子查询
  SELECT e.employee_id, e.employee_name
  FROM employees e
  WHERE e.department_id IN (
    SELECT d.department_id
    FROM departments d
    WHERE d.location_id = 1700
  );
END;
/
