CREATE OR REPLACE PROCEDURE test_procedure1 AS
BEGIN
  -- 调用高权重存储过程
  CALL important_procedure1();
  CALL important_procedure2();
  
  -- 使用高权重表
  INSERT INTO important_table1 (id, name)
  SELECT id, name FROM source_table;
  
  -- 使用自定义函数
  v_result := calculate_value(10, 20);
  
  -- 循环
  FOR i IN 1..10 LOOP
    -- 嵌套循环
    FOR j IN 1..5 LOOP
      INSERT INTO result_table (id, value)
      VALUES (i, j);
    END LOOP;
  END LOOP;
END;
/
