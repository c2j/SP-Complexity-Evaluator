CREATE OR REPLACE PACKAGE PKG_FACC_DATAPROC IS
  PROCEDURE PROC_UPDATE_BALANCE(i_date IN VARCHAR2, o_flag OUT VARCHAR2);
  -- Author  : KFZX-ZHUTING
  -- Created : 2008-4-25
  -- Purpose : 更新财务余额
  PROCEDURE PROC_SUBBALANCE(i_date IN VARCHAR2, o_flag OUT VARCHAR2);
  -- Author  : KFZX-ZHUTING
  -- Created : 2007-2-26
  -- Purpose : 汇总财务余额

  
  /************************************************************************
    --函数名称：      PKG_FACC_DATAPROC.PROC_PA_AUTO_DELAY
    --函数描述：      
    --功能：          记录自动展期台账日志
    --功能模块：
    --作者：          kfzx-weicm
    --时间：          2021-05-12
    --返回值：        O_FLAG    OUT  返回标志
  *************************************************************************/  
  PROCEDURE PROC_PA_AUTO_DELAY(i_date IN VARCHAR2, o_flag OUT VARCHAR2);
END PKG_FACC_DATAPROC;
/
CREATE OR REPLACE PACKAGE BODY PKG_FACC_DATAPROC IS
  /*********************************************************************
   --存储过程名称：  PROC_UPDATE_BALANCE
   --存储过程描述：  更新财务余额
   --功能：
   --功能模块：      日终批量模块
   --作者：          诸婷
   --时间：          2008-4-25
   --参数说明：
                                    i_date     IN VARCHAR2,  系统日期
                                    o_flag     OUT VARCHAR2, 成功失败标志 0成功 失败为具体信息

  **********************************************************************/
  PROCEDURE PROC_UPDATE_BALANCE(i_date IN VARCHAR2, o_flag OUT VARCHAR2) IS
    v_proc_name     db_log.proc_name%TYPE; -- 存储过程名
    v_step_no       db_log.step_no%TYPE; -- 步骤名
    v_info          db_log.info%TYPE; -- 描述
  begin

    --置初始标志
    --==============================================================================================================
    --STEP1  日志变量赋值
    --==============================================================================================================
    v_step_no   := '1';
    v_proc_name := 'PROC_UPDATE_BALANCE';
    o_flag      := 1;

  --==============================================================================================================
    --STEP2  业务处理
    --==============================================================================================================
   v_step_no := '2.1';

    begin

      delete from facc_fiact_tmp t where t.workdate = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除当日主机差错临时表数据出错!';
        RAISE;
    end;

    v_step_no := '2.2';
    --根据财务帐户余额表汇总主机帐户余额
    begin
      insert into facc_fiact_tmp
        (currtype, balance, workdate, accno, balf)
      /*select t.currtype, sum(t.balance), i_date, t.accno, max(t.balf)
                                        from facc_fiact t
                                       group by t.accno, t.currtype;*/ --20070628 修改今天有变化的所有accno
        select t.currtype, sum(t.balance), i_date, t.accno, max(t.balf)
          from facc_fiact t
         where exists (select 1
                  from facc_fiact v
                 where v.workdate = i_date
                   and t.accno = v.accno
                   and t.currtype = v.currtype)
           and t.workdate <= i_date
         group by t.accno, t.currtype;

    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '汇总主机财务帐户余额出错!';
        RAISE;
    end;

    --删除备份历史数据
    v_step_no := '2.3';
    begin
      delete from facc_fiact_his t where t.data_date = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除备份科目余额历史数据出错!';
        RAISE;
    end;

    --备份历史数据
    v_step_no := '2.4';
    begin
      insert into facc_fiact_his
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         data_date,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               i_date,
               t.subcode3,
               t.subcode2
          from facc_fiact t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份科目余额历史数据出错!';
        RAISE;
    end;

    --删除用于生成报表的当日余额表历史数据
    v_step_no := '2.5';
    begin
      delete from facc_fiact_use t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除备份科目余额历史数据出错!';
        RAISE;
    end;

    --备份用于生成报表的当日余额表历史数据
    v_step_no := '2.6';
    begin
      insert into facc_fiact_use
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               t.subcode3,
               t.subcode2
          from facc_fiact t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份用于生成报表的当日余额表历史数据出错!';
        RAISE;
    end;


    --更新系统余额表本期余额到上期余额
    v_step_no := '2.7';
    begin
      update facc_fiact t
         set t.lstbal  = t.balance,
             t.tddramt = 0,
             t.tdcramt = 0,
             t.tddrino = 0,
             t.tdcrino = 0
       where t.workdate <= i_date;

    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '更新本年科目余额日期出错!';
        RAISE;
    end;

    --备份更新数据
    v_step_no := '2.8';
    begin
      insert into facc_fiact_log
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         pkg,
         time,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               'PROC_UPDATE_BALANCE',
               TO_CHAR(SYSTIMESTAMP, 'yyyymmddHH24MISSFF'),
               t.subcode3,
               t.subcode2
          from facc_fiact t
          where t.workdate = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份科目余额历史数据出错!';
        RAISE;
    end;

