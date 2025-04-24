CREATE OR REPLACE PACKAGE BODY BIGFUND.CTP_MX_PCKG_INIT AS

  PROCEDURE proc_get_appinfo(appinfo OUT ref_cursor) AS
  BEGIN
    OPEN appinfo FOR
      SELECT appname_en, version, type FROM ctp_mx_app_info;
  END;

  PROCEDURE proc_get_servinfo(appname IN varchar2, servinfo OUT ref_cursor) AS
  BEGIN
    OPEN servinfo FOR
      SELECT a.ip AS ip, b.port port
        FROM ctp_mx_server_ip a, ctp_mx_server_port b
       WHERE a.hostname = b.hostname
         AND a.hostName = appname;
  END;

  --end proc_get_servinfo;

  PROCEDURE proc_get_monitor_switch(appname    IN varchar2,
                                    switchinfo OUT ref_cursor) AS
  BEGIN
    OPEN switchinfo FOR
      SELECT switchtype, channeltype, action
        FROM ctp_mx_monitor_flag
       WHERE hostname = appname
       ORDER BY hostname, switchtype;
  END;

  --end proc_get_monitor_switch;

  PROCEDURE proc_get_useablity_info(checkers OUT ref_cursor) AS
  BEGIN
    OPEN checkers FOR
      SELECT name, modulecode, submodulecode, checker, status, msg
        FROM ctp_mx_usability_info
       WHERE ismonitor = '1'
       ORDER BY name;
  END;

  --end proc_get_useablity_info;

  PROCEDURE proc_get_trade_define(trades OUT ref_cursor) AS
  BEGIN
    OPEN trades FOR
      SELECT b.eventCode  eventCode,
             b.eventName  eventName,
             a.eventlevel eventlevel,
             a.trancode   trancode,
             a.occurtime  occurtime
        FROM ctp_mx_trans_error_level a, ctp_mx_trans_error_info b
       WHERE a.eventCode = b.eventCode
         AND b.ismonitor = '1'
       ORDER BY eventCode, trancode, occurtime;
  END;

  PROCEDURE proc_get_resource_define(resources OUT ref_cursor) AS
  BEGIN
    OPEN resources FOR
      SELECT b.resourcecode,
             b.resourcerate,
             b.type,
             b.monitor,
             a.occurtime,
             a.eventlevel
        FROM ctp_mx_resource_level_define a, ctp_mx_resource_info b
       WHERE b.resourcecode = a.resourcecode
         AND b.ismonitor = '1'
       ORDER BY resourcecode, a.occurtime, a.eventlevel;
  END;

--end proc_get_trade_define;
  PROCEDURE proc_get_trade_info(tradeRef OUT ref_cursor) AS
  BEGIN
    OPEN tradeRef FOR
      SELECT eventCode, eventName FROM ctp_mx_trans_error_info where ismonitor = '1';
  END;

  PROCEDURE proc_get_resource_info(resourceRef OUT ref_cursor) AS
  BEGIN
    OPEN resourceRef FOR
      SELECT resourceCode, resourceName FROM ctp_mx_resource_info where ismonitor = '1';
  END;
  PROCEDURE proc_get_arm_config(in_hostname in varchar2,
                                armRef      OUT ref_cursor) AS
  BEGIN
    OPEN armRef FOR
      SELECT opname, paramtype, paramvalue, action
        FROM ctp_arm_config
       where hostname = in_hostname;
  END;

  PROCEDURE proc_get_staticsparma_valve(paramRef OUT ref_cursor) AS
  BEGIN
    OPEN paramRef FOR
      SELECT name, trancode, valve,begintime,endtime
        FROM ctp_mx_statistics_valve;
  END;

  PROCEDURE proc_get_staticsparma_period(paramRef OUT ref_cursor) AS
  BEGIN
    OPEN paramRef FOR
      SELECT name, period
        FROM ctp_mx_statistics_period;
  END;
END CTP_MX_PCKG_INIT;