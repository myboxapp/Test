package com.archibus.eventhandler.reservations;

import java.util.*;

import org.json.JSONObject;

import com.archibus.jobmanager.EventHandlerContext;

public class ReservationsResourcesHandler extends ReservationsEventHandlerBase {
    
    /*
     * CreateResourceWr
     */
    public void createResourceWr(final EventHandlerContext context, final String parentId,
            final String newResId) {
        final String RULE_ID = "createResourceWr";
        // this.log.info("Executing '" + ACTIVITY_ID + "-" + RULE_ID + "' ..... ");
        
        final int mtu = 1;
        // String res_id = (String) context.getParameter("res_id");
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
        
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "SAVERESOURCERESERVATIONS_WFR",
                    "SAVERESOURCEERROR", null);
        String sql = "";
        Object[] tradeToCreateObject = null;
        Object[] vendorToCreateObject = null;
        String wrId1 = "";
        String wrId2 = "";
        final List listResId = new ArrayList();
        
        boolean allOk = false;
        
        if (!"0".equals(parentId)) {
            sql = "SELECT res_id FROM reserve WHERE res_parent = " + literal(context, parentId);
            final List recurringResIdList = retrieveDbRecords(context, sql);
            for (final Iterator recurringResIterator = recurringResIdList.iterator(); recurringResIterator
                .hasNext();) {
                final String resIdTemp = getString((Map) recurringResIterator.next(), "res_id");
                listResId.add(resIdTemp);
            }
        } else if (!"0".equals(newResId)) {
            listResId.add(newResId);
        }
        for (final Iterator it = listResId.iterator(); it.hasNext();) {
            final String res_id = (String) it.next();
            sql =
                    "SELECT status, rsres_id, resource_id FROM reserve_rs WHERE res_id = "
                            + literal(context, res_id);
            final List resourceReservations = retrieveDbRecords(context, sql);
            for (int i = 0; i < resourceReservations.size(); i++) {
                final JSONObject event = new JSONObject();
                // event = resourceReservations.getJSONObject(i);
                
                // event.put("starttime", transformDate(event.getString("starttime")));
                // event.put("endtime", transformDate(event.getString("endtime")));
                
                event.put("status", getString((Map) resourceReservations.get(i), "status"));
                event.put("resource_id",
                    getString((Map) resourceReservations.get(i), "resource_id"));
                event.put("rsres_id", getString((Map) resourceReservations.get(i), "rsres_id"));
                
                if (event.getString("status").equals("Cancelled")
                        || event.getString("status").equals("Rejected")) {
                    // cancelled and rejected reservation: Cancel all work requests for this
                    // reservation by
                    // performing
                    try {
                        sql =
                                " UPDATE wr SET status='Can', date_stat_chg="
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + "," + " time_stat_chg="
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + " WHERE rsres_id="
                                        + literal(context, event.getString("rsres_id"))
                                        + " AND res_id=" + literal(context, res_id)
                                        + " AND status IN ('R','Rev','A','AA')";
                        
                        executeDbSql(context, sql, false);
                        
                        sql =
                                " UPDATE wr SET status='S', date_stat_chg="
                                        + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                        + "," + " time_stat_chg="
                                        + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                        + " WHERE rsres_id="
                                        + literal(context, event.getString("rsres_id"))
                                        + " AND res_id=" + literal(context, res_id)
                                        + " AND status IN ('I','HP','HA','HL')";
                        
                        executeDbSql(context, sql, false);
                        
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                + ": Could not retrieve existing work requests: ", errMessage, e);
                    }
                } else {
                    List tradeToCreate = new ArrayList();
                    List vendorToCreate = new ArrayList();
                    
                    // Guo added 2008-08-20 to solve KB3019197
                    final String statusOfReservation = event.getString("status");
                    
                    // Check if the resource has trade that accepts wrs from reservations
                    try {
                        sql =
                                " SELECT resource_std.tr_id FROM resource_std "
                                        + " LEFT OUTER JOIN tr ON resource_std.tr_id = tr.tr_id "
                                        + " LEFT OUTER JOIN resources ON resources.resource_std = resource_std.resource_std "
                                        + " WHERE tr.wr_from_reserve = 1 AND resources.resource_id = "
                                        + literal(context, event.getString("resource_id"));
                        
                        tradeToCreate = selectDbRecords(context, sql);
                        
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                + ": Could not retrieve trades for work requests: ", errMessage, e);
                    }
                    
                    // Check if resource has vendor that accepts wrs from reservations
                    try {
                        sql =
                                " SELECT resource_std.vn_id FROM resource_std "
                                        + " LEFT OUTER JOIN vn ON resource_std.vn_id = vn.vn_id "
                                        + " LEFT OUTER JOIN resources ON resources.resource_std = resource_std.resource_std "
                                        + " WHERE vn.wr_from_reserve = 1 AND resources.resource_id = "
                                        + literal(context, event.getString("resource_id"));
                        
                        vendorToCreate = selectDbRecords(context, sql);
                        
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                + ": Could not retrieve vendors for work requests: ", errMessage, e);
                    }
                    
