CREATE OR REPLACE PROCEDURE BIGFUND.AAATEST1 is


BEGIN
  -----begin 
  v_i_fcode := 'ACSS7002I';
  v_o_fcode := 'ACSS7002O';
  -- v_max_seq            v_all_acnt_info_base.acnt_id%TYPE;
  v_date    := to_char(SYSDATE, 'yyyymmdd');

  /*
   select a.* from v_all_acnt_info_base a
   where id=1;
  */
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

      p_err_message:='';

    v_updatestringT:=p_in_cutstr;                         --传入的字符串
    v_num:=0;
    while instr(v_updatestringT,'~||~')>0 loop
        v_num:=v_num+1;
        if v_num>100 then
           p_err_message:='-1';
           return;
        end if;
        v_lengthcol:=length(v_updatestringT);
        v_pos_colcost:=instr(v_updatestringT,'~||~');

        if v_pos_colcost<>1 then
           v_colcost(v_num):=substr(v_updatestringT,1,v_pos_colcost-1);
        else
           v_colcost(v_num):=null;
        end if;
        if v_lengthcol>v_pos_colcost then
           v_updatestringT:=substr(v_updatestringT,v_pos_colcost+4,v_lengthcol);
        else
           v_updatestringT:=null;
        end if;
        v_colcost.extend;
    end loop;
    v_num:=v_num+1;
    v_colcost(v_num):=v_updatestringT;

    --传入拆分有的值,并校验长度
    --基金代码
    if length(v_colcost(4))>9 then
       p_err_message:='基金代码长度超出范围！';
       return;
    else
       v_fundcode:=v_colcost(4);
    end if;

    IF fnc_com_if_fund(v_fundcode) = 0 THEN
      --不是小组合
      p_err_message := '汇总类型组合无法处理该请求！';
      ROLLBACK;
      RETURN;
    END IF;

    --单位代码
    if length(v_colcost(5))>8 then
       p_err_message:='单位代码长度超出范围！';
       return;
    else
       v_deptcode:=v_colcost(5);
    end if;

    --单位名称
    if length(v_colcost(6))>80 then
       p_err_message:='单位名称长度超出范围！';
       return;
    else
       v_dept_name:=v_colcost(6);
    end if;

    --单位账号
    if length(v_colcost(7))>40 then
       p_err_message:='单位账号长度超出范围！';
       return;
    else
       v_dept_account:=v_colcost(7);
    end if;

    --账号名称
    if length(v_colcost(8))>60 then
       p_err_message:='账号名称长度超出范围！';
       return;
    else
       v_account_name:=v_colcost(8);
    end if;

    --开户银行
    if length(v_colcost(9))>40 then
       p_err_message:='开户银行长度超出范围！';
       return;
    else
       v_dept_bank:=v_colcost(9);
    end if;

    --行内外账号标志
    if v_deptcode is null then
      if length(v_colcost(10))>1 then
         p_err_message:='行内外账号标志长度超出范围！';
         return;
      else
         v_in_out_type:=v_colcost(10);
      end if;
    else
        select t.account_attribute
          into v_in_out_type
          from PAR_ACCOUNT_CUSSENT@aas_link t
         where t.fund_code = v_fundcode
           and t.dept_code = v_deptcode;
    end if;

    --行内外账号标志
    if length(v_colcost(10))>1 then
       p_err_message:='行内外账号标志长度超出范围！';
       return;
    else
       v_in_out_type:=v_colcost(10);
    end if;

    --开户地区
    if v_deptcode is null then
      if length(v_colcost(11))>4 then
         p_err_message:='开户地区长度超出范围！';
         return;
      else
         v_area_code:=v_colcost(11);
      end if;
    else
        select t.area_code
          into v_area_code
          from PAR_ACCOUNT_CUSSENT@aas_link t
         where t.fund_code = v_fundcode
           and t.dept_code = v_deptcode;
    end if;

    --开户地区
    if length(v_colcost(11))>4 then
       p_err_message:='开户地区长度超出范围！';
       return;
    else
       v_area_code:=v_colcost(11);
    end if;

    --如果账户是虚拟账户，行内/行外标志，及开户行地区都为空
    if v_account_attr='1' then
       if v_in_out_type is null then
          p_err_message:='虚拟账号行内行外标志应为空！';
          return;
       end if;
       if v_area_code is null then
          p_err_message:='虚拟账号地区代码应为空！';
          return;
       end if;
    end if;

    --1.1如果是行内账号，计算校验位判断账号有效性
    --1.2如果是行内账号，地区代码必须为空
    --1.3行外账号，地区代码不能为空
    if v_in_out_type='0' then
       if FNC_COM_ACCOUNT_CHECK(trim(v_dept_account))<> substr(trim(v_dept_account),18,2) then
          p_err_message:='行内账号校验错误！';
          return;
       end if;
       if v_area_code is null then
          p_err_message:='行内账号地区代码必须为空！';
          return;
       end if;
    elsif v_in_out_type='1' then
       if v_area_code is null then
          p_err_message:='行外账号地区代码不能为空！';
          return;
       end if;
    end if;

    --判断单位账号不能为空
    if v_dept_account is null then
       p_err_message:='单位账号不能为空！';
       return;
    end if;

    --判断账号名称不能为空
    if v_account_name is null then
       p_err_message:='账号名称不能为空！';
       return;
    end if;

    --判断账户是否重复录入 20061228 modified by tufr
    SELECT COUNT(*)
      INTO v_count
      FROM par_account_cussent@aas_link t
     WHERE t.fund_code = v_fundcode
       AND t.dept_account = v_dept_account
       AND t.dept_account_name = v_account_name;
    IF v_count <> 0 THEN
       p_err_message := '账户在老系统已存在! ';
       RETURN;
    END IF;

    --判断账户是否重复录入 20070321 caohan
    SELECT COUNT(*)
      INTO v_count
      FROM par_clr_acnt_info t
     WHERE t.v_fund_code = v_fundcode
       AND t.v_dept_acnt = v_dept_account;
    IF v_count <> 0 THEN
       p_err_message := '账户在系统中已存在! ';
       RETURN;
    END IF;

    --非虚拟账号开户银行不能为空
    if v_account_attr is null then
       if v_dept_bank is null then
          p_err_message:='非虚拟账户开户银行不能为空！';
          return;
       end if;
       if v_in_out_type is null then
          p_err_message:='非虚拟账户行内行外标志不能为空！';
          return;
       end if;
    end if;

    BEGIN
      select max(to_number(v_acnt_code)) + 1
        into v_max_seq
        from par_clr_acnt_info;
    EXCEPTION WHEN OTHERS THEN
      v_max_seq := 1;
    END;

    --插入账号
    BEGIN
      insert into par_clr_acnt_info
         (v_fund_code,
          v_dept_code,
          v_acnt_type,
          v_dept_acnt,
          v_acnt_name,
          v_bank_name,
          v_sys_flag,
          v_zone_code,
          v_debt_cred,
          v_acnt_code,
          INURE_BEGIN_DATE,
          INURE_END_DATE)
      values
         (v_fundcode,
          '02001032',  --工商银行
          '4',
          v_dept_account,
          v_account_name,
          v_dept_bank,
          decode(v_in_out_type,'0','1','2'),
          v_area_code,
          '2',
          v_max_seq,
          to_char(SYSDATE,'yyyymmdd'),
          '99991231');
    exception when others then
      p_err_message:='新增账号失败! '||SQLERRM;
      ROLLBACK;
      return;
    end;

   commit;

   p_err_message:='';

-- a = 'SELECT T1.* FROM' || '(SELECT T.*, ROWNUM RNUM FROM P_SQL2  T WHERE ROWNUM < TO_NUMBER(' || IN_QRYBEGINPOS ||
--            ') + TO_NUMBER (' || IN_QRYNUM ||
--            ')) T1 WHERE T1.RNUM >= TO_NUMBER (' || IN_QRYBEGINPOS || ')';

DELETE mid_yjqs_detail
    WHERE scdm = v_scdm
      AND bcrq = p_i_date
   
   ;
  COMMIT;
END;