/*   --分行代管机构季末余额汇总  tangjq
   PKG_FACC_YEARCOST_QUERY.proc_agent_balance_generate(i_date,o_flag);
   if (o_flag <> '0') then
   ROLLBACK;
   return;
   end if;*/
    --==============================================================================================================
    --STEP3  记录日志
    --==============================================================================================================
    v_step_no := '3';
    --日志级别为2(INFO)的日志信息，在执行后加COMMIT以提交包括日志记录在内的事务
    pack_log.log(v_proc_name, -- 存储过程名
                 v_step_no, -- 步骤名
                 o_flag, -- 描述
                 '2' -- 日志级别
                 );
    --提交
    commit;

    --置成功标志
    o_flag := 0;

  exception
    WHEN OTHERS THEN
      ROLLBACK;
      o_flag := 'PROC_UPDATE_BALANCE模块内部错误:' || o_flag || sqlerrm;

      --记录出错日志，等级为4（ERROR），最后提交日志记录的事务
      pack_log.log(v_proc_name, -- 存储过程名
                   v_step_no, -- 步骤名
                   o_flag, -- 描述
                   '4');
      RETURN;

  end PROC_UPDATE_BALANCE;

END;

PROCEDURE PROC_UPDATE_BALANCE111(i_date IN VARCHAR2, o_flag OUT VARCHAR2) IS
    v_proc_name     db_log.proc_name%TYPE; -- 存储过程名
    v_step_no       db_log.step_no%TYPE; -- 步骤名
    v_info          db_log.info%TYPE; -- 描述
  begin

    --置初始标志
    --==============================================================================================================
    --STEP1  日志变量赋值
    --==============================================================================================================
    v_step_no   := '1';
    v_proc_name := 'PROC_UPDATE_BALANCE';
    o_flag      := 1;

  --==============================================================================================================
    --STEP2  业务处理
    --==============================================================================================================
   v_step_no := '2.1';

    begin

      delete from facc_fiact_tmp t where t.workdate = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除当日主机差错临时表数据出错!';
        RAISE;
    end;

    v_step_no := '2.2';
    --根据财务帐户余额表汇总主机帐户余额
    begin
      insert into facc_fiact_tmp
        (currtype, balance, workdate, accno, balf)
      /*select t.currtype, sum(t.balance), i_date, t.accno, max(t.balf)
                                        from facc_fiact t
                                       group by t.accno, t.currtype;*/ --20070628 修改今天有变化的所有accno
        select t.currtype, sum(t.balance), i_date, t.accno, max(t.balf)
          from facc_fiact t
         where exists (select 1
                  from facc_fiact v
                 where v.workdate = i_date
                   and t.accno = v.accno
                   and t.currtype = v.currtype)
           and t.workdate <= i_date
         group by t.accno, t.currtype;

    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '汇总主机财务帐户余额出错!';
        RAISE;
    end;

    --删除备份历史数据
    v_step_no := '2.3';
    begin
      delete from facc_fiact_his t where t.data_date = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除备份科目余额历史数据出错!';
        RAISE;
    end;

    --备份历史数据
    v_step_no := '2.4';
    begin
      insert into facc_fiact_his
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         data_date,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               i_date,
               t.subcode3,
               t.subcode2
          from facc_fiact t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份科目余额历史数据出错!';
        RAISE;
    end;

    --删除用于生成报表的当日余额表历史数据
    v_step_no := '2.5';
    begin
      delete from facc_fiact_use t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '删除备份科目余额历史数据出错!';
        RAISE;
    end;

    --备份用于生成报表的当日余额表历史数据
    v_step_no := '2.6';
    begin
      insert into facc_fiact_use
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               t.subcode3,
               t.subcode2
          from facc_fiact t;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份用于生成报表的当日余额表历史数据出错!';
        RAISE;
    end;


    --更新系统余额表本期余额到上期余额
    v_step_no := '2.7';
    begin
      update facc_fiact t
         set t.lstbal  = t.balance,
             t.tddramt = 0,
             t.tdcramt = 0,
             t.tddrino = 0,
             t.tdcrino = 0
       where t.workdate <= i_date;

    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '更新本年科目余额日期出错!';
        RAISE;
    end;

    --备份更新数据
    v_step_no := '2.8';
    begin
      insert into facc_fiact_log
        (fancode,
         seqno,
         currtype,
         tddramt,
         tdcramt,
         tddrino,
         tdcrino,
         lstbal,
         balance,
         workdate,
         accno,
         balf,
         pkg,
         time,
         subcode3,
         subcode2)
        select t.fancode,
               t.seqno,
               t.currtype,
               t.tddramt,
               t.tdcramt,
               t.tddrino,
               t.tdcrino,
               t.lstbal,
               t.balance,
               t.workdate,
               t.accno,
               t.balf,
               'PROC_UPDATE_BALANCE',
               TO_CHAR(SYSTIMESTAMP, 'yyyymmddHH24MISSFF'),
               t.subcode3,
               t.subcode2
          from facc_fiact t
          where t.workdate = i_date;
    exception
      WHEN OTHERS THEN
        ROLLBACK;
        o_flag := '备份科目余额历史数据出错!';
        RAISE;
    end;

/*   --分行代管机构季末余额汇总  tangjq
   PKG_FACC_YEARCOST_QUERY.proc_agent_balance_generate(i_date,o_flag);
   if (o_flag <> '0') then
   ROLLBACK;
   return;
   end if;*/
    --==============================================================================================================
    --STEP3  记录日志
    --==============================================================================================================
    v_step_no := '3';
    --日志级别为2(INFO)的日志信息，在执行后加COMMIT以提交包括日志记录在内的事务
    pack_log.log(v_proc_name, -- 存储过程名
                 v_step_no, -- 步骤名
                 o_flag, -- 描述
                 '2' -- 日志级别
                 );
    --提交
    commit;

    SELECT * where a=1;
    
    --置成功标志
    o_flag := 0;

  exception
    WHEN OTHERS THEN
      ROLLBACK;
      o_flag := 'PROC_UPDATE_BALANCE模块内部错误:' || o_flag || sqlerrm;

      --记录出错日志，等级为4（ERROR），最后提交日志记录的事务
      pack_log.log(v_proc_name, -- 存储过程名
                   v_step_no, -- 步骤名
                   o_flag, -- 描述
                   '4');
      RETURN;

  end PROC_UPDATE_BALANCE111;

END PKG_FACC_DATAPROC;
/
