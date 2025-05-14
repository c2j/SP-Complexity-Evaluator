CREATE OR REPLACE PACKAGE PKG_OAM_QS IS
 TYPE REF_CUR IS REF CURSOR;
 /*********************************************************************/
 --功能：         各种查询的存储过程
 --功能模块：     查询统计
 --作者：         nil
 --时间：         2010-07-12
 --报表内容       --对应存储过程

 FUNCTION FUNC_GET_ROLE_ZIP_PWD (i_id IN VARCHAR2, i_status_type IN VARCHAR2) RETURN VARCHAR2;

 PROCEDURE ZIPMULTI(C CLOB, B BLOB, S varchar2);
 
 PROCEDURE ZIPMULTI_OLD(C CLOB, B BLOB, S varchar2);
 /*********************************************************************
   --名称:FUNC_GET_ROLE_ZIP_PWD
   --描述:根据id和角色人资格类型获取zip加密密码
   --功能:根据id和角色人资格类型获取zip加密密码
   --模块:
   --作者:chenwj
   --参数:
 *********************************************************************/
 FUNCTION FUNC_GET_ROLE_ZIP_PWD (i_id IN VARCHAR2, i_status_type IN VARCHAR2)RETURN VARCHAR2
 AS
   v_pwd     varchar2(2000);
   v_plan_id oam_plan_info.plan_id%type;
   v_role_id oam_role_info.id%type;
 BEGIN
   IF length(i_id) = 13 THEN
     --企业
     v_plan_id := pkg_oam_common.func_get_planidbycoid(i_id);
     IF v_plan_id IS NULL THEN
       RETURN v_pwd;
     END IF;
   ELSIF length(i_id) = 6 THEN
     v_plan_id := i_id;
   END IF;

   RETURN v_pwd;
 EXCEPTION
   WHEN OTHERS THEN
     RETURN '';
 END;

/*********************************************************************/
 --名称：         PKG_OAM_QS
 --功能：         调用JAVA的程序们
 --功能模块：     查询统计
 --作者：         nil
 --时间：         2010-07-12
 --报表内容       --对应存储过程

 /*********************************************************************/

 PROCEDURE ZIPMULTI(C CLOB, B BLOB, S varchar2) AS
   
 BEGIN
   
 IF  pkg_oam_common.func_get_oam_setting_value('MUTI_ESCAPE_FLAG') = '0' THEN
      ZIPMULTI_NEW(C,B,S);
 ELSE
      ZIPMULTI_OLD(C,B,S);
 END IF;    
     
 END;    
 /*********************************************************************/
 --名称：         PKG_OAM_QS
 --功能：         调用JAVA的程序们
 --功能模块：     查询统计
 --作者：         wangyf
 --时间：         2010-07-12
 -- 老的程序，不支持生僻字

 /*********************************************************************/

 PROCEDURE ZIPMULTI_OLD(C CLOB, B BLOB, S varchar2) AS
   LANGUAGE JAVA NAME 'Util.zipMulti(oracle.sql.CLOB,oracle.sql.BLOB,java.lang.String)';
   
/*********************************************************************/
 --名称：         PKG_OAM_QS
 --功能：         调用JAVA的程序们
 --功能模块：     查询统计
 --作者：         wangyf
 --时间：         2010-07-12
 --新的程序，支持生僻字，unescapeHtml4转码

/*********************************************************************/
 PROCEDURE ZIPMULTI_NEW(C CLOB, B BLOB, S varchar2) AS
   LANGUAGE JAVA NAME 'Util.zipMultiEscape(oracle.sql.CLOB,oracle.sql.BLOB,java.lang.String)';    

