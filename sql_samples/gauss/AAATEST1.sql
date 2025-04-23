CREATE OR REPLACE PROCEDURE BIGFUND.AAATEST1 is


BEGIN
  -----begin 
  v_i_fcode := 'ACSS7002I';
  v_o_fcode := 'ACSS7002O';
  v_date    := to_char(SYSDATE, 'yyyymmdd');

  --v_tmp := '<?xml version="1.0" encoding="GB2312"?>
--<ICBCACSS><FCODE>7002</FCODE><TM>20131107142614000653</TM><CUSTOMERID>lq11020.c.0200</CUSTOMERID><QUESTIONID></QUESTIONID><KEYWORD></KEYWORD>
--<GROUPID>020099990042921</GROUPID>
--<CERTTYPECODE>c</CERTTYPECODE>
--<QRYBEGINPOS>1</QRYBEGINPOS>
--<QRYNUM>5</QRYNUM>
--</ICBCACSS>';
 -----end 
 
 --v_tmp :='<?xml version="1.0" encoding="GB2312"?>
--<ICBCACSS><FCODE>7002</FCODE><TM>20141201095831000446</TM>
--<CUSTOMERID>ZCTG0000021005.c.3602</CUSTOMERID>
--<QUESTIONID></QUESTIONID>
--<KEYWORD></KEYWORD>
--<GROUPID>360290001695786</GROUPID>
--<CERTTYPECODE>c</CERTTYPECODE>
--<QRYBEGINPOS>1</QRYBEGINPOS>
--<QRYNUM>100</QRYNUM>
--</ICBCACSS>';

-- v_tmp :='<ICBCACSS>
-- <FCODE>7002</FCODE>
-- <TM>20160614091908000944</TM>
-- <CUSTOMERID>ZCTG0000021005.c.3602</CUSTOMERID>
 --<QUESTIONID></QUESTIONID>
 --<KEYWORD></KEYWORD>
-- <GROUPID>360290001695786</GROUPID>
 --<CERTTYPECODE>c</CERTTYPECODE>
 --<QRYBEGINPOS>1</QRYBEGINPOS>
-- <QRYNUM>20</QRYNUM>
-- </ICBCACSS>';
 
  v_tmp :='<?xml version="1.0" encoding="GB2312"?>
<ICBCACSS>
<FCODE>7002</FCODE>
<TM>20141223140411000452</TM>
<CUSTOMERID>ZCTG0000021005.c.3602</CUSTOMERID>
<QUESTIONID></QUESTIONID>
<KEYWORD></KEYWORD>
<GROUPID>360290001695786</GROUPID>
<CERTTYPECODE>c</CERTTYPECODE>
<QRYBEGINPOS>41</QRYBEGINPOS>
<QRYNUM>20</QRYNUM>
</ICBCACSS>
';
 
  v_xml := xmltype(v_tmp);

  pkg_http_mng.prc_add_recv_inst('ACSS',
                                 v_xml.getclobval(),
                                 v_i_fcode,
                                 v_seq,
                                 p_o_code,
                                 p_o_msg);

  pkg_http_mng.prc_split_http_xml_from_eb(v_seq,
                                          v_date,
                                          v_i_fcode,
                                          v_inst_num,
                                          p_o_code,
                                          p_o_msg);

  pkg_http_mng.prc_send_http_xml_to_eb(v_seq,
                                       v_date,
                                       v_o_fcode,
                                       v_clob,
                                       p_o_code,
                                       p_o_msg);

  pkg_http_mng.prc_update_http_send_xml(v_seq, v_clob, p_o_code, p_o_msg);

  

a = 'SELECT T1.* FROM' || '(SELECT T.*, ROWNUM RNUM FROM P_SQL2  T WHERE ROWNUM < TO_NUMBER(' || IN_QRYBEGINPOS ||
           ') + TO_NUMBER (' || IN_QRYNUM ||
           ')) T1 WHERE T1.RNUM >= TO_NUMBER (' || IN_QRYBEGINPOS || ')';

  COMMIT;
END;
