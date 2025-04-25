CREATE OR REPLACE PROCEDURE test_dml_proc IS
BEGIN
  INSERT INTO test_table VALUES (1);
  UPDATE test_table SET col = 2;
  DELETE FROM test_table WHERE col = 2;
  MERGE INTO target_table t
  USING source_table s
  ON (t.id = s.id)
  WHEN MATCHED THEN
    UPDATE SET t.value = s.value
  WHEN NOT MATCHED THEN
    INSERT (id, value) VALUES (s.id, s.value);
END;
/
