CREATE OR REPLACE PROCEDURE BIGFUND.AAATEST2 is
begin meta.p_mt_std_cover_log.r_cover_log('3795','2','1');
  DECLARE
  -- Local variables here
  i         NUMBER(38);
  v_xml     XMLTYPE;
  v_map     map_object := map_object();
  v_numlist numtabletype := numtabletype();
  v_tmp     VARCHAR2(4000);
  v_seq     VARCHAR2(100);

  v_i_fcode  VARCHAR2(10);
  v_o_fcode  VARCHAR2(10);
  v_date     VARCHAR2(8);
  v_inst_num NUMBER(30);

  v_clob   CLOB := empty_clob();
  p_o_code VARCHAR2(100);
  p_o_msg  VARCHAR2(1000);
BEGIN
  -----begin 根据自己的接口改对应参数
  v_i_fcode := 'ACSS7003I';
  v_o_fcode := 'ACSS7003O';
  v_date    := to_char(SYSDATE, 'yyyymmdd');

  --v_tmp := '<?xml version="1.0" encoding="GB2312"?>
--<ICBCACSS>
--<FCODE>7003</FCODE><TM>20130929105653000008</TM>
--<CUSTOMERID>lq11020.c.0200</CUSTOMERID>
--<QUESTIONID>1309290003</QUESTIONID>
--<ANSWERID></ANSWERID>
--<GROUPID>020099990042921</GROUPID>
--<CERTTYPECODE>c</CERTTYPECODE>
--<QRYBEGINPOS>11</QRYBEGINPOS>
--<QRYNUM>20</QRYNUM></ICBCACSS>';
 -----end 
 v_tmp := '<ICBCACSS><FCODE>7003</FCODE>
 <TM>20131107191751000957</TM>
 <CUSTOMERID>lq11020.c.0200</CUSTOMERID>
 <QUESTIONID>1309270001</QUESTIONID>
 <ANSWERID></ANSWERID>
 <GROUPID>020099990042921</GROUPID>
 <CERTTYPECODE>c</CERTTYPECODE>
 <QRYBEGINPOS>1</QRYBEGINPOS><QRYNUM>20</QRYNUM>
 </ICBCACSS>';
 
 
 
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

  COMMIT;
END;
--0
--1
--v_xml.getclobval()
end ;
--%stub%--