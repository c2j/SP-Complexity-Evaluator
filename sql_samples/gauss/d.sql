ZIPMULTI_OLDCREATE OR REPLACE PACKAGE PKG_OAM_QS IS
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

END PKG_OAM_QS
/