                    if (tradeToCreate.size() > 0) {
                        // Start cancelling all existing work requests assigned to a different trade
                        String tradetoc = "";
                        tradeToCreateObject = (Object[]) tradeToCreate.get(0);
                        tradetoc = tradeToCreateObject[0].toString();
                        
                        try {
                            sql =
                                    " UPDATE wr SET status='Can', date_stat_chg="
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + "," + " time_stat_chg="
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " WHERE rsres_id="
                                            + literal(context, event.getString("rsres_id"))
                                            + " AND res_id=" + literal(context, res_id)
                                            + " AND tr_id IS NOT NULL" + " AND tr_id <> "
                                            + literal(context, tradetoc)
                                            + " AND status IN ('R','Rev','A','AA')";
                            
                            executeDbSql(context, sql, false);
                            
                            sql =
                                    " UPDATE wr SET status='S', date_stat_chg="
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + "," + " time_stat_chg="
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " WHERE rsres_id="
                                            + literal(context, event.getString("rsres_id"))
                                            + " AND res_id=" + literal(context, res_id)
                                            + " AND tr_id IS NOT NULL" + " AND tr_id <> "
                                            + literal(context, tradetoc)
                                            + " AND status IN ('I','HP','HA','HL')";
                            
                            executeDbSql(context, sql, false);
                            
                        } catch (final Throwable e) {
                            handleError(
                                context,
                                ACTIVITY_ID
                                        + "-"
                                        + RULE_ID
                                        + ": Could not cancel existing work requests for different trades: ",
                                errMessage, e);
                        }
                        
                        List wrFound = new ArrayList();
                        
                        // If it's a existing reservation: Get possible existing work requests for
                        // this
                        // trade
                        if (!event.getString("rsres_id").equals("")) {
                            try {
                                sql =
                                        " SELECT wr_id FROM wr WHERE rsres_id="
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id=" + literal(context, res_id)
                                                + " AND tr_id=" + literal(context, tradetoc)
                                                + " AND status <> 'Can'"
                                                + " AND status <> 'S' ORDER BY time_assigned";
                                
                                wrFound = selectDbRecords(context, sql);
                            } catch (final Throwable e) {
                                handleError(
                                    context,
                                    ACTIVITY_ID
                                            + "-"
                                            + RULE_ID
                                            + ": Could not retrieve existing work requests for trades: ",
                                    errMessage, e);
                            }
                        }
                        
                        if (event.getString("rsres_id").equals("") || wrFound.size() <= 0) {
                            tradeToCreateObject = (Object[]) tradeToCreate.get(0);
                            final String tradeTocreate = tradeToCreateObject[0].toString();
                            try {
                                // Insert new wr record for setup by
                                sql =
                                        " INSERT INTO wr (res_id,rsres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status,"
                                                + " date_assigned,time_assigned,date_requested,time_requested,tr_id,phone,dv_id,dp_id,"
                                                + " description,date_stat_chg,time_stat_chg,prob_type)"
                                                + " SELECT reserve.res_id,reserve_rs.rsres_id, reserve_rs.bl_id,reserve_rs.fl_id,reserve_rs.rm_id,"
                                                + " reserve.user_requested_by,ROUND(CAST("
                                                + formatSqlIsNull(context, "resources.pre_block,0")
                                                + " AS real)/60,2),"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rs.date_start, "
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rs.time_start",
                                                    " ("
                                                            + mtu
                                                            + "*"
                                                            + formatSqlIsNull(context,
                                                                "-(resources.pre_block),0") + ")")
                                                + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + literal(context, tradeTocreate)
                                                + ",reserve.phone, reserve.dv_id,reserve.dp_id,"
                                                // PC KB 3038222
                                                + " '"
                                                + setupdesc
                                                + ". "
                                                + reservationComments
                                                + " '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve.comments), '' ")
                                                + formatSqlConcat(context)
                                                + "'. '"
                                                + formatSqlConcat(context) 
                                                + formatSqlIsNull(context, "RTRIM(reserve_rs.comments), '' ") + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ",'RES. SETUP'"
                                                + " FROM reserve_rs"
                                                + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id "
                                                + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id"
                                                + " WHERE Reserve.res_id = "
                                                + literal(context, res_id)
                                                + " AND reserve_rs.rsres_id = "
                                                + literal(context, event.getString("rsres_id"));
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not create work request for trade set-up: ",
                                    errMessage, e);
                            }
                            
                            try {
                                // Insert new wr record for cleaning by
                                sql =
                                        " INSERT INTO wr (res_id,rsres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status,"
                                                + " date_assigned,time_assigned,date_requested,time_requested,tr_id,phone,dv_id,dp_id,"
                                                + " description, date_stat_chg,time_stat_chg,prob_type)"
                                                + " SELECT reserve.res_id,reserve_rs.rsres_id, reserve_rs.bl_id, reserve_rs.fl_id, reserve_rs.rm_id,"
                                                + " reserve.user_requested_by,ROUND(CAST("
                                                + formatSqlIsNull(context, "resources.post_block,0")
                                                + " AS real)/60,2),"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rs.date_start,reserve_rs.time_end,"
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + literal(context, tradeTocreate)
                                                + ",reserve.phone, reserve.dv_id,reserve.dp_id,"
                                                // PC KB 3038222
                                                + " '"
                                                + cleanupdesc
                                                + ". "
                                                + reservationComments
                                                + " '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve.comments), '' ")
                                                + formatSqlConcat(context)
                                                + "'. '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve_rs.comments), ''") + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ",'RES. CLEANUP'"
                                                + " FROM reserve_rs"
                                                + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id "
                                                + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id"
                                                + " WHERE Reserve.res_id = "
                                                + literal(context, res_id)
                                                + " AND reserve_rs.rsres_id = "
                                                + literal(context, event.getString("rsres_id"));
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not create work request for trade clean-up: ",
                                    errMessage, e);
                            }
                        } // End if(!event.getString("rsres_id").equals("")||wrFound.size() <= 0)
                        else {
                            final Object[] wrObject = (Object[]) wrFound.get(0);
                            final Object[] wrObject1 = (Object[]) wrFound.get(1);
                            wrId1 = wrObject[0].toString();
                            wrId2 = wrObject1[0].toString();
                            
                            try {
                                // Update [wrId1] for setup work request
                                
                                // PC changed to solve KB item 3019287
                                if (isOracle(context)) {
                                    
                                    sql =
                                            " UPDATE wr SET ("
                                                    + " time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                    + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                    + " time_requested,phone,dv_id,dp_id"
                                                    + " ) = ( SELECT "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " reserve_rs.bl_id,"
                                                    + " reserve_rs.fl_id,"
                                                    + " reserve_rs.rm_id,"
                                                    + " reserve.user_requested_by,"
                                                    + " ROUND(CAST("
                                                    + formatSqlIsNull(context,
                                                        "resources.pre_block,0")
                                                    + " AS real)/60,2),"
                                                    // Guo changed 2008-08-20 to solve KB3019197
                                                    + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                            : " 'A'")
                                                    + ","
                                                    + " reserve_rs.date_start,"
                                                    + formatSqlAddMinutesToExpression(
                                                        context,
                                                        "reserve_rs.time_start",
                                                        formatSqlIsNull(context,
                                                            "-(resources.pre_block) , 0"))
                                                    + ","
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " reserve.phone,"
                                                    + " reserve.dv_id,"
                                                    + " reserve.dp_id "
                                                    + " FROM reserve_rs, reserve, resources "
                                                    + " WHERE reserve_rs.res_id = reserve.res_id "
                                                    + " AND reserve_rs.resource_id = resources.resource_id "
                                                    + " AND reserve.res_id = "
                                                    + literal(context, res_id)
                                                    + " AND reserve_rs.rsres_id = "
                                                    + literal(context, event.getString("rsres_id"))
                                                    + ")"
                                                    + " WHERE wr_id = "
                                                    + literal(context, wrId1);
                                    
                                } else {
                                    
                                    sql =
                                            " UPDATE wr SET bl_id = reserve_rs.bl_id,"
                                                    + " fl_id = reserve_rs.fl_id,"
                                                    + " rm_id = reserve_rs.rm_id,"
                                                    + " time_stat_chg = "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ", "
                                                    + " date_stat_chg = "
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ", "
                                                    + " requestor = reserve.user_requested_by,"
                                                    + " est_labor_hours = ROUND(CAST("
                                                    + formatSqlIsNull(context,
                                                        "resources.pre_block,0")
                                                    + " AS real)/60,2),"
                                                    + " date_assigned = reserve_rs.date_start,"
                                                    + " time_assigned =  "
                                                    + formatSqlAddMinutesToExpression(
                                                        context,
                                                        "reserve_rs.time_start",
                                                        formatSqlIsNull(context,
                                                            "-(resources.pre_block) , 0"))
                                                    + ","
                                                    + " date_requested = "
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " time_requested = "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " phone = reserve.phone,"
                                                    + " dv_id = reserve.dv_id,"
                                                    + " dp_id = reserve.dp_id "
                                                    // Guo changed 2008-08-20 to solve KB3019197
                                                    + ", status = "
                                                    + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                            : " 'A'")
                                                    + " FROM reserve_rs"
                                                    + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id "
                                                    + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id "
                                                    + " WHERE reserve.res_id = "
                                                    + literal(context, res_id)
                                                    + " AND reserve_rs.rsres_id = "
                                                    + literal(context, event.getString("rsres_id"))
                                                    + " AND wr_id = " + literal(context, wrId1);
                                    
                                }
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not update work request for trade set-up: ",
                                    errMessage, e);
                            }
                            
                            try {
                                // Update [wrId2] for setup work request
                                
                                // PC changed to solve KB item 3019287
                                if (isOracle(context)) {
                                    
                                    sql =
                                            " UPDATE wr SET ("
                                                    + " time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                    + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                    + " time_requested,phone,dv_id,dp_id"
                                                    + " ) = ( SELECT "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " reserve_rs.bl_id,"
                                                    + " reserve_rs.fl_id,"
                                                    + " reserve_rs.rm_id,"
                                                    + " reserve.user_requested_by,"
                                                    + " ROUND(CAST("
                                                    + formatSqlIsNull(context,
                                                        "resources.post_block,0")
                                                    + " AS real)/60,2),"
                                                    // Guo changed 2008-08-20 to solve KB3019197
                                                    + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                            : " 'A'")
                                                    + ","
                                                    + " reserve_rs.date_start,reserve_rs.time_end,"
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " reserve.phone,"
                                                    + " reserve.dv_id,"
                                                    + " reserve.dp_id "
                                                    + " FROM reserve_rs, reserve, resources "
                                                    + " WHERE reserve_rs.res_id = reserve.res_id "
                                                    + " AND reserve_rs.resource_id = resources.resource_id "
                                                    + " AND reserve.res_id = "
                                                    + literal(context, res_id)
                                                    + " AND reserve_rs.rsres_id = "
                                                    + literal(context, event.getString("rsres_id"))
                                                    + ")"
                                                    + " WHERE wr_id = "
                                                    + literal(context, wrId2);
                                    
                                } else {
                                    
                                    sql =
                                            " UPDATE wr SET bl_id = reserve_rs.bl_id,"
                                                    + " fl_id = reserve_rs.fl_id, rm_id = reserve_rs.rm_id,"
                                                    + " time_stat_chg = "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ", "
                                                    + " date_stat_chg = "
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ", "
                                                    + " requestor = reserve.user_requested_by,"
                                                    + " est_labor_hours = ROUND(CAST("
                                                    + formatSqlIsNull(context,
                                                        "resources.post_block,0")
                                                    + " AS real)/60,2),"
                                                    + " date_assigned = reserve_rs.date_start,"
                                                    + " time_assigned = reserve_rs.time_end,"
                                                    + " date_requested = "
                                                    + formatSqlIsoToNativeDate(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " time_requested = "
                                                    + formatSqlIsoToNativeTime(context,
                                                        "CurrentDateTime")
                                                    + ","
                                                    + " phone = reserve.phone,"
                                                    + " dv_id = reserve.dv_id,"
                                                    + " dp_id = reserve.dp_id"
                                                    // Guo changed 2008-08-20 to solve KB3019197
                                                    + ", status = "
                                                    + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                            : " 'A'")
                                                    + " FROM reserve_rs"
                                                    + " LEFT OUTER JOIN reserve on reserve_rs.res_id = reserve.res_id "
                                                    + " LEFT OUTER JOIN resources on reserve_rs.resource_id = resources.resource_id "
                                                    + " WHERE reserve.res_id = "
                                                    + literal(context, res_id)
                                                    + " AND reserve_rs.rsres_id = "
                                                    + literal(context, event.getString("rsres_id"))
                                                    + " AND wr_id = " + literal(context, wrId2);
                                    
                                }
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not update work request for trade clean-up: ",
                                    errMessage, e);
                            }
                        } // End else
                    } // End if (tradeToCreate.size() > 0)
                    else {
                        if (!event.getString("rsres_id").equals("")) {
                            // Existing reservation
                            try {
                                // Cancel all possible existing work requests for trades for this
                                // reservation
                                sql =
                                        " UPDATE wr SET status='Can', date_stat_chg="
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + " time_stat_chg="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime") + " WHERE rsres_id = "
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id = " + literal(context, res_id)
                                                + " AND tr_id IS NOT NULL"
                                                + " AND status IN ('R','Rev','A','AA')";
                                
                                executeDbSql(context, sql, false);
                                
                                sql =
                                        " UPDATE wr SET status='S', date_stat_chg="
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + " time_stat_chg="
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + " WHERE rsres_id = "
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id = "
                                                + literal(context, res_id)
                                                + " AND tr_id IS NOT NULL AND status IN ('I','HP','HA','HL')";
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not cancel existing work request for trades: ",
                                    errMessage, e);
                            }
                        }
                    }
                    
                    if (vendorToCreate.size() > 0) {
                        vendorToCreateObject = (Object[]) vendorToCreate.get(0);
                        final String vn = vendorToCreateObject[0].toString();
                        
                        // Start with cancelling all existing work requests assigned to a different
                        // vendor
                        try {
                            sql =
                                    " UPDATE wr SET status='Can', date_stat_chg="
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + "," + " time_stat_chg="
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " WHERE rsres_id="
                                            + literal(context, event.getString("rsres_id"))
                                            + " AND res_id=" + literal(context, res_id)
                                            + " AND vn_id IS NOT NULL" + " AND vn_id <> "
                                            + literal(context, vn)
                                            + " AND status IN ('R','Rev','A','AA')";
                            
                            executeDbSql(context, sql, false);
                            
                            sql =
                                    " UPDATE wr SET status='S', date_stat_chg="
                                            + formatSqlIsoToNativeDate(context, "CurrentDateTime")
                                            + "," + " time_stat_chg="
                                            + formatSqlIsoToNativeTime(context, "CurrentDateTime")
                                            + " WHERE rsres_id="
                                            + literal(context, event.getString("rsres_id"))
                                            + " AND res_id=" + literal(context, res_id)
                                            + " AND vn_id IS NOT NULL" + " AND vn_id <> "
                                            + literal(context, vn)
                                            + " AND status IN ('I','HP','HA','HL')";
                            
                            executeDbSql(context, sql, false);
                        } catch (final Throwable e) {
                            handleError(
                                context,
                                ACTIVITY_ID
                                        + "-"
                                        + RULE_ID
                                        + ": Could not cancel existing work requests for different vendors: ",
                                errMessage, e);
                        }
                        
                        List wrIdList = new ArrayList();
                        
                        if (!event.getString("rsres_id").equals("")) {
                            // existing reservation
                            try {
                                // Get possible existing work requests for this vendor
                                sql =
                                        " SELECT wr_id FROM wr WHERE rsres_id = "
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id = " + literal(context, res_id)
                                                + " AND vn_id = " + literal(context, vn)
                                                + " AND status <> 'Can'" + " AND status <> 'S'";
                                
                                wrIdList = selectDbRecords(context, sql);
                            } catch (final Throwable e) {
                                handleError(
                                    context,
                                    ACTIVITY_ID
                                            + "-"
                                            + RULE_ID
                                            + ": Could not retrieve existing work requests for vendors: ",
                                    errMessage, e);
                            }
                        }
                        
                        if (event.getString("rsres_id").equals("") || wrIdList.size() == 0) {
                            try {
                                // Insert new wr record for setup by
                                sql =
                                        " INSERT INTO wr (res_id,rsres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status,"
                                                + " date_assigned,time_assigned,date_requested,time_requested,vn_id,phone,dv_id,dp_id,"
                                                + " description,date_stat_chg,time_stat_chg,prob_type) "
                                                + " SELECT reserve.res_id,reserve_rs.rsres_id,reserve_rs.bl_id,reserve_rs.fl_id,reserve_rs.rm_id,"
                                                + " reserve.user_requested_by,ROUND(CAST("
                                                + formatSqlIsNull(context, "resources.pre_block,0")
                                                + " AS real)/60,2),"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rs.date_start,"
                                                + formatSqlAddMinutesToExpression(
                                                    context,
                                                    "reserve_rs.time_start",
                                                    " ("
                                                            + mtu
                                                            + "*"
                                                            + formatSqlIsNull(context,
                                                                "-(resources.pre_block),0") + ")")
                                                + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + literal(context, vn)
                                                + ","
                                                + " reserve.phone,reserve.dv_id,reserve.dp_id,"
                                                // PC KB 3038222
                                                + " '"
                                                + setupdesc
                                                + ". "
                                                + reservationComments
                                                + " '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve.comments), '' ")
                                                + formatSqlConcat(context)
                                                + "'. '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve_rs.comments), '' ") + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ",'RES. SETUP'"
                                                + " FROM reserve_rs"
                                                + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id"
                                                + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id"
                                                + " WHERE Reserve.res_id = "
                                                + literal(context, res_id)
                                                + " AND reserve_rs.rsres_id = "
                                                + literal(context, event.getString("rsres_id"));
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not create work request for vendor set-up: ",
                                    errMessage, e);
                            }
                            
                            try {
                                // Insert new wr record for cleaning by
                                sql =
                                        " INSERT INTO wr (res_id,rsres_id,bl_id,fl_id,rm_id,requestor,est_labor_hours,status,"
                                                + " date_assigned,time_assigned,date_requested,time_requested,vn_id,phone,dv_id,dp_id,"
                                                + " description, date_stat_chg, time_stat_chg,prob_type) "
                                                + " SELECT reserve.res_id,reserve_rs.rsres_id,reserve_rs.bl_id,reserve_rs.fl_id,reserve_rs.rm_id,"
                                                + " reserve.user_requested_by,ROUND(CAST("
                                                + formatSqlIsNull(context, "resources.pre_block,0")
                                                + " AS real)/60,2),"
                                                // Guo changed 2008-08-20 to solve KB3019197
                                                + ("Awaiting App.".equals(statusOfReservation) ? " 'R'"
                                                        : " 'A'")
                                                + ",reserve_rs.date_start,reserve_rs.time_end,"
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + literal(context, vn)
                                                + ",reserve.phone, reserve.dv_id,reserve.dp_id,"
                                                // PC KB 3038222
                                                + " '"
                                                + cleanupdesc
                                                + ". "
                                                + reservationComments
                                                + " '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve.comments), '' ")
                                                + formatSqlConcat(context)
                                                + "'. '"
                                                + formatSqlConcat(context)
                                                + formatSqlIsNull(context, "RTRIM(reserve_rs.comments), '' ") + ","
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime")
                                                + ",'RES. CLEANUP'"
                                                + " FROM reserve_rs"
                                                + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id"
                                                + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id"
                                                + " WHERE Reserve.res_id = "
                                                + literal(context, res_id)
                                                + " AND reserve_rs.rsres_id = "
                                                + literal(context, event.getString("rsres_id"));
                                
                                executeDbSql(context, sql, false);
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not create work request for vendor clean-up: ",
                                    errMessage, e);
                            }
                        } // End if (event.getString("rsres_id").equals("") || wrIdList.size() ==
                          // 0)
                        else {
                            if (wrIdList.size() > 1) {
                                final Object[] wrObject = (Object[]) wrIdList.get(0);
                                final Object[] wrObject1 = (Object[]) wrIdList.get(1);
                                
                                if (wrObject.length >= 1 && wrObject1.length >= 1) {
                                    wrId1 = wrObject[0].toString();
                                    wrId2 = wrObject1[0].toString();
                                    
                                    try {
                                        // Update [wrId1] for setup work request
                                        
                                        // PC changed to solve KB item 3019287
                                        if (isOracle(context)) {
                                            
                                            sql =
                                                    " UPDATE wr SET ("
                                                            + " time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                            + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                            + " time_requested,phone,dv_id,dp_id"
                                                            + " ) = ( SELECT "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " reserve_rs.bl_id,"
                                                            + " reserve_rs.fl_id,"
                                                            + " reserve_rs.rm_id,"
                                                            + " reserve.user_requested_by,"
                                                            + " ROUND(CAST("
                                                            + formatSqlIsNull(context,
                                                                "resources.pre_block,0")
                                                            + " AS real)/60,2),"
                                                            // Guo changed 2008-08-20 to solve
                                                            // KB3019197
                                                            + ("Awaiting App."
                                                                .equals(statusOfReservation) ? " 'R'"
                                                                    : " 'A'")
                                                            + ","
                                                            + " reserve_rs.date_start,"
                                                            + formatSqlAddMinutesToExpression(
                                                                context,
                                                                "reserve_rs.time_start",
                                                                formatSqlIsNull(context,
                                                                    "-(resources.pre_block) , 0"))
                                                            + ","
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " reserve.phone,"
                                                            + " reserve.dv_id,"
                                                            + " reserve.dp_id "
                                                            + " FROM reserve_rs, reserve, resources "
                                                            + " WHERE reserve_rs.res_id = reserve.res_id "
                                                            + " AND reserve_rs.resource_id = resources.resource_id "
                                                            + " AND reserve.res_id = "
                                                            + literal(context, res_id)
                                                            + " AND reserve_rs.rsres_id = "
                                                            + literal(context,
                                                                event.getString("rsres_id"))
                                                            + ")"
                                                            + " WHERE wr_id = "
                                                            + literal(context, wrId1);
                                            
                                        } else {
                                            sql =
                                                    " UPDATE wr SET bl_id = reserve_rs.bl_id,"
                                                            + " fl_id = reserve_rs.fl_id,"
                                                            + " rm_id = reserve_rs.rm_id,"
                                                            + " time_stat_chg = "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ", "
                                                            + " date_stat_chg = "
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ", "
                                                            + " requestor = reserve.user_requested_by,"
                                                            + " est_labor_hours = ROUND(CAST("
                                                            + formatSqlIsNull(context,
                                                                "resources.pre_block,0")
                                                            + " AS real)/60,2),"
                                                            + " date_assigned = reserve_rs.date_start,"
                                                            + " time_assigned = "
                                                            + formatSqlAddMinutesToExpression(
                                                                context,
                                                                "reserve_rs.time_start",
                                                                formatSqlIsNull(context,
                                                                    "-(resources.pre_block) , 0"))
                                                            + ", date_requested = "
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ", time_requested = "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ", phone = reserve.phone"
                                                            + ", dv_id = reserve.dv_id"
                                                            + ", dp_id = reserve.dp_id"
                                                            // Guo changed 2008-08-20 to solve
                                                            // KB3019197
                                                            + ", status = "
                                                            + ("Awaiting App."
                                                                .equals(statusOfReservation) ? " 'R'"
                                                                    : " 'A'")
                                                            + " FROM reserve_rs"
                                                            + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id "
                                                            + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id "
                                                            + " WHERE reserve.res_id = "
                                                            + literal(context, res_id)
                                                            + " AND reserve_rs.rsres_id = "
                                                            + literal(context,
                                                                event.getString("rsres_id"))
                                                            + " AND wr_id = "
                                                            + literal(context, wrId1);
                                        }
                                        executeDbSql(context, sql, false);
                                        
                                    } catch (final Throwable e) {
                                        handleError(
                                            context,
                                            ACTIVITY_ID
                                                    + "-"
                                                    + RULE_ID
                                                    + ": Could not update work request for vendor set-up: ",
                                            errMessage, e);
                                    }
                                    
                                    try {
                                        // Update [wrId2] for setup work request
                                        
                                        // PC changed to solve KB item 3019287
                                        if (isOracle(context)) {
                                            
                                            sql =
                                                    " UPDATE wr SET ("
                                                            + " time_stat_chg,date_stat_chg,bl_id,fl_id,rm_id,"
                                                            + " requestor,est_labor_hours,status,date_assigned,time_assigned,date_requested,"
                                                            + " time_requested,phone,dv_id,dp_id"
                                                            + " ) = ( SELECT "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " reserve_rs.bl_id,"
                                                            + " reserve_rs.fl_id,"
                                                            + " reserve_rs.rm_id,"
                                                            + " reserve.user_requested_by,"
                                                            + " ROUND(CAST("
                                                            + formatSqlIsNull(context,
                                                                "resources.post_block,0")
                                                            + " AS real)/60,2),"
                                                            // Guo changed 2008-08-20 to solve
                                                            // KB3019197
                                                            + ("Awaiting App."
                                                                .equals(statusOfReservation) ? " 'R'"
                                                                    : " 'A'")
                                                            + ","
                                                            + " reserve_rs.date_start,reserve_rs.time_end,"
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " reserve.phone,"
                                                            + " reserve.dv_id,"
                                                            + " reserve.dp_id "
                                                            + " FROM reserve_rs, reserve, resources "
                                                            + " WHERE reserve_rs.res_id = reserve.res_id "
                                                            + " AND reserve_rs.resource_id = resources.resource_id "
                                                            + " AND reserve.res_id = "
                                                            + literal(context, res_id)
                                                            + " AND reserve_rs.rsres_id = "
                                                            + literal(context,
                                                                event.getString("rsres_id"))
                                                            + ")"
                                                            + " WHERE wr_id = "
                                                            + literal(context, wrId2);
                                            
                                        } else {
                                            
                                            sql =
                                                    " UPDATE wr SET bl_id = reserve_rs.bl_id,"
                                                            + " fl_id = reserve_rs.fl_id,"
                                                            + " rm_id = reserve_rs.rm_id,"
                                                            + " time_stat_chg = "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ", "
                                                            + " date_stat_chg = "
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ", "
                                                            + " requestor = reserve.user_requested_by,"
                                                            + " est_labor_hours = ROUND(CAST("
                                                            + formatSqlIsNull(context,
                                                                "resources.post_block,0")
                                                            + " AS real)/60,2),"
                                                            + " date_assigned = reserve_rs.date_start,"
                                                            + " time_assigned = reserve_rs.time_end,"
                                                            + " date_requested = "
                                                            + formatSqlIsoToNativeDate(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " time_requested = "
                                                            + formatSqlIsoToNativeTime(context,
                                                                "CurrentDateTime")
                                                            + ","
                                                            + " phone = reserve.phone,"
                                                            + " dv_id = reserve.dv_id,"
                                                            + " dp_id = reserve.dp_id "
                                                            // Guo changed 2008-08-20 to solve
                                                            // KB3019197
                                                            + ", status = "
                                                            + ("Awaiting App."
                                                                .equals(statusOfReservation) ? " 'R'"
                                                                    : " 'A'")
                                                            + " FROM reserve_rs"
                                                            + " LEFT OUTER JOIN reserve ON reserve_rs.res_id = reserve.res_id"
                                                            + " LEFT OUTER JOIN resources ON reserve_rs.resource_id = resources.resource_id"
                                                            + " WHERE reserve.res_id = "
                                                            + literal(context, res_id)
                                                            + " AND reserve_rs.rsres_id = "
                                                            + literal(context,
                                                                event.getString("rsres_id"))
                                                            + " AND wr_id = "
                                                            + literal(context, wrId2);
                                            
                                        }
                                        executeDbSql(context, sql, false);
                                    } catch (final Throwable e) {
                                        handleError(
                                            context,
                                            ACTIVITY_ID
                                                    + "-"
                                                    + RULE_ID
                                                    + ": Could not update work request for vendor clean-up: ",
                                            errMessage, e);
                                    }
                                }
                            } // End if (wrIdList.size() > 1)
                        } // End else
                    } // End if (vendorToCreate.size() > 0)
                    else {
                        if (!event.getString("rsres_id").equals("")) {
                            // Existing reservation
                            // Cancel all possible existing work requests for vendors for this
                            // reservation
                            try {
                                sql =
                                        " UPDATE wr SET status = 'Can', date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime") + " WHERE rsres_id = "
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id = " + literal(context, res_id)
                                                + " AND vn_id IS NOT NULL"
                                                + " AND status IN ('R','Rev','A','AA')";
                                
                                executeDbSql(context, sql, false);
                                
                                sql =
                                        " UPDATE wr SET status = 'S', date_stat_chg = "
                                                + formatSqlIsoToNativeDate(context,
                                                    "CurrentDateTime")
                                                + ","
                                                + " time_stat_chg = "
                                                + formatSqlIsoToNativeTime(context,
                                                    "CurrentDateTime") + " WHERE rsres_id = "
                                                + literal(context, event.getString("rsres_id"))
                                                + " AND res_id = " + literal(context, res_id)
                                                + " AND vn_id IS NOT NULL"
                                                + " AND status IN ('I','HP','HA','HL')";
                                
                                executeDbSql(context, sql, false);
                                
                            } catch (final Throwable e) {
                                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                                        + ": Could not cancel existing work request for vendors: ",
                                    errMessage, e);
                            }
                        }
                    } // End else
                    
                    // kb#3036675: Commented below lines to use a consistent way to process email
                    // notification error
                    // JSONObject results = new JSONObject();
                    // results.put("messageEmail", (context.parameterExists("errorMessage")) ?
                    // context
                    // .getParameter("errorMessage") : "");
                    // context.addResponseParameter("jsonExpression", results.toString());
                }
                allOk = true;
                
            } // End for
        }// End for(Iterator it =listResId.iterator();it.hasNext();)
         // kb#3036675: Commented below lines to use a consistent way to process email notification
         // error
         // JSONObject results = new JSONObject();
         // results.put("messageEmail", (context.parameterExists("errorMessage")) ? context
         // .getParameter("errorMessage") : "");
         // context.addResponseParameter("jsonExpression", results.toString());
        
        if (allOk) {
            // Guo changed 2008-09-12 to remove all executeDbCommit(context)
            // executeDbCommit(context);
        }
    }

}
