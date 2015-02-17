package com.archibus.eventhandler.reservations;

import java.util.*;

import com.archibus.jobmanager.EventHandlerContext;

/**
 * Contains common event handlers used in Rooms reservation WFRs.
 */
public class ReservationsRoomHandler extends ReservationsEventHandlerBase {
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN createWorkRequest wfr
    // ---------------------------------------------------------------------------------------------
    /**
     * gets the identifier of a created or modified room reservation and generates or updates the
     * work request associated to thisreservation in needed Inputs: res_id res_id (String);
     * parent_id parent_id (String); Outputs: message error message in necesary case
     * 
     * @param context Event handler context.
     */
    public void createWorkRequest(final EventHandlerContext context) {
        
        final String RULE_ID = "createWorkRequest";
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        // Get the input res_id parameter
        String resId = (String) context.getParameter("res_id");
        final String parentId = (String) context.getParameter("res_parent");
        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [res_id]: "+resId+" ");
        
        String tradeToCreate = "";
        String vendorToCreate = "";
        String sql = "";
        boolean allOk = false;
        
        // createWorkRequest rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "CREATEWORKREQUEST_WFR",
                    "CREATEWORKREQUESTERROR", null);
        // createWorkRequest setup and cleanup description messages
        final String setupdesc =
                localizeMessage(context, ACTIVITY_ID, "CREATEWORKREQUEST_WFR",
                    "CREATEWORKREQUESTSETUPDESCRIPTION", null);
        final String cleanupdesc =
                localizeMessage(context, ACTIVITY_ID, "CREATEWORKREQUEST_WFR",
                    "CREATEWORKREQUESTCLEANUPDESCRIPTION", null);
        // createWorkRequest reservation comments description messages
        final String reservationComments =
                localizeMessage(context, ACTIVITY_ID, "CREATEWORKREQUEST_WFR",
                    "CREATEWORKREQUESTRESERVATIONCOMMENTSDESCRIPTION", null);
        