/*  function func_check_date(i_plan_id varchar2,
                          i_co_id   varchar2,
                          i_query_date varchar2)
                          return number is
  v_flag                  number:=0;
  v_op_date
  begin
    if i_plan_id is not null then
      select
    else

    end if;
  end;*/


 /******************************************************************************
 --存储过程名：    PROC_asyn_download_query
 --存储过程描述：  异步下载申请查询
 --功能：          异步下载申请查询
 --功能模块：      查询统计模块-异步下载
 --作者：          NIL
 --时间：          2011-03-29
 ******************************************************************************/
 PROCEDURE PROC_asyn_download_query(i_user_id   IN VARCHAR2,
                                    i_plan_id   in varchar2,
                                    i_co_id     in varchar2,
                                    i_begin_num in varchar2,
                                    i_query_num in varchar2,
                                    o_flag      OUT NUMBER,
                                    O_TOTALNUM  OUT NUMBER,
                                    o_cur       OUT ref_cur) is

   V_PROC_NAME DB_LOG.PROC_NAME%TYPE := 'PKG_OAM_QS.PROC_asyn_download_query';
   V_STEP      NUMBER := null;
   v_msg       varchar2(1000) := null;
 begin
   o_flag := 0;

   PACK_LOG.LOG(v_proc_name,
                PACK_LOG.START_STEP,
                PACK_LOG.START_MSG || '|' || i_user_id || '|' || i_plan_id || '|' || i_co_id || '|' || i_begin_num || '|' || i_query_num,
                PACK_LOG.INFO_LEVEL);

   select count(1)
     into O_TOTALNUM
     from oam_app t, oam_asynchronous_download b
    where t.app_no = b.app_no
      and t.app_type = '021601'
      and t.app_user = i_user_id
      and (t.plan_id = i_plan_id or i_plan_id is null)
      and (i_co_id is null or
          (t.co_id = i_co_id and nvl(t.flag,0) <> 1) or
          (t.co_id like substr(i_co_id, 1, 7)||'%' and nvl(t.flag,0) = 1))
      and t.valid_flag = 0;

   if O_TOTALNUM > 0 then
     open o_cur for
       select a.app_no,
              a.reportname,
              a.app_time,
              a.state,
              a.memo,
              a.able,
              a.del
         from (select b.app_no,
                      c.reportname,
                      t.app_time,
                      decode(t.app_state,
                             9,
                             '查询结果超过限定条数，请缩小范围重新查询',
                             8,
                             '',
                             1,
                             '',
                             10,
                             '生成失败',
                             7,
                             '文件校验出错') memo,
                      decode(t.app_state,
                             1,
                             '正在生成',
                             8,
                             '已生成',
                             9,
                             '生成失败',
                             10,
                             '生成失败',
                             7,
                             '文件校验出错',
                             '校验中') state,
                      decode(t.app_state,
                             1,
                             ' ',
                             8,
                             '<button onclick="download(' || b.app_no ||
                             ')">下载</button>',
                             9,
                             '<button onclick="download(' || b.app_no ||
                             ')">下载</button>',
                             10,
                             '<button onclick="download(' || b.app_no ||
                             ')">下载</button>',
                             7,
                             '<button onclick="download(' || b.app_no ||
                             ')">下载</button>',
                             ' ') able,
                            '<button onclick="del(' || b.app_no ||','||
                            t.app_state || ','||
                            --新增一个标志位，
                            case
                              when to_char(t.app_time,'yyyymmdd')=to_char(sysdate,'yyyymmdd')
                              then 0
                              else 1
                            end ||
                             ')">删除</button>' del,
                      row_number() over(ORDER BY t.app_no DESC) rown
               --如果app_state是1则前台下载按钮不能使用，为8才可以用
                 from oam_app t, oam_asynchronous_download b, excel_rpt c
                where t.app_no = b.app_no
                  and c.reportid = b.report_id
                  and t.app_type = '021601'
                  and (t.plan_id = i_plan_id or i_plan_id is null)
                  and (i_co_id is null or
                      (t.co_id = i_co_id and nvl(t.flag,0) <> 1) or
                      (t.co_id like substr(i_co_id, 1, 7)||'%' and nvl(t.flag,0) = 1))
                  and t.app_user = i_user_id
                  and t.valid_flag = 0) a
        where a.rown >= i_begin_num
          and a.rown <= (i_begin_num + i_query_num - 1);

   ELSE
     o_cur := PKG_OAM_COMMON.FUNC_NULLCURSOR;
   END IF;
   PACK_LOG.LOG(v_proc_name,
                PACK_LOG.END_STEP,
                PACK_LOG.START_MSG || '+++' || i_user_id,
                PACK_LOG.INFO_LEVEL);

 EXCEPTION
   WHEN OTHERS THEN
     O_FLAG := 1;
     v_msg  := SQLERRM || '|' || DBMS_UTILITY.FORMAT_ERROR_BACKTRACE;

     PACK_LOG.LOG(v_proc_name,
                  v_step,
                  v_msg || '+++' || i_user_id,
                  PACK_LOG.ERR_LEVEL);

 end;

 /*********************************************************************
   --名称:PROC_ASYN_DOWNLOAD_SUBMIT
   --描述:报表异步下载申请提交
   --功能:报表异步下载申请提交
   --模块:查询统计
   --作者:niling
   --时间:2011-03-29
   --参数:
 *********************************************************************/
 PROCEDURE proc_asyn_download_submit
 (
     i_app_no    IN VARCHAR2,
     i_user_id   IN VARCHAR2,
     i_role_id   IN VARCHAR2,
     i_plan_id   IN VARCHAR2,
     i_co_id     IN VARCHAR2,
     i_report_id IN VARCHAR2,
     i_prm       IN VARCHAR2,
     o_flag      OUT NUMBER,
     o_msg       OUT VARCHAR2
 ) IS
     v_proc_name db_log.proc_name%TYPE := 'PKG_OAM_QS.PROC_ASYN_DOWNLOAD_SUBMIT';
     v_params    db_log.info%TYPE := substrb(i_app_no || '|' || i_user_id || '|' ||
                                             i_role_id || '|' || i_plan_id || '|' ||
                                             i_co_id || '|' || i_report_id || '|' ||
                                             i_prm,
                                             1,
                                             4000);
     v_step      NUMBER := NULL;
     v_msg       VARCHAR2(4000);
     exec EXCEPTION;

     v_app_no    oam_app.app_no%TYPE;
     v_prm       CLOB := substrb(i_prm, 1, 4000);
     v_asyn_download_num NUMBER;
     v_count             NUMBER;
 BEGIN
     pack_log.info(v_proc_name,
                   pack_log.start_step,
                   pack_log.start_msg || '|' || v_params);
    

     o_msg := v_app_no;

     pack_log.info(v_proc_name,
                   pack_log.end_step,
                   pack_log.end_msg || '|' || v_params);
 EXCEPTION
     WHEN exec THEN
         o_flag := 1;
         o_msg := nvl(v_msg, '文件更新失败');
         pack_log.warn(v_proc_name, v_step, o_msg || '|' || v_params);
     WHEN OTHERS THEN
         o_flag := 1;
         o_msg := SQLERRM || '|' || dbms_utility.format_error_backtrace;
         pack_log.error(v_proc_name,
                        v_step,
                        '异常' || SQLERRM || '|' || v_params);
 END proc_asyn_download_submit;

 /*********************************************************************
 --名称:PROC_ASYN_DOWNLOAD_SUBMIT
 --描述:报表异步下载申请提交
 --功能:报表异步下载申请提交
 --模块:查询统计
 --作者:niling
 --时间:2011-03-29
 --参数:
 *********************************************************************/
 PROCEDURE proc_uasyn_download_submit
 (
     i_app_no    IN VARCHAR2,
     i_user_id   IN VARCHAR2,
     i_role_id   IN VARCHAR2,
     i_plan_id   IN VARCHAR2,
     i_co_id     IN VARCHAR2,
     i_report_id IN VARCHAR2,
     i_prm       IN VARCHAR2,
     o_flag      OUT NUMBER,
     o_msg       OUT VARCHAR2
 ) IS
     v_proc_name db_log.proc_name%TYPE := 'PKG_OAM_QS.PROC_UASYN_DOWNLOAD_SUBMIT';
     v_params    db_log.info%TYPE := substrb(i_app_no || '|' || i_user_id || '|' ||
                                             i_role_id || '|' || i_plan_id || '|' ||
                                             i_co_id || '|' || i_report_id || '|' ||
                                             i_prm,
                                             1,
                                             4000);
     v_step      NUMBER := NULL;
     v_msg       VARCHAR2(4000);
     exec EXCEPTION;

     v_app_no            oam_app.app_no%TYPE;
     v_prm               CLOB := substrb(i_prm, 1, 4000);
     v_plan_id           oam_co_info.plan_id%TYPE;
     v_aud_time_plan     oam_plan_info.plan_time%TYPE;
     v_asyn_download_num NUMBER;
     v_count             NUMBER;
 BEGIN
     pack_log.info(v_proc_name,
                   pack_log.start_step,
                   pack_log.start_msg || '|' || v_params);
    

     o_msg := v_app_no;
     pack_log.info(v_proc_name,
                   pack_log.end_step,
                   pack_log.end_msg || '|' || v_params);
 EXCEPTION
     WHEN exec THEN
         o_flag := 1;
         o_msg := nvl(v_msg, '文件更新失败');
         pack_log.warn(v_proc_name, v_step, o_msg || '|' || v_params);
     WHEN OTHERS THEN
         o_flag := 1;
         o_msg := '报表异步下载申请提交异常！';
         pack_log.error(v_proc_name,
                        v_step,
                        '异常' || SQLERRM || '|' || v_params);
 END proc_uasyn_download_submit;



 /******************************************************************************
 --存储过程名：    PROC_asyn_download_cbt
 --存储过程描述：  异步下载申请查询
 --功能：          异步下载申请查询
 --功能模块：      查询统计模块-异步下载
 --作者：          NIL
 --时间：          2011-03-29
 ******************************************************************************/
 PROCEDURE proc_asyn_download_cbt(i_app_no IN VARCHAR2) IS
     v_col       pkg_oam_common.arrytype;
     v_prm       oam_asynchronous_download.prm%TYPE;
     v_sql2      CLOB;
     v_step      NUMBER := NULL;
     v_proc_name db_log.proc_name%TYPE := 'PKG_OAM_QS.PROC_ASYN_DOWNLOAD_CBT';
     v_flag      NUMBER := 0;
     v_msg       VARCHAR2(4000) := '';
     v_clob      CLOB;
     v_blob      BLOB;
     v_file_name VARCHAR2(300) := '';
     v_count     NUMBER := 0;
     v_reportid  oam_asynchronous_download.report_id%TYPE;
 v_user_id   oam_asynchronous_download.user_id%TYPE := NULL;
 BEGIN
     pack_log.log(v_proc_name,
                  pack_log.start_step,
                  pack_log.start_msg || '+++' || i_app_no,
                  pack_log.info_level);
     
     pack_log.log(v_proc_name,
                  pack_log.end_step,
                  pack_log.end_msg || '+++' || i_app_no,
                  pack_log.info_level);
 EXCEPTION
     WHEN OTHERS THEN
         ROLLBACK;
         UPDATE oam_app t SET t.app_state = 10 WHERE t.app_no = i_app_no;
         UPDATE oamu_app t SET t.app_state = 10 WHERE t.app_no = i_app_no;
         DELETE FROM oam_batch_alt_tmp t WHERE t.app_no = i_app_no;
         COMMIT;
         v_msg := SQLERRM;
         pack_log.log(v_proc_name,
                      1,
                      pack_log.start_msg || '+++' || v_msg || '+++' ||
                      i_app_no,
                      pack_log.err_level);
 END;

 /******************************************************************************
 --存储过程名：    PROC_asyn_download_cbt
 --存储过程描述：  异步下载申请查询
 --功能：          异步下载申请查询
 --功能模块：      查询统计模块-异步下载
 --作者：          NIL
 --时间：          2011-03-29
 ******************************************************************************/
 PROCEDURE proc_asyn_download_cbt_t
 (
     i_app_no IN VARCHAR2,
     o_flag   OUT VARCHAR2
 ) IS
     v_col       pkg_oam_common.arrytype;
     v_prm       oam_asynchronous_download.prm%TYPE;
     v_sql2      CLOB;
     v_step      NUMBER := NULL;
     v_proc_name db_log.proc_name%TYPE := 'PKG_OAM_QS.PROC_ASYN_DOWNLOAD_CBT_T';
     v_flag      NUMBER := 0;
     v_msg       VARCHAR2(4000) := '';
     v_clob      CLOB;
     v_blob      BLOB;
     v_file_name VARCHAR2(300) := '';
     v_count     NUMBER := 0;
     v_countflag NUMBER := 0;
     v_reportid  oam_asynchronous_download.report_id%TYPE;
 BEGIN

     pack_log.log(v_proc_name,
                  pack_log.start_step,
                  pack_log.start_msg || '+++' || i_app_no,
                  pack_log.info_level);
     
     pack_log.log(v_proc_name,
                  pack_log.end_step,
                  pack_log.end_msg || '+++' || i_app_no,
                  pack_log.info_level);
 EXCEPTION
     WHEN OTHERS THEN
         ROLLBACK;
         UPDATE oam_app t SET t.app_state = 10 WHERE t.app_no = i_app_no;
         UPDATE oamu_app t SET t.app_state = 10 WHERE t.app_no = i_app_no;
         COMMIT;
         v_msg := SQLERRM;
         pack_log.log(v_proc_name,
                      1,
                      pack_log.start_msg || '+++' || v_msg || '+++' ||
                      i_app_no,
                      pack_log.err_level);
 END;

END PKG_OAM_QS
/
