CREATE OR REPLACE PROCEDURE delete_test2
AS
BEGIN
  DELETE mid_yjqs_detail
  WHERE scdm = 'test'
    AND bcrq = 'test';
  COMMIT;
END;