        try {
            // BEGIN: it gets one o more room reserve
            final Vector vectorRes_Id = new Vector();
            if (!parentId.equals("0")) {
                sql = " SELECT res_id " + " FROM reserve " + " WHERE res_parent= " + parentId;
                // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 0]: "+sql);
                
                final List recordsSql0 = retrieveDbRecords(context, sql);
                
                if (!recordsSql0.isEmpty()) {
                    int i = 0;
                    for (final Iterator it = recordsSql0.iterator(); it.hasNext();) {
                        final Map values = (Map) it.next();
                        vectorRes_Id.add(i, values.get("res_id"));
                        i++;
                    }
                } else {
                    vectorRes_Id.add(0, "0");
                }
            }
            // END: it gets one o more room reserve
            else {
                vectorRes_Id.add(0, resId);
            }
            
            // BEGIN: For each res_id do createWorkRequest
            for (final Iterator it = vectorRes_Id.iterator(); it.hasNext();) {
                resId = (String) it.next();
                if (!resId.equals("0") && !resId.equals("")) {
                    // Guo added 2008-08-20 to solve KB3019197
                    final String statusOfRoomReservation =
                            (String) selectDbValue(context, "reserve_rm", "status", "res_id="
                                    + resId);
                    // -----------------------------------------------------------------------------------
                    // BEGIN: WORK REQUEST FOR TRADE
                    // -----------------------------------------------------------------------------------
                    
                    // BEGIN: the system must get the filed "tr_id" in "tr"
                    // table to generate the work
                    sql =
                            " SELECT tr.tr_id as tradetocreate "
                                    + " FROM reserve_rm, rm_arrange_type, tr "
                                    + " WHERE reserve_rm.rm_arrange_type_id=rm_arrange_type.rm_arrange_type_id "
                                    + " AND reserve_rm.res_id= " + resId
                                    + " AND rm_arrange_type.tr_id IS NOT NULL "
                                    + " AND rm_arrange_type.tr_id=tr.tr_id "
                                    + " AND tr.wr_from_reserve=1 ";
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 1]:
                    // "+sql);
                    
                    final List recordsSql1 = retrieveDbRecords(context, sql);
                    
                    // BEGIN: If the system must create the wr for the asociated
                    // trade
                    if (!recordsSql1.isEmpty()) {
                        final Map recordOfSql1 = (Map) recordsSql1.get(0);
                        tradeToCreate = getString(recordOfSql1, "tradetocreate");
                        
                        // BEGIN: cancel wr for this room reservation and
                        // diferent trade
                        sql =
                                " UPDATE wr " + " SET status = 'Can', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND tr_id IS NOT NULL "
                                        + " AND tr_id <> " + literal(context, tradeToCreate)
                                        + " AND status IN ('R','Rev','A','AA') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 2]:
                        // "+sql);
                        executeDbSql(context, sql, false);
                        
                        // BEGIN: stop wr for this room reservation and diferent
                        // trade
                        sql =
                                " UPDATE wr " + " SET status = 'S', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND tr_id IS NOT NULL "
                                        + " AND tr_id <> " + literal(context, tradeToCreate)
                                        + " AND status IN ('I','HP','HA','HL') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 3]:
                        // "+sql);
                        executeDbSql(context, sql, false);
                        
                        // BEGIN: exist work request for this trade
                        sql =
                                " SELECT wr_id " + " FROM wr " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND status <> 'Can' "
                                        + " AND tr_id = " + literal(context, tradeToCreate)
                                        + " ORDER BY time_assigned ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 4]:
                        // "+sql);
                        
                        final List recordsSql2 = retrieveDbRecords(context, sql);
                        
                        if (!recordsSql2.isEmpty()) {
                            
                            final Map recordOfSql2_1 = (Map) recordsSql2.get(0);
                            final String wr_id_1 = getString(recordOfSql2_1, "wr_id");
                            final Map recordOfSql2_2 = (Map) recordsSql2.get(1);
                            final String wr_id_2 = getString(recordOfSql2_2, "wr_id");
                            
                            // BEGIN: Update work request for setting up and
                            // cleaning
                            if (isOracle(context)) {
                                sql =
                                        " UPDATE wr SET ("
                                                + " res_id,rmres_id,time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                + " time_requested,tr_id,phone,dv_id,dp_id"
                                                + " ) = ( SELECT "
                                                + " reserve_rm.res_id,reserve_rm.rmres_id, "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " reserve_rm.bl_id,reserve_rm.fl_id,reserve_rm.rm_id, "
                                                + " reserve.user_requested_by, "
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.pre_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rm.date_start, "
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rm.time_start",
                                                    formatSqlIsNull(context,
                                                        "-(rm_arrange.pre_block) , 0"))
                                                + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + literal(context, tradeToCreate)
                                                + ", "
                                                + " reserve.phone,reserve.dv_id,reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id )"
                                                + " WHERE wr.wr_id= " + wr_id_1;
                            } else {
                                sql =
                                        " UPDATE wr " + " SET res_id=reserve_rm.res_id, "
                                                + " rmres_id=reserve_rm.rmres_id, "
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " bl_id=reserve_rm.bl_id, "
                                                + " fl_id=reserve_rm.fl_id, "
                                                + " rm_id=reserve_rm.rm_id, "
                                                + " requestor=reserve.user_requested_by, "
                                                + " est_labor_hours="
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.pre_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + " status= "
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ","
                                                + " date_assigned=reserve_rm.date_start, "
                                                + " time_assigned = "
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rm.time_start",
                                                    formatSqlIsNull(context,
                                                        "-(rm_arrange.pre_block) , 0"))
                                                + ", "
                                                + " date_requested = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " time_requested="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " tr_id= "
                                                + literal(context, tradeToCreate)
                                                + ", "
                                                + " phone=reserve.phone, "
                                                + " dv_id=reserve.dv_id, "
                                                + " dp_id=reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND wr.wr_id= "
                                                + wr_id_1
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            }
                            
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 5]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                            if (isOracle(context)) {
                                sql =
                                        " UPDATE wr SET ("
                                                + " res_id,rmres_id,time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                + " time_requested,tr_id,phone,dv_id,dp_id"
                                                + " ) = ( SELECT "
                                                + " reserve_rm.res_id,reserve_rm.rmres_id, "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " reserve_rm.bl_id,reserve_rm.fl_id,reserve_rm.rm_id, "
                                                + " reserve.user_requested_by, "
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.post_block , 0")
                                                + "/60 ,"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rm.date_start,reserve_rm.time_end, "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + literal(context, tradeToCreate)
                                                + ", "
                                                + " reserve.phone,reserve.dv_id,reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id )"
                                                + " WHERE wr.wr_id= " + wr_id_2;
                            } else {
                                sql =
                                        " UPDATE wr " + " SET res_id=reserve_rm.res_id, "
                                                + " rmres_id=reserve_rm.rmres_id, "
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " bl_id=reserve_rm.bl_id, "
                                                + " fl_id=reserve_rm.fl_id, "
                                                + " rm_id=reserve_rm.rm_id, "
                                                + " requestor=reserve.user_requested_by, "
                                                + " est_labor_hours="
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.post_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + " status= "
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ","
                                                + " date_assigned=reserve_rm.date_start, "
                                                + " time_assigned=reserve_rm.time_end, "
                                                + " date_requested="
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " time_requested="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " tr_id="
                                                + literal(context, tradeToCreate)
                                                + ", "
                                                + " phone=reserve.phone, "
                                                + " dv_id=reserve.dv_id, "
                                                + " dp_id=reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND wr.wr_id= "
                                                + wr_id_2
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            }
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 6]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                        }
                        // END: Update work request for setting up and cleaning
                        
                        // BEGIN: Create two new work request for setting up and
                        // cleaning
                        else {
                            
                            sql =
                                    " INSERT INTO wr "
                                            + " (res_id,rmres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status, "
                                            + " date_assigned,time_assigned,date_requested,time_requested,tr_id,phone, "
                                            + " dv_id,dp_id,description,prob_type) "
                                            + " SELECT reserve_rm.res_id, reserve_rm.rmres_id, "
                                            + " reserve_rm.bl_id, reserve_rm.fl_id, reserve_rm.rm_id, "
                                            + " reserve.user_requested_by as requestor, " + " "
                                            + formatSqlIsNull(context, "rm_arrange.pre_block , 0")
                                            + "/60 as est_labor_hours, "
                                            // Guo changed 2008-08-20 to solve KB3019197
                                            + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                    : " 'A'")
                                            + " as status, reserve_rm.date_start as date_assigned, "
                                            + " "
                                            + formatSqlAddMinutesToExpression(
                                                context,
                                                "reserve_rm.time_start",
                                                formatSqlIsNull(context,
                                                    "-(rm_arrange.pre_block),0"))
                                            + " as time_assigned, "
                                            + " "
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + " as date_requested, "
                                            + " "
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " as time_requested, "
                                            + " "
                                            + literal(context, tradeToCreate)
                                            + " as tr_id, "
                                            + " reserve.phone, reserve.dv_id, reserve.dp_id, "
                                            // PC KB 3038222
                                            + literal(context, setupdesc)
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + literal(context, reservationComments)
                                            + formatSqlConcat(context)
                                            + "' '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve.comments) , ''")
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve_rm.comments) , ''")
                                            + " AS description, "
                                            + " 'RES. SETUP' as prob_type "
                                            + " FROM reserve_rm, reserve, rm_arrange "
                                            + " WHERE reserve_rm.res_id=reserve.res_id "
                                            + " AND reserve_rm.res_id= "
                                            + resId
                                            + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                            + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                            + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                            + " AND reserve_rm.config_id=rm_arrange.config_id "
                                            + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 7]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, true);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                            sql =
                                    " INSERT INTO wr  "
                                            + " (res_id,rmres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status, "
                                            + " date_assigned,time_assigned,date_requested,time_requested,tr_id,phone, "
                                            + " dv_id,dp_id,description,prob_type) "
                                            + " SELECT reserve_rm.res_id, reserve_rm.rmres_id, "
                                            + " reserve_rm.bl_id, reserve_rm.fl_id, reserve_rm.rm_id, "
                                            + " reserve.user_requested_by as requestor, " + " "
                                            + formatSqlIsNull(context, "rm_arrange.post_block , 0")
                                            + "/60 as est_labor_hours, "
                                            // Guo changed 2008-08-20 to solve KB3019197
                                            + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                    : " 'A'")
                                            + " as status, reserve_rm.date_start as date_assigned, "
                                            + " reserve_rm.time_end as time_assigned, "
                                            + " "
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + " as date_requested, "
                                            + " "
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " as time_requested, "
                                            + " "
                                            + literal(context, tradeToCreate)
                                            + " as tr_id, "
                                            + " reserve.phone, reserve.dv_id, reserve.dp_id, "
                                            // PC KB 3038222
                                            + literal(context, cleanupdesc)
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + literal(context, reservationComments)
                                            + formatSqlConcat(context)
                                            + "' '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve.comments) , ''")
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve_rm.comments) , ''")
                                            + " AS description, "
                                            + " 'RES. CLEANUP' as prob_type "
                                            + " FROM reserve_rm, reserve, rm_arrange "
                                            + " WHERE reserve_rm.res_id=reserve.res_id "
                                            + " AND reserve_rm.res_id= "
                                            + resId
                                            + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                            + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                            + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                            + " AND reserve_rm.config_id=rm_arrange.config_id "
                                            + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 8]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, true);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                        }
                        // END: Create two new work request for setting up and
                        // cleaning
                        
                    }
                    // END: If the system must create the wr for the asociated
                    // trade
                    
                    // BEGIN: If the system doesn't have to create the wr for
                    // the asociated trade
                    else {
                        
                        // Cancel and Stop all wr
                        sql =
                                " UPDATE wr " + " SET status = 'Can', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id = " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND tr_id IS NOT NULL "
                                        + " AND status IN ('R','Rev','A','AA') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 9]:
                        // "+sql);
                        
                        executeDbSql(context, sql, false);
                        
                        sql =
                                " UPDATE wr  " + " SET status = 'S', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id = " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND tr_id IS NOT NULL "
                                        + " AND status IN ('I','HP','HA','HL') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 10]: "+sql);
                        
                        executeDbSql(context, sql, false);
                    }
                    // END: If the system doesn't have to create the wr for the
                    // asociated trade
                    // -----------------------------------------------------------------------------------
                    // END: WORK REQUEST FOR TRADE
                    // -----------------------------------------------------------------------------------
                    
                    // -----------------------------------------------------------------------------------
                    // BEGIN: WORK REQUEST FOR VENDOR
                    // -----------------------------------------------------------------------------------
                    // BEGIN: the system must get the filed "vn_id" in "vn"
                    // table to generate the work
                    sql =
                            " SELECT vn.vn_id as vendortocreate "
                                    + " FROM reserve_rm, rm_arrange_type, vn "
                                    + " WHERE reserve_rm.rm_arrange_type_id=rm_arrange_type.rm_arrange_type_id "
                                    + " AND reserve_rm.res_id= " + resId
                                    + " AND rm_arrange_type.vn_id IS NOT NULL "
                                    + " AND rm_arrange_type.vn_id=vn.vn_id "
                                    + " AND vn.wr_from_reserve=1 ";
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 11]:
                    // "+sql);
                    
                    final List recordsSql3 = retrieveDbRecords(context, sql);
                    
                    // BEGIN: If the system must create the wr for the asociated
                    // vendor
                    if (!recordsSql3.isEmpty()) {
                        
                        final Map recordOfSql3 = (Map) recordsSql3.get(0);
                        vendorToCreate = getString(recordOfSql3, "vendortocreate");
                        
                        // BEGIN: cancel wr for this room reservation and
                        // diferent vendor
                        sql =
                                " UPDATE wr SET status = 'Can', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND vn_id IS NOT NULL "
                                        + " AND vn_id <> " + literal(context, vendorToCreate) + " "
                                        + " AND status IN ('R','Rev','A','AA') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 12]: "+sql);
                        
                        executeDbSql(context, sql, false);
                        
                        // BEGIN: stop wr for this room reservation and diferent
                        // vendor
                        sql =
                                " UPDATE wr SET status = 'S', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND vn_id IS NOT NULL "
                                        + " AND vn_id <> " + literal(context, vendorToCreate) + " "
                                        + " AND status IN ('I','HP','HA','HL') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 13]: "+sql);
                        
                        executeDbSql(context, sql, false);
                        
                        // BEGIN: exist work request for this vendor
                        sql =
                                " SELECT wr_id " + " FROM wr " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND status <> 'Can' "
                                        + " AND vn_id = " + literal(context, vendorToCreate) + " "
                                        + " ORDER BY time_assigned ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 14]: "+sql);
                        
                        final List recordsSql4 = retrieveDbRecords(context, sql);
                        
                        if (!recordsSql4.isEmpty()) {
                            
                            final Map recordOfSql4_1 = (Map) recordsSql4.get(0);
                            final String wr_id_1 = getString(recordOfSql4_1, "wr_id");
                            final Map recordOfSql4_2 = (Map) recordsSql4.get(1);
                            final String wr_id_2 = getString(recordOfSql4_2, "wr_id");
                            
                            // BEGIN: Update work request for setting up and
                            // cleaning
                            
                            // PC changed 2008-09-02 to solve KB item 3019287
                            if (isOracle(context)) {
                                sql =
                                        " UPDATE wr SET ("
                                                + " res_id,rmres_id,time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                + " time_requested,vn_id,phone,dv_id,dp_id"
                                                + " ) = ( SELECT "
                                                + " reserve_rm.res_id,reserve_rm.rmres_id, "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " reserve_rm.bl_id,reserve_rm.fl_id,reserve_rm.rm_id, "
                                                + " reserve.user_requested_by, "
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.pre_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rm.date_start, "
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rm.time_start",
                                                    formatSqlIsNull(context,
                                                        "-(rm_arrange.pre_block) , 0"))
                                                + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + literal(context, vendorToCreate)
                                                + ", "
                                                + " reserve.phone,reserve.dv_id,reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id )"
                                                + " WHERE wr.wr_id= " + wr_id_1;
                            } else {
                                sql =
                                        " UPDATE wr " + " SET res_id=reserve_rm.res_id, "
                                                + " rmres_id=reserve_rm.rmres_id, "
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " bl_id=reserve_rm.bl_id, "
                                                + " fl_id=reserve_rm.fl_id, "
                                                + " rm_id=reserve_rm.rm_id, "
                                                + " requestor=reserve.user_requested_by, "
                                                + " est_labor_hours="
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.pre_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + " status= "
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ","
                                                + " date_assigned=reserve_rm.date_start, "
                                                + " time_assigned = "
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rm.time_start",
                                                    formatSqlIsNull(context,
                                                        "-(rm_arrange.pre_block) , 0"))
                                                + " , "
                                                + " date_requested="
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " time_requested="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " vn_id="
                                                + literal(context, vendorToCreate)
                                                + ", "
                                                + " phone=reserve.phone, "
                                                + " dv_id=reserve.dv_id, "
                                                + " dp_id=reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND wr.wr_id="
                                                + wr_id_1
                                                + " "
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                                // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                                // 15]: "+sql);
                            }
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                            // PC changed 2008-09-02 to solve KB item 3019287
                            if (isOracle(context)) {
                                sql =
                                        " UPDATE wr SET ("
                                                + " res_id,rmres_id,time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                + " time_requested,vn_id,phone,dv_id,dp_id"
                                                + " ) = ( SELECT "
                                                + " reserve_rm.res_id,reserve_rm.rmres_id, "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " reserve_rm.bl_id,reserve_rm.fl_id,reserve_rm.rm_id, "
                                                + " reserve.user_requested_by, "
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.post_block , 0")
                                                + "/60 ,"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rm.date_start,reserve_rm.time_end, "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + literal(context, vendorToCreate)
                                                + ", "
                                                + " reserve.phone,reserve.dv_id,reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id )"
                                                + " WHERE wr.wr_id= " + wr_id_2;
                            } else {
                                
                                sql =
                                        " UPDATE wr " + " SET res_id=reserve_rm.res_id, "
                                                + " rmres_id=reserve_rm.rmres_id, "
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " bl_id=reserve_rm.bl_id, "
                                                + " fl_id=reserve_rm.fl_id, "
                                                + " rm_id=reserve_rm.rm_id, "
                                                + " requestor=reserve.user_requested_by, "
                                                + " est_labor_hours="
                                                + formatSqlIsNull(context,
                                                    "rm_arrange.post_block , 0")
                                                + "/60 , "
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + " status= "
                                                + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ","
                                                + " date_assigned=reserve_rm.date_start, "
                                                + " time_assigned=reserve_rm.time_end, "
                                                + " date_requested="
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " time_requested="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ", "
                                                + " vn_id="
                                                + literal(context, vendorToCreate)
                                                + ", "
                                                + " phone=reserve.phone, "
                                                + " dv_id=reserve.dv_id, "
                                                + " dp_id=reserve.dp_id "
                                                + " FROM reserve_rm, reserve, rm_arrange "
                                                + " WHERE reserve_rm.res_id=reserve.res_id "
                                                + " AND reserve_rm.res_id= "
                                                + resId
                                                + " AND wr.wr_id="
                                                + wr_id_2
                                                + " "
                                                + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                                + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                                + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                                + " AND reserve_rm.config_id=rm_arrange.config_id "
                                                + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                                // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                                // 16]: "+sql);
                            }
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                        }
                        // END: Update work request for setting up and cleaning
                        
                        // BEGIN: Create two new work request for setting up and
                        // cleaning
                        else {
                            
                            sql =
                                    " INSERT INTO wr "
                                            + " (res_id,rmres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status, "
                                            + " date_assigned,time_assigned,date_requested,time_requested,vn_id,phone, "
                                            + " dv_id,dp_id,description,prob_type) "
                                            + " SELECT reserve_rm.res_id, reserve_rm.rmres_id, "
                                            + " reserve_rm.bl_id, reserve_rm.fl_id, reserve_rm.rm_id, "
                                            + " reserve.user_requested_by as requestor, " + " "
                                            + formatSqlIsNull(context, "rm_arrange.pre_block , 0")
                                            + "/60 as est_labor_hours, "
                                            // Guo changed 2008-08-20 to solve KB3019197
                                            + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                    : " 'A'")
                                            + " as status, reserve_rm.date_start as date_assigned, "
                                            + " "
                                            + formatSqlAddMinutesToExpression(
                                                context,
                                                "reserve_rm.time_start",
                                                formatSqlIsNull(context,
                                                    "-(rm_arrange.pre_block) , 0"))
                                            + " as time_assigned, "
                                            + " "
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + " as date_requested, "
                                            + " "
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " as time_requested, "
                                            + " "
                                            + literal(context, vendorToCreate)
                                            + " as vn_id, "
                                            + " reserve.phone, reserve.dv_id, reserve.dp_id, "
                                            // PC KB 3038222
                                            + literal(context, setupdesc)
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + literal(context, reservationComments)
                                            + formatSqlConcat(context)
                                            + "' '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve.comments) , ''")
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve_rm.comments) , ''")
                                            + " AS description, "
                                            + " 'RES. SETUP' as prob_type "
                                            + " FROM reserve_rm, reserve, rm_arrange "
                                            + " WHERE reserve_rm.res_id=reserve.res_id "
                                            + " AND reserve_rm.res_id= "
                                            + resId
                                            + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                            + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                            + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                            + " AND reserve_rm.config_id=rm_arrange.config_id "
                                            + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 17]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                            sql =
                                    " INSERT INTO wr "
                                            + " (res_id,rmres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status, "
                                            + " date_assigned,time_assigned,date_requested,time_requested,vn_id,phone, "
                                            + " dv_id,dp_id,description,prob_type) "
                                            + " SELECT reserve_rm.res_id, reserve_rm.rmres_id, "
                                            + " reserve_rm.bl_id, reserve_rm.fl_id, reserve_rm.rm_id, "
                                            + " reserve.user_requested_by as requestor, " + " "
                                            + formatSqlIsNull(context, "rm_arrange.post_block , 0")
                                            + "/60 as est_labor_hours, "
                                            // Guo changed 2008-08-20 to solve KB3019197
                                            + ("Awaiting App.".equals(statusOfRoomReservation) ? " 'R'"
                                                    : " 'A'")
                                            + " as status, reserve_rm.date_start as date_assigned, "
                                            + " reserve_rm.time_end as time_assigned, "
                                            + " "
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + " as date_requested, "
                                            + " "
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " as time_requested, "
                                            + " "
                                            + literal(context, vendorToCreate)
                                            + " as vn_id, "
                                            + " reserve.phone, reserve.dv_id, reserve.dp_id, "
                                            // PC KB 3038222
                                            + literal(context, cleanupdesc)
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + literal(context, reservationComments)
                                            + formatSqlConcat(context)
                                            + "' '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve.comments) , ''")
                                            + formatSqlConcat(context)
                                            + "'. '"
                                            + formatSqlConcat(context)
                                            + formatSqlIsNull(context,
                                                "RTRIM(reserve_rm.comments) , ''")
                                            + " AS description, "
                                            + " 'RES. CLEANUP' as prob_type "
                                            + " FROM reserve_rm, reserve, rm_arrange "
                                            + " WHERE reserve_rm.res_id=reserve.res_id "
                                            + " AND reserve_rm.res_id= "
                                            + resId
                                            + " AND reserve_rm.bl_id=rm_arrange.bl_id "
                                            + " AND reserve_rm.fl_id=rm_arrange.fl_id "
                                            + " AND reserve_rm.rm_id=rm_arrange.rm_id "
                                            + " AND reserve_rm.config_id=rm_arrange.config_id "
                                            + " AND reserve_rm.rm_arrange_type_id=rm_arrange.rm_arrange_type_id ";
                            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                            // 18]: "+sql);
                            
                            try {
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                        + sql, errMessage, e);
                            }
                            
                        }
                        // END: Create two new work request for setting up and
                        // cleaning
                        
                    }
                    // END: If the system must create the wr for the asociated
                    // vendor
                    
                    // BEGIN: If the system doesn't have to create the wr for
                    // the asociated vendor
                    else {
                        
                        // BEGIN: Cancel and Stop all wr
                        sql =
                                " UPDATE wr SET status = 'Can', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND vn_id IS NOT NULL "
                                        + " AND status IN ('R','Rev','A','AA') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 19]: "+sql);
                        
                        executeDbSql(context, sql, false);
                        
                        sql =
                                " UPDATE wr SET status = 'S', " + " time_stat_chg = "
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + ", " + " date_stat_chg = "
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + " " + " WHERE res_id= " + resId
                                        + " AND rmres_id IS NOT NULL " + " AND vn_id IS NOT NULL "
                                        + " AND status IN ('I','HP','HA','HL') ";
                        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql
                        // 20]: "+sql);
                        
                        executeDbSql(context, sql, false);
                    }
                    // END: If the system doesn't have to create the wr for the
                    // asociated vendor
                    
                    // -----------------------------------------------------------------------------------
                    // END: WORK REQUEST FOR VENDOR
                    // -----------------------------------------------------------------------------------
                    
                    allOk = true;
                } // end if
            }// end for
             // END: For each res_id do createWorkRequest
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql, errMessage,
                e);
        }
        
        if (!allOk) {
            context.addResponseParameter("message", errMessage);
        } else {
            // Guo changed 2008-09-12 to remove all executeDbCommit(context)
            // executeDbCommit(context);
            // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [sql 21]: do
            // commit");
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END: createWorkRequest wfr
    // ---------------------------------------------------------------------------------------------

} // class
