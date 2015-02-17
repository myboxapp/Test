package com.archibus.eventhandler.reservations;

import java.io.StringReader;
import java.sql.Time;
import java.text.*;
import java.util.*;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.json.*;

import com.archibus.context.ContextStore;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.*;

/**
 * Contains common event handlers used in both Rooms and Resources reservation WFRs.
 * 
 */
public class ReservationsCommonHandler extends ReservationsEventHandlerBase {
    
    // ----------------------- workflow rule implementation methods --------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN notifyRequestedBy wfr
    // ---------------------------------------------------------------------------------------------
    /**
     * This rule gets the identifier of a created, modified, cancelled or rejected reservation and
     * notifies the �requested by?user of this event. Inputs: res_id res_id (String); Outputs:
     * message error message in necesary case
     * 
     * @param context Event handler context.
     */
    public void notifyRequestedBy(final EventHandlerContext context) {
        this.notifyRequestedStd(context, "by");
    }
    
    // ---------------------------------------------------------------------------------------------
    // END notifyRequestedBy wfr
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN notifyRequestedFor wfr
    // ---------------------------------------------------------------------------------------------
    /**
     * This rule gets the identifier of a created, modified, cancelled or rejected reservation and
     * notifies the �requested for?user of this event. Inputs: res_id res_id (String); Outputs:
     * message error message in necesary case
     * 
     * @param context Event handler context.
     */
    public void notifyRequestedFor(final EventHandlerContext context) {
        this.notifyRequestedStd(context, "for");
    }
    
    // ---------------------------------------------------------------------------------------------
    // END notifyRequestedFor wfr
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN notifyRequestedStd
    // ---------------------------------------------------------------------------------------------
    /**
     * This rule gets the identifier of a created, modified, cancelled or rejected reservation and
     * notifies the �requested by o for?user of this event. Inputs: context context
     * (EventHandlerContext); resId resID (String); parentId parentId (String); Std Std (String) It
     * can be "by" or "for" Outputs: message error message in necesary case
     * 
     * @param context Event handler context.
     */
    public void notifyRequestedStd(final EventHandlerContext context, final String Std) {
        
        final String RULE_ID = "notifyRequested" + Std;
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        // Get input parameters
        final String resId = (String) context.getParameter("res_id");
        String parentId = "0";
        // If exists a res_parent parameter in context. the wfr inicialize this parameter.
        if (context.parameterExists("res_parent")) {
            parentId = (String) context.getParameter("res_parent");
        }
        // canceling message
        String cancelingMessage = null;
        if (context.parameterExists("cancel_message")) {
            cancelingMessage = (String) context.getParameter("cancel_message");
        }
        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [res_id]: "+resId+" [res_parent]:
        // "+parentId+" ");
        String sql = "";
        boolean allOk = true;
        boolean allQueriesOk = true;
        final TreeMap valuesToMail = new TreeMap();
        final ArrayList listResources = new ArrayList();
        final ArrayList listRoom = new ArrayList();
        final boolean isRegular = parentId.equals("0");
        final boolean isRecurring = (!parentId.equals("0"));
        boolean existsRoom = false;
        boolean existsResource = false;
        boolean isRoomCancelled = false;
        boolean isRoomRejected = false;
        
        
        // notification rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "NOTIFYREQUESTED" + Std.toUpperCase()
                        + "_WFR", "NOTIFYREQUESTED" + Std.toUpperCase() + "ERROR", null);
        
        try {
            
            // BEGIN: Only enter if exists a resId.
            if (!resId.equals("")) {
                // Guo added 2008-08-22 to solve KB3019268
                if ("for".equals(Std)) {
                    sql = "SELECT user_requested_by, user_requested_for FROM reserve ";
                    
                    if (!resId.equals("0")) {
                        sql += " WHERE reserve.res_id= " + resId;
                    } else {
                        sql += " WHERE reserve.res_parent = " + parentId;
                    }
                    
                    final List records = retrieveDbRecords(context, sql);
                    
                    if (!records.isEmpty()) {
                        final Map record = (Map) records.get(0);
                        final String userRequestBy = getString(record, "user_requested_by");
                        final String userRequestFor = getString(record, "user_requested_for");
                        if (userRequestBy.equals(userRequestFor)) {
                            return;
                        }
                    }
                }
                // BEGIN: Get reservation info and the user to notify
                sql =
                        "SELECT em.email as requested" + Std
                                + "mail, reserve.res_id, reserve.user_requested_" + Std
                                + ", reserve.comments " + " FROM reserve, em ";
                // if resId is not zero makes the query for a single a reserve,
                // but in another case makes it for a group of reserves with a common father.
                if (!resId.equals("0")) {
                    sql += " WHERE reserve.res_id= " + resId;
                } else {
                    sql += " WHERE reserve.res_parent = " + parentId;
                }
                sql += " AND reserve.user_requested_" + Std + " = em.em_id ";
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                
                final List recordsSql1 = retrieveDbRecords(context, sql);
                
                if (!recordsSql1.isEmpty()) {
                    final Map recordOfSql1 = (Map) recordsSql1.get(0);
                    valuesToMail.put("reserve.requested" + Std + "mail",
                        getString(recordOfSql1, "requested" + Std + "mail"));
                    valuesToMail.put("reserve.res_id", getString(recordOfSql1, "res_id"));
                    valuesToMail.put("reserve.user_requested_" + Std,
                        getString(recordOfSql1, "user_requested_" + Std));
                    
                    valuesToMail.put("reserve.comments", getString(recordOfSql1, "comments"));
                    
                    // Search the locale of the user to notify
                    sql =
                            " SELECT locale "
                                    + " FROM afm_users "
                                    + " WHERE email="
                                    + literal(
                                        context,
                                        (String) valuesToMail.get("reserve.requested" + Std
                                                + "mail"));
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                    
                    final List recordsSql2 = retrieveDbRecords(context, sql);
                    
                    // If the locale is found
                    if (!recordsSql2.isEmpty()) {
                        final Map recordOfSql2 = (Map) recordsSql2.get(0);
                        valuesToMail.put("locale", getString(recordOfSql2, "locale"));
                        allQueriesOk = true;
                    } else {
                        allQueriesOk = false;
                    }
                }
                
                // If we can't find the locale of the user to notify, search the locale of the
                // creator of the reservation
                if (!allQueriesOk) {
                    
                    // BEGIN: Get reservation info and the creator to search for its locale
                    sql =
                            " SELECT em.email as createdbymail, " + " reserve.user_requested_"
                                    + Std + ", " + " reserve.comments " + " FROM reserve, em ";
                    // if resId is not zero makes the query for a single a reserve,
                    // but in another case makes it for a group of reserves with a common father.
                    if (!resId.equals("0")) {
                        sql += " WHERE reserve.res_id= " + resId;
                    } else {
                        sql += " WHERE reserve.res_parent = " + parentId;
                    }
                    sql += " AND reserve.user_created_by = em.em_id ";
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                    
                    final List recordsSql3 = retrieveDbRecords(context, sql);
                    
                    if (!recordsSql3.isEmpty()) {
                        final Map recordOfSql3 = (Map) recordsSql3.get(0);
                        valuesToMail.put("reserve.createdbymail",
                            getString(recordOfSql3, "createdbymail"));
                        valuesToMail.put("reserve.user_requested_" + Std,
                            getString(recordOfSql3, "user_requested_" + Std));
                        valuesToMail.put("reserve.comments", getString(recordOfSql3, "comments"));
                        
                        // Search the locale of the creator
                        sql =
                                " SELECT locale "
                                        + " FROM afm_users "
                                        + " WHERE email="
                                        + literal(context, getString(recordOfSql3, "createdbymail"));
                        // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                        
                        final List recordsSql4 = retrieveDbRecords(context, sql);
                        
                        // If the locale is found
                        if (!recordsSql4.isEmpty()) {
                            final Map recordOfSql4 = (Map) recordsSql4.get(0);
                            valuesToMail.put("locale", getString(recordOfSql4, "locale"));
                            allQueriesOk = true;
                        } else {
                            allQueriesOk = false;
                        }
                    }
                }
                
                // In case that we haven't found the locale to use in the email, we'll take the
                // default locale of the connected user
                if (StringUtil.isNullOrEmpty(valuesToMail.get("locale"))) {
                    valuesToMail.put("locale", ContextStore.get().getUser().getLocale());
                }
                
                // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [data to mail]:
                // "+valuesToMail.get("createdbymail")+" "+valuesToMail.get("locale"));
                
                allQueriesOk = false;
                
                // Get the room reservation information to notify
                sql =
                        " SELECT date_start, time_start, time_end, status, bl_id, fl_id, rm_id, config_id, "
                                + " rm_arrange_type_id, guests_internal, guests_external, comments "
                                + " FROM reserve_rm ";
                // if resId is not zero makes the query for a single a reserve,
                // but in another case makes it for a group of reserves with a common father.
                if (!resId.equals("0")) {
                    sql += " WHERE res_id= " + resId;
                } else {
                    sql +=
                            " WHERE EXISTS (SELECT 1 FROM reserve WHERE reserve.res_id=reserve_rm.res_id AND res_parent = "
                                    + parentId + " ) ";
                }
                sql += " ORDER BY date_start ";
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                
                final List listRoomReserve = retrieveDbRecords(context, sql);
                
                // if exists room reserve
                if (!listRoomReserve.isEmpty()) {
                    existsRoom = true;
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [there are reserve!!] ");
                    // For each room reservation, get the information to notify
                    for (final Iterator it = listRoomReserve.iterator(); it.hasNext();) {
                        final Map recordOfSql5 = (Map) it.next();
                        final TreeMap roomToMail = new TreeMap();
                        roomToMail.put("reserve_rm.guests_internal",
                            getString(recordOfSql5, "guests_internal"));
                        roomToMail.put("reserve_rm.guests_external",
                            getString(recordOfSql5, "guests_external"));
                        roomToMail.put("reserve_rm.comments", getString(recordOfSql5, "comments"));
                        roomToMail.put("reserve_rm.date_start",
                            getDateValue(context, recordOfSql5.get("date_start")).toString());
                        roomToMail.put("reserve_rm.time_start",
                            getTimeValue(context, recordOfSql5.get("time_start")).toString());
                        roomToMail.put("reserve_rm.time_end",
                            getTimeValue(context, recordOfSql5.get("time_end")).toString());
                        roomToMail.put(
                            "reserve_rm.status",
                            getEnumFieldDisplayedValue(context, "reserve_rm", "status",
                                getString(recordOfSql5, "status")));
                        
                        if(getString(recordOfSql5,"status").equals("Cancelled")) {
                            isRoomCancelled = true;
                        }  
                        if(getString(recordOfSql5,"status").equals("Rejected")) {
                            isRoomRejected = true;
                        }                        
                                                
                        roomToMail.put("reserve_rm.bl_id", getString(recordOfSql5, "bl_id"));
                        roomToMail.put("reserve_rm.fl_id", getString(recordOfSql5, "fl_id"));
                        roomToMail.put("reserve_rm.rm_id", getString(recordOfSql5, "rm_id"));
                        roomToMail
                            .put("reserve_rm.config_id", getString(recordOfSql5, "config_id"));
                        roomToMail.put("reserve_rm.rm_arrange_type_id",
                            getString(recordOfSql5, "rm_arrange_type_id"));
                        listRoom.add(roomToMail);
                        if (!valuesToMail.containsKey("reserve_rm.guests_internal")) {
                            valuesToMail.putAll(roomToMail);
                        }
                    }
                    allQueriesOk = true;
                }
                
                // Get the resources reservations information to notify
                sql =
                        " SELECT date_start,time_start,time_end,status,resource_id,quantity,bl_id,fl_id,rm_id,comments "
                                + " FROM reserve_rs ";
                // if resId is not zero makes the query for a single a reserve,
                // but in another case makes it for a group of reserves with a common father.
                if (!resId.equals("0")) {
                    sql += " WHERE res_id= " + resId;
                } else {
                    sql += " WHERE res_id= " + parentId;
                }
                sql += " ORDER BY date_start ";
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                
                final List listResource = retrieveDbRecords(context, sql);
                if (!listResource.isEmpty()) {
                    existsResource = true;
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [there are resources!!] ");
                    // For each resource reservation, get the information to notify
                    for (final Iterator it = listResource.iterator(); it.hasNext();) {
                        final Map recordOfSql6 = (Map) it.next();
                        final TreeMap resourcesToMail = new TreeMap();
                        resourcesToMail.put("reserve_rs.comments",
                            getString(recordOfSql6, "comments"));
                        resourcesToMail.put("reserve_rs.date_start",
                            getDateValue(context, recordOfSql6.get("date_start")).toString());
                        resourcesToMail.put("reserve_rs.time_start",
                            getTimeValue(context, recordOfSql6.get("time_start")).toString());
                        resourcesToMail.put("reserve_rs.time_end",
                            getTimeValue(context, recordOfSql6.get("time_end")).toString());
                        resourcesToMail.put(
                            "reserve_rs.status",
                            getEnumFieldDisplayedValue(context, "reserve_rs", "status",
                                getString(recordOfSql6, "status")));
                        
                        resourcesToMail.put("reserve_rs.resource_id",
                            getString(recordOfSql6, "resource_id"));
                        resourcesToMail.put("reserve_rs.quantity",
                            getString(recordOfSql6, "quantity"));
                        resourcesToMail.put("reserve_rs.bl_id", getString(recordOfSql6, "bl_id"));
                        resourcesToMail.put("reserve_rs.fl_id", getString(recordOfSql6, "fl_id"));
                        resourcesToMail.put("reserve_rs.rm_id", getString(recordOfSql6, "rm_id"));
                        listResources.add(resourcesToMail);
                    }
                    allQueriesOk = true;
                }
                
                // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [allOk]: "+allQueriesOk);
                
                if (allQueriesOk) {
                    // Get all mesages to Mail in a TreeMap (It is more easy to write)
                    final TreeMap messages =
                            this.getMailMessages(context, Std, (String) valuesToMail.get("locale"));
                    
                    // BEGIN: create message email
                    String subject = "";
                    String message = "";
                    
                    // BEGIN: subject
                    if (isRecurring) {
                        if ((existsRoom) || (existsRoom && existsResource)) {
                            subject =
                                    messages.get("SUBJECT1") + " "
                                            + (parentId.equals("0") ? resId : parentId) + ", "
                                            + messages.get("SUBJECT2") + " "
                                            + valuesToMail.get("reserve_rm.date_start") + " "
                                            + messages.get("SUBJECT4");
                        }
                        if ((!existsRoom) && (existsResource)) {
                            final Iterator res_it = listResources.iterator();
                            final TreeMap resources_map = (TreeMap) res_it.next();
                            final String res_date_start =
                                    (String) resources_map.get("reserve_rs.date_start");
                            subject =
                                    messages.get("SUBJECT1") + " "
                                            + (parentId.equals("0") ? resId : parentId) + ", "
                                            + messages.get("SUBJECT2") + " " + res_date_start + " "
                                            + messages.get("SUBJECT4");
                        }
                    }
                    
                    if (isRegular) {
                        if ((existsRoom) || (existsRoom && existsResource)) {
                            subject =
                                    messages.get("SUBJECT1") + " "
                                            + (parentId.equals("0") ? resId : parentId) + ", "
                                            + messages.get("SUBJECT2") + " "
                                            + valuesToMail.get("reserve_rm.date_start");
                        }
                        if ((!existsRoom) && (existsResource)) {
                            final Iterator res_it = listResources.iterator();
                            final TreeMap resources_map = (TreeMap) res_it.next();
                            final String res_date_start =
                                    (String) resources_map.get("reserve_rs.date_start");
                            subject =
                                    messages.get("SUBJECT1") + " "
                                            + (parentId.equals("0") ? resId : parentId) + ", "
                                            + messages.get("SUBJECT2") + " " + res_date_start;
                        }
                    }
                    // END: subject
                    
                    // BEGIN: message
                    message +=
                            messages.get("BODY1") + " "
                                    + valuesToMail.get("reserve.user_requested_" + Std) + ",\n\n";
                    if(isRoomCancelled) {
                        message +=
                                messages.get("BODY_PART2_CANCEL") + " " + (parentId.equals("0") ? resId : parentId)
                                        + "\n\n";
                    } else if (isRoomRejected) {
                        message +=
                                messages.get("BODY_PART2_REJECT") + " " + (parentId.equals("0") ? resId : parentId)
                                        + "\n\n";
                    } else {
                        message +=
                                messages.get("BODY2") + " " + (parentId.equals("0") ? resId : parentId)
                                        + "\n\n";
                    }
                    
                    
                    if (isRegular) {
                        if ((existsRoom) || (existsRoom && existsResource)) {
                            message +=
                                    messages.get("BODY3") + " "
                                            + valuesToMail.get("reserve_rm.bl_id") + " - "
                                            + valuesToMail.get("reserve_rm.fl_id") + " - "
                                            + valuesToMail.get("reserve_rm.rm_id") + " - "
                                            + valuesToMail.get("reserve_rm.config_id") + " - "
                                            + valuesToMail.get("reserve_rm.rm_arrange_type_id")
                                            + "\n\n";
                            message +=
                                    messages.get("BODY4") + " "
                                            + valuesToMail.get("reserve_rm.time_start") + "\n\n";
                            message +=
                                    messages.get("BODY5") + " "
                                            + valuesToMail.get("reserve_rm.time_end") + "\n\n";
                            
                            final int guests_int =
                                    valuesToMail.containsKey("reserve_rm.guests_internal") ? Integer
                                        .parseInt((String) valuesToMail
                                            .get("reserve_rm.guests_internal")) : 0;
                            final int guests_ext =
                                    valuesToMail.containsKey("reserve_rm.guests_external") ? Integer
                                        .parseInt((String) valuesToMail
                                            .get("reserve_rm.guests_external")) : 0;
                            
                            message +=
                                    messages.get("BODY6") + " " + (guests_int + guests_ext)
                                            + "\n\n";
                            message +=
                                    messages.get("BODY7") + " "
                                            + valuesToMail.get("reserve_rm.status") + "\n\n";
                            message +=
                                    messages.get("BODY11") + " "
                                            + valuesToMail.get("reserve_rm.comments") + "\n\n";
                        }
                    }
                    
                    // BEGIN: list of room
                    if (isRecurring) {
                        if ((existsRoom) || (existsRoom && existsResource)) {
                            message += messages.get("BODY9") + "\n\n";
                            String rm_comments = "";
                            for (final Iterator it = listRoom.iterator(); it.hasNext();) {
                                final TreeMap rooms = (TreeMap) it.next();
                                final int guests_int =
                                        rooms.containsKey("reserve_rm.guests_internal") ? Integer
                                            .parseInt((String) rooms
                                                .get("reserve_rm.guests_internal")) : 0;
                                final int guests_ext =
                                        rooms.containsKey("reserve_rm.guests_external") ? Integer
                                            .parseInt((String) rooms
                                                .get("reserve_rm.guests_external")) : 0;
                                message +=
                                        rooms.get("reserve_rm.date_start") + " - "
                                                + rooms.get("reserve_rm.bl_id") + " - "
                                                + rooms.get("reserve_rm.fl_id") + " - "
                                                + rooms.get("reserve_rm.rm_id") + " - "
                                                + rooms.get("reserve_rm.config_id") + " - "
                                                + rooms.get("reserve_rm.rm_arrange_type_id")
                                                + " - " + rooms.get("reserve_rm.time_start")
                                                + " - " + rooms.get("reserve_rm.time_end") + " - "
                                                + (guests_int + guests_ext) + " - "
                                                + rooms.get("reserve_rm.status") + "\n";
                                rm_comments = (String) valuesToMail.get("reserve_rm.comments");
                            }
                            message += "\n" + messages.get("BODY11") + " " + rm_comments + "\n\n";
                        }
                    }
                    // END: list of room
                    
                    // BEGIN: list of resources with room
                    if (isRegular) {
                        if (existsRoom && existsResource) {
                            message += messages.get("BODY10") + "\n\n";
                            String rs_comments = "";
                            for (final Iterator it = listResources.iterator(); it.hasNext();) {
                                final TreeMap resources = (TreeMap) it.next();
                                message +=
                                        resources.get("reserve_rs.resource_id") + " - "
                                                + resources.get("reserve_rs.quantity") + " - "
                                                + resources.get("reserve_rs.status") + " - "
                                                + resources.get("reserve_rs.time_start") + " - "
                                                + resources.get("reserve_rs.time_end") + "\n";
                                rs_comments = (String) resources.get("reserve_rs.comments");
                                message += messages.get("BODY11_2") + " " + rs_comments + "\n\n";
                            }
                        }
                    }
                    // END: list of resources with room
                    
                    // BEGIN: list of resources without room
                    if (isRegular) {
                        if (!listResources.isEmpty() && listRoomReserve.isEmpty()) {
                            message += messages.get("BODY10") + "\n\n";
                            String rs_comments = "";
                            for (final Iterator it = listResources.iterator(); it.hasNext();) {
                                final TreeMap resources = (TreeMap) it.next();
                                message +=
                                        resources.get("reserve_rs.resource_id") + " - "
                                                + resources.get("reserve_rs.quantity") + " - "
                                                + resources.get("reserve_rs.status") + " - "
                                                + resources.get("reserve_rs.time_start") + " - "
                                                + resources.get("reserve_rs.time_end") + " - "
                                                + resources.get("reserve_rs.bl_id") + " - "
                                                + resources.get("reserve_rs.fl_id") + " - "
                                                + resources.get("reserve_rs.rm_id") + "\n";
                                rs_comments = (String) resources.get("reserve_rs.comments");
                                message += messages.get("BODY11_2") + " " + rs_comments + "\n\n";
                            }
                        }
                    }
                    // END: list of resources without room
                    
                    // BEGIN: list of resources when is recurring
                    if (isRecurring) {
                        if (!listResources.isEmpty()) {
                            message += messages.get("BODY10") + "\n\n";
                            String rs_comments = "";
                            for (final Iterator it = listResources.iterator(); it.hasNext();) {
                                final TreeMap resources = (TreeMap) it.next();
                                message +=
                                        resources.get("reserve_rs.resource_id") + " - "
                                                + resources.get("reserve_rs.quantity") + " - "
                                                + resources.get("reserve_rs.time_start") + " - "
                                                + resources.get("reserve_rs.time_end") + " - "
                                                + resources.get("reserve_rs.status") + " - "
                                                + resources.get("reserve_rs.bl_id") + " - "
                                                + resources.get("reserve_rs.fl_id") + " - "
                                                + resources.get("reserve_rs.rm_id") + "\n";
                                rs_comments = getString(resources, "reserve_rs.comments");
                                message += messages.get("BODY11_2") + " " + rs_comments + "\n\n";
                            }
                        }
                    }
                    // END: list of resources when is recurring
                    
                    message += messages.get("BODY12") + "\n\n";
                    message += getWebCentralPath(context);
                    message +=
                            "/schema/ab-system/html/url-proxy.htm?viewName=ab-rr-reservations-details-grid.axvw&fieldName=";
                    if (isRegular) {
                        message += "reserve.res_id&fieldValue=" + resId;
                    } else {
                        message += "reserve.res_parent&fieldValue=" + parentId;
                    }
                    message += "\n\n";
                    message +=
                            messages.get("BODY8") + " " + valuesToMail.get("reserve.comments")
                                    + "\n";
                    // add canceling message
                    if (cancelingMessage == null) {
                        message += "\n";
                    } else {
                        message += "\t" + cancelingMessage + "\n\n";
                    }
                    message += messages.get("BODY13") + "\n\n";
                    message +=
                            getActivityParameterString(context, ACTIVITY_ID, "InternalServicesName");
                    
                    // END: message
                    
                    // END: Create message email
                    
                    // Get email, from and host
                    final String from =
                            getActivityParameterString(context, ACTIVITY_ID,
                                "InternalServicesEmail");
                    final String host = getEmailHost(context);
                    // PC changed to solve KB 3016618
                    final String port = getEmailPort(context);
                    final String userId = getEmailUserId(context);
                    final String password = getEmailPassword(context);
                    
                    // Send the email
                    try {
                        // PC changed to solve KB 3016618
                        this.sendEmail(message, from, host, port, subject,
                            (String) valuesToMail.get("reserve.requested" + Std + "mail"), null,
                            null, userId, password, null, CONTENT_TYPE_TEXT_UFT8, ACTIVITY_ID);
                    } catch (final Throwable e) {
                        handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID
                                + ": SENDEMAIL ERROR", errMessage, e,
                            (String) valuesToMail.get("reserve.requested" + Std + "mail"));
                        allOk = false;
                    }
                }
            }
            // END: Only enter if exists a resId.
        } catch (final Throwable e) {
            handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                errMessage, e, "");
            allOk = false;
        }
        
        if (!allOk) {
            context.addResponseParameter("message", errMessage);
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END notifyRequestedStd
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN saveRoomOverride
    // ---------------------------------------------------------------------------------------------
    /**
     * Updates existing record using specified table name and field values.
     */
    public void saveRoomOverride(final String record) {
        final String RULE_ID = "saveRoomOverride";
        
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        // get input parameter containing <record> XML string
        final String recordXmlString = record;
        
        // this.log.info("Input parameter: "+recordXmlString);
        String bl_id = "";
        String fl_id = "";
        String rm_id = "";
        String rm_name = "";
        Integer reservable = new Integer(0);
        
        String sql = "";
        
        // saveRoomOverride rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "SAVEROOMOVERRIDE_WFR",
                    "SAVEROOMOVERRIDEERROR", null);
        
        try {
            if (!recordXmlString.equals("")) {
                // parse XML string into DOM document
                final Document recordXmlDoc =
                        new SAXReader().read(new StringReader(recordXmlString));
                // parse record XML into a Map
                final Map recordValues = parseRecord(context, recordXmlDoc.getRootElement());
                // obtain room primary key values and the reservable field to update
                bl_id = (String) recordValues.get("rm.bl_id");
                fl_id = (String) recordValues.get("rm.fl_id");
                rm_id = (String) recordValues.get("rm.rm_id");
                // If the room hasn't a name, we assign to the configuration name=id
                rm_name =
                        (((String) recordValues.get("rm.name") != "") ? ((String) recordValues
                            .get("rm.name")) : rm_id);
                reservable = (Integer) recordValues.get("rm.reservable");
            }
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed parse XML", errMessage, e);
        }
        
        if ((!bl_id.equals("")) && (!fl_id.equals("")) && (!rm_id.equals(""))) {
            sql =
                    " UPDATE rm SET " + " reservable = " + reservable + " WHERE bl_id = "
                            + literal(context, bl_id) + " AND" + " fl_id = "
                            + literal(context, fl_id) + " AND" + " rm_id = "
                            + literal(context, rm_id);
            
            // this.log.info("SQL 1: "+sql);
            try {
                executeDbSql(context, sql, false);
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                    errMessage, e);
            }
            
            if ((reservable.intValue()) == 1) {
                // Check that one configuration for the room doesn't exist first
                sql =
                        " SELECT 1 FROM rm_config WHERE " + " bl_id = " + literal(context, bl_id)
                                + " AND" + " fl_id = " + literal(context, fl_id) + " AND"
                                + " rm_id = " + literal(context, rm_id);
                
                // this.log.info("SQL 2: "+ sql);
                try {
                    final List recordsConfig = retrieveDbRecords(context, sql);
                    
                    // If not exists, create the configuration
                    if (recordsConfig.isEmpty()) {
                        sql =
                                " INSERT INTO rm_config (bl_id,fl_id,rm_id,config_id,config_name) "
                                        + " VALUES ( " + literal(context, bl_id) + ","
                                        + literal(context, fl_id) + "," + literal(context, rm_id)
                                        + "," + literal(context, rm_id) + ","
                                        + literal(context, rm_name) + " )";
                        // this.log.info("SQL 3: "+sql);
                        
                        try {
                            executeDbSql(context, sql, false);
                        } catch (final Throwable e) {
                            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: "
                                    + sql, errMessage, e);
                        }
                    }
                } catch (final Throwable e) {
                    handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                        errMessage, e);
                }
            }
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END saveRoomOverride
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN saveArrangementFixedResource wfr
    // ---------------------------------------------------------------------------------------------
    /**
     * save the defined fixed resource to all the room arrangements First, update if exists the
     * records of arrangements of this rooms. Second, insert if don't exist the records for the
     * arrangements of this rooms.
     * 
     * Kb# 3015539 Added by Keven
     * 
     * @param context Event handler context.
     */
    public void saveArrangementFixedResource(final String rmResourceStd) {
        final String RULE_ID = "saveArrangementFixedResource";
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        // Get the selected arrangement from the input parameter
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        final String jsonExpression = rmResourceStd;
        boolean allOk = false;
        
        // saveArrangementFixedResources rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "SAVEARRANGEMENTFIXEDRESOURCE_WFR",
                    "SAVEARRANGEMENTFIXEDRESOURCEERROR", null);
        
        try {
            final JSONObject roomResourceStandard = new JSONObject("" + jsonExpression + ")");
            String sql = "";
            sql =
                    " UPDATE rm_resource_std " + " SET resource_std="
                            + literal(context, roomResourceStandard.getString("resource_std"))
                            + ", " + "eq_id="
                            + literal(context, roomResourceStandard.getString("eq_id")) + ", "
                            + "description="
                            + literal(context, roomResourceStandard.getString("description"))
                            + " WHERE bl_id="
                            + literal(context, roomResourceStandard.getString("bl_id"))
                            + " AND fl_id="
                            + literal(context, roomResourceStandard.getString("fl_id"))
                            + " AND rm_id="
                            + literal(context, roomResourceStandard.getString("rm_id"))
                            + " AND config_id="
                            + literal(context, roomResourceStandard.getString("config_id"))
                            + " AND fixed_resource_id= "
                            + literal(context, roomResourceStandard.getString("fixed_resource_id"));
            
            try {
                executeDbSql(context, sql, false);
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                        + ": Failed rm_resource_std table: " + sql, errMessage, e);
            }
            
            sql =
                    " INSERT INTO rm_resource_std (rm_arrange_type_id, bl_id, fl_id, rm_id, config_id, fixed_resource_id, resource_std, eq_id, description) "
                            + " SELECT a.rm_arrange_type_id AS rm_arrange_type_id, "
                            + literal(context, roomResourceStandard.getString("bl_id"))
                            + " AS bl_id, "
                            + literal(context, roomResourceStandard.getString("fl_id"))
                            + " AS fl_id, "
                            + literal(context, roomResourceStandard.getString("rm_id"))
                            + " AS rm_id, "
                            + literal(context, roomResourceStandard.getString("config_id"))
                            + " AS config_id, "
                            + literal(context, roomResourceStandard.getString("fixed_resource_id"))
                            + " AS fixed_resource_id, "
                            + literal(context, roomResourceStandard.getString("resource_std"))
                            + " AS resource_std,  "
                            + literal(context, roomResourceStandard.getString("eq_id"))
                            + " AS eq_id, "
                            + literal(context, roomResourceStandard.getString("description"))
                            + " AS description "
                            + " FROM rm_arrange a "
                            + " WHERE a.bl_id="
                            + literal(context, roomResourceStandard.getString("bl_id"))
                            + " AND a.fl_id="
                            + literal(context, roomResourceStandard.getString("fl_id"))
                            + " AND a.rm_id="
                            + literal(context, roomResourceStandard.getString("rm_id"))
                            + " AND a.config_id="
                            + literal(context, roomResourceStandard.getString("config_id"))
                            + " AND NOT EXISTS"
                            + " ( SELECT 1 FROM rm_resource_std b "
                            + "   WHERE b.bl_id="
                            + literal(context, roomResourceStandard.getString("bl_id"))
                            + " AND b.fl_id="
                            + literal(context, roomResourceStandard.getString("fl_id"))
                            + " AND b.rm_id="
                            + literal(context, roomResourceStandard.getString("rm_id"))
                            + " AND b.config_id="
                            + literal(context, roomResourceStandard.getString("config_id"))
                            + " AND b.fixed_resource_id="
                            + literal(context, roomResourceStandard.getString("fixed_resource_id"))
                            + " AND b.rm_arrange_type_id=a.rm_arrange_type_id )";
            try {
                executeDbSql(context, sql, false);
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                        + ": Failed rm_resource_std table: " + sql, errMessage, e);
            }
            allOk = true;
            
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Global Exception", errMessage, e);
        }
        
        if (!allOk) {
            context.addResponseParameter("message", errMessage);
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END saveArrangementFixedResource wfr
    // ---------------------------------------------------------------------------------------------
            
    // ---------------------------------------------------------------------------------------------
    // BEGIN closeReservations wfr
    // ---------------------------------------------------------------------------------------------
    /**
     * The data from tables reserve, reserve_rm, reserve_rs and wr will be moved to the historical
     * data tables hreserve, hreserve_rm, hreserve_rs and hwr if the current date is X days after
     * the meeting date. Inputs: context context (EventHandlerContext); Outputs:
     * 
     * @param context Event handler context.
     */
    public void closeReservations(final EventHandlerContext context) {
        
        final String RULE_ID = "closeReservations";
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        
        boolean allOk = false;
        String sql = "";
        
        // closeReservations rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "CLOSERESERVATIONS_WFR",
                    "ARCHIVERESERVATIONSERROR", null);
        
        try {
            
            final String currentDate = formatSqlIsoToNativeDate(context, "CurrentDateTime");
            
            // First of all the system must get the DaysBeforeArchiving value to take
            // into account in the process from the reservation parameters table
            sql =
                    " SELECT param_value " + " FROM afm_activity_params " + " WHERE activity_id='"
                            + ACTIVITY_ID + "' " + " AND param_id='DaysBeforeArchiving'";
            
            // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Select-1]: " + sql);
            
            final List listParamValue = retrieveDbRecords(context, sql);
            
            if (!listParamValue.isEmpty()) {
                final Map recordOfSql1 = (Map) listParamValue.get(0);
                final int daysBeforeArchiving =
                        getIntegerValue(context, recordOfSql1.get("param_value")).intValue();
                
                // BEGIN: Move to HRESERVE_RM historical table
                // Insert the room reservations that meet the criteria into the historical table
                sql =
                        "INSERT INTO hreserve_rm (res_id, rmres_id, date_start, time_start, time_end, "
                                + "cost_rmres, user_last_modified_by, date_last_modified, date_created, "
                                + "date_cancelled, date_rejected, bl_id, fl_id, rm_id, config_id, "
                                + "rm_arrange_type_id, recurring_order, comments, status, guests_internal,guests_external)"
                                + " SELECT res_id, rmres_id, date_start, time_start, time_end, cost_rmres,"
                                + " user_last_modified_by, date_last_modified, date_created, date_cancelled,"
                                + " date_rejected, bl_id, fl_id, rm_id, config_id, rm_arrange_type_id,"
                                + " recurring_order, comments, status, guests_internal, guests_external "
                                + " FROM reserve_rm "
                                + " WHERE "
                                + currentDate
                                + " >= "
                                + formatSqlAddDaysToExpression(context, "date_start",
                                    Integer.toString(daysBeforeArchiving));
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Insert-2]: " + sql);
                
                try {
                    
                    executeDbSql(context, sql, false);
                    setArchiveStatus(context, errMessage, RULE_ID, "hreserve_rm");
                    
                    // Remove the inserted reservations into the historical table from the original
                    // table
                    sql =
                            " DELETE FROM reserve_rm "
                                    + " WHERE "
                                    + currentDate
                                    + " >= "
                                    + formatSqlAddDaysToExpression(context, "date_start",
                                        Integer.toString(daysBeforeArchiving));
                    
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Delete-2]: " + sql);
                    
                    try {
                        executeDbSql(context, sql, false);
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                            errMessage, e);
                    }
                } catch (final Throwable e) {
                    handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                        errMessage, e);
                }
                // END: Move to HRESERVE_RM historical table
                
                // BEGIN: Move to HRESERVE_RS historical table
                
                // Insert the resource reservations that meet the criteria into the historical table
                sql =
                        " INSERT INTO hreserve_rs (res_id, rsres_id, date_start, time_start, time_end,"
                                + " cost_rsres, user_last_modified_by, date_last_modified, date_created,"
                                + " date_cancelled, date_rejected, bl_id, fl_id, rm_id, resource_id,"
                                + " quantity, recurring_order, comments, status)"
                                + " SELECT res_id, rsres_id, date_start, time_start, time_end, cost_rsres,"
                                + " user_last_modified_by, date_last_modified, date_created, date_cancelled,"
                                + " date_rejected, bl_id, fl_id, rm_id, resource_id, quantity,"
                                + " recurring_order, comments, status "
                                + " FROM reserve_rs "
                                + " WHERE "
                                + currentDate
                                + " >= "
                                + formatSqlAddDaysToExpression(context, "date_start",
                                    Integer.toString(daysBeforeArchiving));
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Insert-3]: " + sql);
                
                try {
                    
                    executeDbSql(context, sql, false);
                    setArchiveStatus(context, errMessage, RULE_ID, "hreserve_rs");
                    
                    // Remove the inserted resources into the historical table from the original
                    // table
                    sql =
                            " DELETE FROM reserve_rs "
                                    + " WHERE "
                                    + currentDate
                                    + " >= "
                                    + formatSqlAddDaysToExpression(context, "date_start",
                                        Integer.toString(daysBeforeArchiving));
                    
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Delete-3]: " + sql);
                    
                    try {
                        executeDbSql(context, sql, false);
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                            errMessage, e);
                    }
                } catch (final Throwable e) {
                    handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                        errMessage, e);
                }
                
                // END: Move to HRESERVE_RS historical table
                
                // BEGIN: Move to HRESERVE historical table
                
                // Insert the reservations that meet the criteria into the historical table
                sql =
                        " INSERT INTO hreserve (res_id, user_created_by, user_requested_by, user_requested_for,"
                                + " user_last_modified_by, cost_res, date_created, date_last_modified, date_cancelled,"
                                + " dv_id, dp_id, ac_id, phone, email, reservation_name, comments, date_start, date_end,"
                                + " time_start, time_end, contact, doc_event,"
                                + " recurring_rule, status) "
                                + " SELECT res_id, user_created_by, user_requested_by, user_requested_for,"
                                + " user_last_modified_by, cost_res, date_created, date_last_modified, date_cancelled,"
                                + " dv_id, dp_id, ac_id, phone, email, reservation_name, comments, date_start, date_end,"
                                + " time_start, time_end, contact, doc_event, recurring_rule,"
                                + " status "
                                + " FROM reserve "
                                + " WHERE "
                                + currentDate
                                + " >= "
                                + formatSqlAddDaysToExpression(context, "date_start",
                                    Integer.toString(daysBeforeArchiving));
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Insert-4]: " + sql);
                
                try {
                    
                    executeDbSql(context, sql, false);
                    setArchiveStatus(context, errMessage, RULE_ID, "hreserve");
                    
                    // Remove the inserted reservations into the historical table from the original
                    // table
                    sql =
                            " DELETE FROM reserve "
                                    + " WHERE "
                                    + currentDate
                                    + " >= "
                                    + formatSqlAddDaysToExpression(context, "date_start",
                                        Integer.toString(daysBeforeArchiving));
                    
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+"[Delete-4]: " + sql);
                    
                    try {
                        executeDbSql(context, sql, false);
                        allOk = true;
                    } catch (final Throwable e) {
                        handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                            errMessage, e);
                    }
                } catch (final Throwable e) {
                    handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                        errMessage, e);
                }
                // END: Move to HRESERVE historical table
                
            }
            
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Global Exception ", errMessage, e);
        }
        
        if (!allOk) {
            context.addResponseParameter("message", errMessage);
        }
        
    }
    
    /**
     * Set the status value of all Awaiting App. and Confirmed records in the given table to Closed.
     * KB#3030979
     * 
     * @param context event handler context
     * @param errMessage error message to use when it failed
     * @param RULE_ID current WFR rule identifier
     * @param tableName table to update
     */
    private void setArchiveStatus(EventHandlerContext context, String errMessage,
            String RULE_ID, String tableName) {
        String sql =
                " UPDATE " + tableName + " SET status = 'Closed' "
                        + " WHERE status = 'Awaiting App.' OR status = 'Confirmed'";
        try {
            executeDbSql(context, sql, false);
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql, errMessage,
                e);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // END closeReservations wfr
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN - sendEmailInvitations
    // ---------------------------------------------------------------------------------------------
    /**
     * This function creates the email result of changes in reservations and attaches the
     * corresponding ics files depending on the type of reservation transition
     */
    public void sendEmailInvitations(final EventHandlerContext context) {
        
        final String RULE_ID = "sendEmailInvitations";
        // this.log.info("Executing '"+ACTIVITY_ID+"-"+RULE_ID+"' .... ");
        
        // Get input parameters
        final String resId = (String) context.getParameter("res_id");
        String parentId = "0";
        final String invitation_type = (String) context.getParameter("invitation_type");
        String email_invitations = (String) context.getParameter("email_invitations");
        // If invitation_type="cancel" && date_cancel=null, if recurring, cancel all the instances
        // If invitation_type="cancel" && date_cancel<>null, if recurring, cancel only date_cancel
        String date_cancel = null;
        if (context.parameterExists("date_cancel")) {
            date_cancel = (String) context.getParameter("date_cancel");
        }       
        // canceling message
        String cancelingMessage = null;
        if (context.parameterExists("cancel_message")) {
            cancelingMessage = (String) context.getParameter("cancel_message");
        }
        
        // If updating a reservation from a recurring set, and date_start changed, we need the
        // information on original date, times and city to cancel it, and later create the new one
        String original_date = null;
        if (context.parameterExists("original_date")) {
            original_date = (String) context.getParameter("original_date");
        }
        
        String original_time_start = "";
        String original_time_end = "";
        String original_cityTimezone = "";
        if (context.parameterExists("original_time_start")) {
            original_time_start =
                    transformDate((String) context.getParameter("original_time_start"));
            original_time_end = transformDate((String) context.getParameter("original_time_end"));
            original_cityTimezone = (String) context.getParameter("original_cityTimezone");
        }
        
        Boolean require_reply = null;
        if (context.parameterExists("require_reply")) {
            require_reply = (Boolean) context.getParameter("require_reply");
        }
        
        // invitations rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                    "SENDEMAILINVITATIONSERROR", null);
        
        // If exists a reservation parameter in context. the wfr inicialize this parameter.
        JSONObject jsReservation = null;
        if (context.parameterExists("Reservation")
                && (!context.getString("Reservation").equals(""))) {
            try {
                jsReservation = new JSONObject(context.getString("Reservation"));
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed parsing Reservation: "
                        + e.getMessage(), errMessage, e);
            }
        }
        
        // If exists a roomConflicts parameter in context. the wfr inicialize this parameter.
        JSONArray jsRoomConflicts = null;
        if (context.parameterExists("RoomConflicts")
                && (!context.getString("RoomConflicts").equals(""))) {
            try {
                jsRoomConflicts = new JSONArray("" + context.getString("RoomConflicts") + ")");
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID
                        + ": Failed parsing RoomConflicts: " + e.getMessage(), errMessage, e);
            }
        }
        
        // If exists a res_parent parameter in context. the wfr inicialize this parameter.
        if (context.parameterExists("res_parent")) {
            parentId = (String) context.getParameter("res_parent");
        }
        // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [res_id]: "+resId+" [res_parent]:
        // "+parentId+" ");
        
        String sql = "";
        boolean allQueriesOk = true;
        final TreeMap valuesToMail = new TreeMap();
        
        try {
            
            if (!resId.equals("")) {
                
                // BEGIN: Get reservation info and the user to notify
                sql =
                        " SELECT em.email as requestedbymail,reserve.user_requested_by,"
                                + " reserve.comments,reserve.reservation_name "
                                + " FROM reserve, em ";
                if (!resId.equals("0")) {
                    sql += " WHERE reserve.res_id= " + resId;
                } else {
                    sql += " WHERE reserve.res_parent = " + parentId;
                    // PC 3018035 If we want to cancel only one date in the recurrent
                    // reservation get the information on just this reservation and not all
                    if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                            && (date_cancel != null) && (!date_cancel.equals(""))) {
                        sql +=
                                " AND date_start = "
                                        + formatSqlIsoToNativeDate(context, date_cancel);
                    }
                }
                sql += " AND reserve.user_requested_by = em.em_id ";
                sql += " ORDER BY date_start ASC";
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                
                final List recordsSql1 = retrieveDbRecords(context, sql);
                
                if (!recordsSql1.isEmpty()) {
                    
                    final Map recordOfSql1 = (Map) recordsSql1.get(0);
                    valuesToMail.put("reserve.requestedbymail",
                        getString(recordOfSql1, "requestedbymail"));
                    valuesToMail.put("reserve.user_requested_by",
                        getString(recordOfSql1, "user_requested_by"));
                    valuesToMail.put("reserve.comments", getString(recordOfSql1, "comments"));
                    valuesToMail.put("reserve.reservation_name",
                        getString(recordOfSql1, "reservation_name"));
                    
                    // kb#3035551: add different ics file for requested by email, so don't need
                    // below code
                    /*
                     * email_invitations = email_invitations + ";" + getString(recordOfSql1,
                     * "requestedbymail");
                     */
                    
                    // Search the locale of the user to notify
                    sql =
                            " SELECT locale "
                                    + " FROM afm_users "
                                    + " WHERE email="
                                    + literal(context,
                                        (String) valuesToMail.get("reserve.requestedbymail"));
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                    
                    final List recordsSql2 = retrieveDbRecords(context, sql);
                    
                    // If the locale is found
                    if (!recordsSql2.isEmpty()) {
                        final Map recordOfSql2 = (Map) recordsSql2.get(0);
                        valuesToMail.put("locale", getString(recordOfSql2, "locale"));
                        allQueriesOk = true;
                    } else {
                        allQueriesOk = false;
                    }
                }
                
                // If we can't find the locale of the user to notify, search the locale of the
                // creator of the reservation
                if (!allQueriesOk) {
                    
                    // BEGIN: Get reservation info and the creator to search for its locale
                    sql =
                            " SELECT em.email as createdbymail, " + " reserve.user_requested_by, "
                                    + " reserve.comments " + " FROM reserve, em ";
                    if (!resId.equals("0")) {
                        sql += " WHERE reserve.res_id= " + resId;
                    } else {
                        sql += " WHERE reserve.res_parent = " + parentId;
                        // PC 3018035 If we want to cancel only one date in the recurrent
                        // reservation get the information on just this reservation and not all
                        if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                                && (date_cancel != null) && (!date_cancel.equals(""))) {
                            sql +=
                                    " AND date_start = "
                                            + formatSqlIsoToNativeDate(context, date_cancel);
                        }
                    }
                    sql += " AND reserve.user_created_by = em.em_id ";
                    sql += " ORDER BY date_start ASC";
                    
                    // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                    
                    final List recordsSql3 = retrieveDbRecords(context, sql);
                    
                    if (!recordsSql3.isEmpty()) {
                        
                        final Map recordOfSql3 = (Map) recordsSql3.get(0);
                        valuesToMail.put("reserve.user_requested_by",
                            getString(recordOfSql3, "user_requested_by"));
                        valuesToMail.put("reserve.comments", getString(recordOfSql3, "comments"));
                        
                        // kb#3035551: add different ics file for requested by email, so don't need
                        // below code
                        /*
                         * email_invitations = email_invitations + ";" + getString(recordOfSql3,
                         * "createdbymail");
                         */
                        
                        // Search the locale of the creator
                        sql =
                                " SELECT locale FROM afm_users "
                                        + " WHERE email="
                                        + literal(context, getString(recordOfSql3, "createdbymail"));
                        // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                        
                        final List recordsSql4 = retrieveDbRecords(context, sql);
                        
                        // If the locale is found
                        if (!recordsSql4.isEmpty()) {
                            final Map recordOfSql4 = (Map) recordsSql4.get(0);
                            valuesToMail.put("locale", getString(recordOfSql4, "locale"));
                            allQueriesOk = true;
                        } else {
                            allQueriesOk = false;
                        }
                    }
                    
                    // In case that we haven't found the locale to use in the email, we'll take the
                    // default locale of the connected user
                    if (StringUtil.isNullOrEmpty(valuesToMail.get("locale"))) {
                        valuesToMail.put("locale", ContextStore.get().getUser().getLocale());
                    }
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [data to mail]:
                    // "+valuesToMail.get("createdbymail")+" "+valuesToMail.get("locale"));
                    
                    allQueriesOk = false;
                }
                
                // PC KB 3021259: Get the email of the requested for to include him in the list of
                // attendees
                
                sql =
                        " SELECT em.email as requestedformail, reserve.user_requested_for"
                                + " FROM reserve, em ";
                if (!resId.equals("0")) {
                    sql += " WHERE reserve.res_id= " + resId;
                } else {
                    sql += " WHERE reserve.res_parent = " + parentId;
                    // PC 3018035 If we want to cancel only one date in the recurrent
                    // reservation get the information on just this reservation and not all
                    if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                            && (date_cancel != null) && (!date_cancel.equals(""))) {
                        sql +=
                                " AND date_start = "
                                        + formatSqlIsoToNativeDate(context, date_cancel);
                    }
                }
                sql += " AND reserve.user_requested_for = em.em_id ";
                sql += " ORDER BY date_start ASC";
                
                final List recordsSql5 = retrieveDbRecords(context, sql);
                
                if (!recordsSql5.isEmpty()) {
                    final Map recordOfSql5 = (Map) recordsSql5.get(0);
                    
                    // kb#3035551: add different ics file for requested by email
                    if (!getString(recordOfSql5, "requestedformail").equals(
                        valuesToMail.get("reserve.requestedbymail"))) {
                        email_invitations =
                                email_invitations + ";"
                                        + getString(recordOfSql5, "requestedformail");
                    }
                }
                // END PC KB 3021259
                
                final ArrayList listRoom = new ArrayList();
                
                // Get the room reservation information to notify
                // PC KB 3018035 - Get also the city's timezone information
                sql =
                        " SELECT date_start, time_start, time_end, status, reserve_rm.bl_id, fl_id, rm_id, "
                                + " reserve_rm.comments, rmres_id, city.timezone_id"
                                + " FROM reserve_rm, bl LEFT OUTER JOIN city "
                                + " ON city.city_id=bl.city_id AND city.state_id=bl.state_id";
                if (!resId.equals("0")) {
                    sql += " WHERE res_id=" + resId;
                } else {
                    sql +=
                            " WHERE EXISTS (SELECT 1 FROM reserve WHERE reserve.res_id=reserve_rm.res_id AND res_parent="
                                    + parentId + ")";
                    // PC 3018035 If we want to cancel only one date in the recurrent
                    // reservation get the information on just this reservation and not all
                    if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                            && (date_cancel != null) && (!date_cancel.equals(""))) {
                        sql +=
                                " AND date_start = "
                                        + formatSqlIsoToNativeDate(context, date_cancel);
                    }
                    
                }
                sql += " AND reserve_rm.bl_id=bl.bl_id";
                sql += " ORDER BY date_start ASC";
                
                // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                
                final List listRoomReserve = retrieveDbRecords(context, sql);
                
                // PC KB 3018035 - Get also the city timezone and the date to check the GMT offset
                String cityTimezone = "";
                Date dateCheckTimezone = null;
                
                if (!listRoomReserve.isEmpty()) {
                    // For each room reservation, get the information to notify
                    for (final Iterator it = listRoomReserve.iterator(); it.hasNext();) {
                        final Map recordOfSql5 = (Map) it.next();
                        final TreeMap roomToMail = new TreeMap();
                        roomToMail.put("reserve_rm.comments", getString(recordOfSql5, "comments"));
                        roomToMail.put("reserve_rm.date_start",
                            getDateValue(context, recordOfSql5.get("date_start")).toString());
                        roomToMail.put("reserve_rm.time_start",
                            getTimeValue(context, recordOfSql5.get("time_start")).toString());
                        roomToMail.put("reserve_rm.time_end",
                            getTimeValue(context, recordOfSql5.get("time_end")).toString());
                        roomToMail.put(
                            "reserve_rm.status",
                            getEnumFieldDisplayedValue(context, "reserve_rm", "status",
                                getString(recordOfSql5, "status")));
                        roomToMail.put("reserve_rm.bl_id", getString(recordOfSql5, "bl_id"));
                        roomToMail.put("reserve_rm.fl_id", getString(recordOfSql5, "fl_id"));
                        roomToMail.put("reserve_rm.rm_id", getString(recordOfSql5, "rm_id"));
                        roomToMail.put("reserve_rm.rmres_id", getString(recordOfSql5, "rmres_id"));
                        roomToMail.put("reserve_rm.tz_id", getString(recordOfSql5, "timezone_id"));
                        listRoom.add(roomToMail);
                        if (!valuesToMail.containsKey("reserve_rm.comments")) {
                            valuesToMail.putAll(roomToMail);
                        }
                        // PC KB 3018035 - Get also the city timezone and the date to check the GMT
                        // offset
                        if (cityTimezone.equals("")) {
                            cityTimezone = getString(recordOfSql5, "timezone_id");
                        }
                        if (dateCheckTimezone == null) {
                            dateCheckTimezone =
                                    getDateValue(context, recordOfSql5.get("date_start"));
                        }
                    }
                }
                
                // BEGIN: prepare messages
                final TreeMap messages =
                        getSendMailMessages(context, (String) valuesToMail.get("locale"));
                // END: prepare messages
                
                // BEGIN: create message email
                String subject = "";
                String body = "";
                String to = "";
                
                // TIMEZONE PC 3018035 - Instead of server's timezone take city's one when possible
                TimeZone tz;
                
                if (!cityTimezone.equals("")) {
                    tz = TimeZone.getTimeZone(cityTimezone);
                } else {
                    tz = TimeZone.getDefault();
                }
                
                final int minutesoffset = -(tz.getOffset(dateCheckTimezone.getTime()) / 60000);
                
                final DecimalFormat timeZoneFormatter = new DecimalFormat("00");
                String TimeZoneStr =
                        (minutesoffset / 60) < 0 ? "+"
                                + timeZoneFormatter.format(-minutesoffset / 60) : timeZoneFormatter
                            .format(-minutesoffset / 60);
                final int absOffset = (minutesoffset > 0 ? minutesoffset : -minutesoffset);
                TimeZoneStr +=
                        (absOffset % 60) > 0 ? ":" + timeZoneFormatter.format(absOffset % 60) : "";
                
                // BEGIN: subject
                // Include the reservation name if it's not null
                if (!valuesToMail.get("reserve.reservation_name").equals("")) {
                    subject += valuesToMail.get("reserve.reservation_name") + " - ";
                }
                subject += messages.get("SUBJECT1") + " ";
                // Include date and time start
                // While updating a date of one of the recurrent reservations, the subject
                // must show the old date and time and a 'Changed reservation' description
                if ((invitation_type.equals("update"))
                        && ((original_date != null) && (!original_date.equals("")))) {
                    subject += original_date + " " + original_time_start;
                } else {
                    subject +=
                            valuesToMail.get("reserve_rm.date_start") + " "
                                    + valuesToMail.get("reserve_rm.time_start");
                }
                if (invitation_type.equals("new")) {
                    // Indicate that's a recurrent reservation
                    if (!parentId.equals("0")) {
                        subject += " - " + messages.get("SUBJECT4");
                        // Indicate that's an updated reservation
                    }
                } else if (invitation_type.equals("update")) {
                    // Indicate that's a recurrent reservation
                    if (!parentId.equals("0")) {
                        subject += " - " + messages.get("SUBJECT4");
                    }
                    subject += " - " + messages.get("SUBJECT2");
                    // Indicate that's a cancelled reservation
                } else if (invitation_type.equals("cancel")) {
                    // Indicate that is cancelling the full recurrence set
                    if ((!parentId.equals("0"))
                            && ((date_cancel == null || date_cancel.equals("")))) {
                        subject += " - " + messages.get("SUBJECT4");
                    }
                    subject += " - " + messages.get("SUBJECT3");
                }
                // END: subject
                
                // BEGIN: body
                if ((invitation_type.equals("new")) || (invitation_type.equals("update"))) {
                    // Indicate that's a recurrent reservation
                    if ((invitation_type.equals("new")) && (!parentId.equals("0"))) {
                        body +=
                                messages.get("BODY1_3") + " "
                                        + valuesToMail.get("reserve.user_requested_by") + "\n\n";
                    } else {
                        body +=
                                messages.get("BODY1") + " "
                                        + valuesToMail.get("reserve.user_requested_by") + "\n\n";
                    }
                } else if (invitation_type.equals("cancel")) {
                    body +=
                            messages.get("BODY1_2") + " "
                                    + valuesToMail.get("reserve.user_requested_by") + "\n\n";
                }
                // PC - If creating recurring reservation or cancelling all dates of a
                // recurrence set show the full list of dates in email, otherwise just one
                if (((invitation_type.equals("new")) && (!parentId.equals("0")))
                        || (invitation_type.equals("cancel") && !parentId.equals("0") && (date_cancel == null || date_cancel
                            .equals("")))) {
                    // Create recurring
                    if (invitation_type.equals("new")) {
                        body += messages.get("BODY2_2") + "\n\n";
                    }
                    // Cancelling all the recurrent reservation dates
                    else {
                        body += messages.get("BODY2_3") + "\n\n";
                    }
                    // PC 3018035 Get the different timezone for all the reserved dates
                    TimeZone tz2;
                    Date dateCheckTimezone2 = null;
                    int minutesoffset2 = 0;
                    String TimeZoneStr2 = "";
                    int absOffset2 = 0;
                    
                    for (final Iterator it = listRoom.iterator(); it.hasNext();) {
                        final TreeMap rooms = (TreeMap) it.next();
                        body +=
                                "* " + messages.get("BODY2") + " "
                                        + rooms.get("reserve_rm.date_start") + " ";
                        if (!(rooms.get("reserve_rm.tz_id").equals(""))) {
                            tz2 = TimeZone.getTimeZone(rooms.get("reserve_rm.tz_id").toString());
                        } else {
                            tz2 = TimeZone.getDefault();
                        }
                        // PC 3018035 - Each date can have a different GMT offset, indicate
                        // it in email's content (even when .ics files can only have one offset)
                        final String date_room = (String) rooms.get("reserve_rm.date_start");
                        dateCheckTimezone2 = getDateValue(context, date_room);
                        minutesoffset2 = -(tz2.getOffset(dateCheckTimezone2.getTime()) / 60000);
                        TimeZoneStr2 =
                                (minutesoffset2 / 60) < 0 ? "+"
                                        + timeZoneFormatter.format(-minutesoffset2 / 60)
                                        : timeZoneFormatter.format(-minutesoffset2 / 60);
                        absOffset2 = (minutesoffset2 > 0 ? minutesoffset2 : -minutesoffset2);
                        TimeZoneStr2 +=
                                (absOffset2 % 60) > 0 ? ":"
                                        + timeZoneFormatter.format(absOffset2 % 60) : "";
                        
                        body +=
                                messages.get("BODY3") + " " + rooms.get("reserve_rm.time_start")
                                        + " GMT" + TimeZoneStr2 + " ";
                        body +=
                                messages.get("BODY4") + " " + rooms.get("reserve_rm.time_end")
                                        + " GMT" + TimeZoneStr2 + "\n";
                    }
                } else {
                    body +=
                            messages.get("BODY2") + " " + valuesToMail.get("reserve_rm.date_start")
                                    + "\n";
                    body +=
                            messages.get("BODY3") + " " + valuesToMail.get("reserve_rm.time_start")
                                    + " GMT" + TimeZoneStr + "\n";
                    body +=
                            messages.get("BODY4") + " " + valuesToMail.get("reserve_rm.time_end")
                                    + " GMT" + TimeZoneStr + "\n";
                }
                body +=
                        "\n" + messages.get("BODY5") + " "
                                + (String) valuesToMail.get("reserve_rm.bl_id") + "-";
                body +=
                        valuesToMail.get("reserve_rm.fl_id") + "-"
                                + valuesToMail.get("reserve_rm.rm_id") + "\n";
                body += messages.get("BODY6") + "\n";
                body += "\t" + valuesToMail.get("reserve.comments") + "\n";
                // add canceling message
                if (cancelingMessage != null) {
                    body += "\t" + cancelingMessage + "\n";
                }
                body += messages.get("BODY6_2") + "\n";
                body += "\t" + valuesToMail.get("reserve_rm.comments") + "\n\n";
                body += messages.get("BODY7") + "\n";
                // PC 3018035 If it's recurrent reservation add a note to be careful on timezones
                if (!parentId.equals("0")) {
                    body += "\n" + messages.get("BODY8") + "\n";
                }
                // END: body
                
                // this.log.info(body);
                
                // BEGIN: to
                to = email_invitations.replaceAll(";", ",");
                
                // END Create message mail
                
                // BEGIN: ics files creation
                final String path =
                        getParentContextAttributeXPath(context, "/*/preferences/@webAppPath")
                                + java.io.File.separator
                                + getParentContextAttributeXPath(context,
                                    "/*/preferences/@schemaDirectory") + java.io.File.separator
                                + "per-site" + java.io.File.separator + "temp";
                
                FileUtil.createFoldersIfNot(path);
                
                // this.log.info("ICS FILES PATH: " + path);
                
                // attachments creation: variables initialization
                String filename = "";
                
                // kb#3035551: add different ics file for requested by email
                String filename_requestedBy = "";
                String absoluteFilename_requestedBy = "";
                final ArrayList attachments_requestedBy = new ArrayList();
                final String attendeesSection_requestedBy =
                        "ATTENDEE;CN="
                                + valuesToMail.get("reserve.requestedbymail")
                                + ";ROLE=OPT-PARTICIPANT;RSVP="
                                + String.valueOf(require_reply)
                                + ":MAILTO:"
                                + valuesToMail.get("reserve.requestedbymail")
                                + "\nATTENDEE;CN="
                                + getActivityParameterString(context, ACTIVITY_ID,
                                    "InternalServicesEmail")
                                + ";ROLE=OPT-PARTICIPANT;RSVP=false"
                                + ":MAILTO:"
                                + getActivityParameterString(context, ACTIVITY_ID,
                                    "InternalServicesEmail");
                // end kb3035551
                
                String absoluteFilename = "";
                String line = "";
                final ArrayList attachments = new ArrayList();
                
                final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
                final SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
                
                final String rm_time_start = (String) valuesToMail.get("reserve_rm.time_start");
                final String rm_time_end = (String) valuesToMail.get("reserve_rm.time_end");
                final String rm_date_start = (String) valuesToMail.get("reserve_rm.date_start");
                
                final Time tTimeStart = getTimeFromString(rm_time_start);
                final Time tTimeEnd = getTimeFromString(rm_time_end);
                final Date tDateStart = getDateValue(context, rm_date_start);
                
                final Date DateTimeStartGMT =
                        new Date(tDateStart.getYear(), tDateStart.getMonth(), tDateStart.getDate(),
                            tTimeStart.getHours(), tTimeStart.getMinutes(), tTimeStart.getSeconds());
                final Date DateTimeEndGMT =
                        new Date(tDateStart.getYear(), tDateStart.getMonth(), tDateStart.getDate(),
                            tTimeEnd.getHours(), tTimeEnd.getMinutes(), tTimeEnd.getSeconds());
                
                long time;
                
                time = DateTimeStartGMT.getTime();
                time += minutesoffset * 60 * 1000;
                DateTimeStartGMT.setTime(time);
                
                time = DateTimeEndGMT.getTime();
                time += minutesoffset * 60 * 1000;
                DateTimeEndGMT.setTime(time);
                
                // attendees
                final String[] attendees = email_invitations.split(";");
                line = "";
                for (int i = 0; i < attendees.length; i++) {
                    line += "ATTENDEE;CN=" + attendees[i];
                    line += ";ROLE=OPT-PARTICIPANT;RSVP=" + String.valueOf(require_reply);
                    line += ":MAILTO:" + attendees[i];
                    if (i < attendees.length - 1) {
                        line += "\n";
                    }
                }
                line = line.substring(0, line.length());
                final String AttendeesSection = line;
                
                // Create ICS files
                try {
                    
                    // creating or cancelling reservations
                    if (invitation_type.equals("new") || invitation_type.equals("cancel")) {
                        
                        filename = "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename += valuesToMail.get("reserve_rm.date_start") + ".ics";
                        
                        // kb#3035551: add different ics file for requested by email
                        filename_requestedBy =
                                "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename_requestedBy +=
                                valuesToMail.get("reserve_rm.date_start") + "-requestedBy.ics";
                        
                        // BEGIN: Create Config parameters
                        final TreeMap parametersValues = new TreeMap();
                        parametersValues.put("path", path);
                        parametersValues.put("filename", filename);
                        
                        // kb#3035551: add different ics file for requested by email
                        parametersValues.put("filename_requestedBy", filename_requestedBy);
                        
                        if (invitation_type.equals("new")) {
                            parametersValues.put("method", "REQUEST");
                        } else if (invitation_type.equals("cancel")) {
                            parametersValues.put("method", "CANCEL");
                        }
                        parametersValues.put("attendeesSection", AttendeesSection);
                        
                        // kb#3035551: add different ics file for requested by email
                        parametersValues.put("attendeesSection_requestedBy",
                            attendeesSection_requestedBy);
                        
                        parametersValues.put("mailTo", valuesToMail.get("reserve.requestedbymail"));
                        parametersValues.put("dateStart", dateFormatter.format(DateTimeStartGMT));
                        parametersValues.put("timeStart", timeFormatter.format(DateTimeStartGMT));
                        parametersValues.put("dateEnd", dateFormatter.format(DateTimeEndGMT));
                        parametersValues.put("timeEnd", timeFormatter.format(DateTimeEndGMT));
                        
                        // If it's a recurrent reservation
                        if (invitation_type.equals("new") && !parentId.equals("0")) {
                            // format date_end
                            String res_date_end = jsReservation.getString("date_end");
                            final String res_time_end = jsReservation.getString("time_end");
                            if (!res_date_end.equals("") && res_date_end != null) {
                                final Date tResDateEnd = getDateValue(context, res_date_end);
                                final Date tResTimeEnd = parseTimeString(context, res_time_end);
                                final Date ResDateTimeEndGMT =
                                        new Date(tResDateEnd.getYear(), tResDateEnd.getMonth(),
                                            tResDateEnd.getDate(), tResTimeEnd.getHours(),
                                            tResTimeEnd.getMinutes(), tResTimeEnd.getSeconds());
                                time = ResDateTimeEndGMT.getTime();
                                time += minutesoffset * 60 * 1000;
                                ResDateTimeEndGMT.setTime(time);
                                res_date_end =
                                        dateFormatter.format(ResDateTimeEndGMT) + "T"
                                                + timeFormatter.format(ResDateTimeEndGMT) + "Z";
                            }
                            // get recur_val1 and 2 array
                            final JSONArray res_recur_val1 =
                                    new JSONArray("" + jsReservation.get("recur_val1") + ")");
                            
                            final String interval = jsReservation.has("recur_val3") ?  jsReservation.get("recur_val3").toString() : "1";  
                            
                            if (jsReservation.getString("recur_type").equals("day")) {
                                parametersValues.put("rruleFreq", "DAILY");
                                parametersValues.put("rruleUntil", res_date_end);
                                parametersValues.put("rruleInternal", (res_recur_val1.get(0).toString()));
                                parametersValues.put("WKST", "SU");
                                
                            } else if (jsReservation.getString("recur_type").equals("week")) {
                                String selected_days = "";
                                if ( ! "null".equals(res_recur_val1.getString(0))) {
                                    selected_days += "SU,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(1))) {
                                    selected_days += "MO,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(2))) {
                                    selected_days += "TU,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(3))) {
                                    selected_days += "WE,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(4))) {
                                    selected_days += "TH,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(5))) {
                                    selected_days += "FR,";
                                }
                                if ( ! "null".equals(res_recur_val1.getString(6))) {
                                    selected_days += "SA,";
                                }
                                if (!selected_days.equals("")) {
                                    selected_days =
                                            selected_days.substring(0, selected_days.length() - 1);
                                }
                                line = "RRULE:FREQ=WEEKLY;UNTIL=" + res_date_end;
                                line += ";INTERVAL=1;BYDAY=" + selected_days + ";WKST=SU";
                                parametersValues.put("rruleFreq", "WEEKLY");
                                parametersValues.put("rruleUntil", res_date_end);
                                parametersValues.put("rruleInternal", "1");
                                parametersValues.put("rruleByDay", selected_days);
                                parametersValues.put("WKST", "SU");
                                
                            } else if (jsReservation.getString("recur_type").equals("month")) {
                                // BV
                                if  (jsReservation.has("recur_val2") ) {
                                    
                                    final JSONArray res_recur_val2 =
                                            new JSONArray("" + jsReservation.get("recur_val2") + " )");
                                    
                                    String selected_pos = res_recur_val1.get(0).toString();
                                    // Changed PC 08-05-2007
                                    if (selected_pos.equals("5")) {
                                        selected_pos = "-1";
                                    }
                                    // BV change to equals
                                    String selected_day = "";
                                    if (res_recur_val2.get(0).toString().equals("0")) {
                                        selected_day += "SU";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("1")) {
                                        selected_day += "MO";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("2")) {
                                        selected_day += "TU";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("3")) {
                                        selected_day += "WE";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("4")) {
                                        selected_day += "TH";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("5")) {
                                        selected_day += "FR";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("6")) {
                                        selected_day += "SA";
                                    }
                                    // YG added support for day, weekday and weekend day
                                    if (res_recur_val2.get(0).toString().equals("7")) {
                                        selected_day += "SU,MO,TU,WE,TH,FR,SA";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("8")) {
                                        selected_day += "MO,TU,WE,TH,FR";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("9")) {
                                        selected_day += "SU,SA";
                                    }
                                    parametersValues.put("rruleFreq", "MONTHLY");
                                    parametersValues.put("rruleUntil", res_date_end);
                                    parametersValues.put("rruleInternal", interval);
                                    parametersValues.put("rruleByDay", selected_day);
                                    parametersValues.put("rruleBySetPos", selected_pos);
                                    parametersValues.put("WKST", "SU");
                                    
                                } else {
                                    final String selected_day = res_recur_val1.get(0).toString();
                                    // RRULE:FREQ=MONTHLY;COUNT=5;BYMONTHDAY=5
                                    parametersValues.put("rruleFreq", "MONTHLY");
                                    parametersValues.put("rruleUntil", res_date_end);
                                    parametersValues.put("rruleInternal", interval);
                                    parametersValues.put("rruleByMonthDay", selected_day);
                                    parametersValues.put("WKST", "SU");
                                } 
                                
                            } else if (jsReservation.getString("recur_type").equals("year")) {
                                if (jsReservation.has("recur_val4")) {
                                    final JSONArray res_recur_val2 =
                                            new JSONArray("" + jsReservation.get("recur_val2")
                                                    + " )");
                                    
                                    String selected_pos = res_recur_val1.get(0).toString();
                                    // Changed PC 08-05-2007
                                    if (selected_pos.equals("5")) {
                                        selected_pos = "-1";
                                    }
                                    // BV change to equals
                                    String selected_day = "";
                                    if (res_recur_val2.get(0).toString().equals("0")) {
                                        selected_day += "SU";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("1")) {
                                        selected_day += "MO";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("2")) {
                                        selected_day += "TU";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("3")) {
                                        selected_day += "WE";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("4")) {
                                        selected_day += "TH";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("5")) {
                                        selected_day += "FR";
                                    }
                                    if (res_recur_val2.get(0).toString().equals("6")) {
                                        selected_day += "SA";
                                    }
                                    
                                    final JSONArray res_recur_val4 =
                                            new JSONArray("" + jsReservation.get("recur_val4")
                                                    + " )");
                                    final String selected_month = res_recur_val4.get(0).toString();

                                    parametersValues.put("rruleByDay", selected_day);
                                    parametersValues.put("rruleBySetPos", selected_pos);
                                    parametersValues.put("rruleByMonth", selected_month);
                                } else {
                                    final JSONArray res_recur_val2 =
                                            new JSONArray("" + jsReservation.get("recur_val2")
                                                    + " )");
                                    final String selected_day = res_recur_val1.get(0).toString();
                                    final String selected_month = res_recur_val2.get(0).toString();
                                    // RRULE:FREQ=YEARLY;COUNT=5;BYMONTHDAY=5;BYMONTH=6
                                    parametersValues.put("rruleByMonthDay", selected_day);
                                    parametersValues.put("rruleByMonth", selected_month);
                                }
                                parametersValues.put("rruleFreq", "YEARLY");
                                parametersValues.put("rruleUntil", res_date_end);
                                parametersValues.put("rruleInternal", interval);
                                parametersValues.put("WKST", "SU");
                            }
                            
                            // Get excluded days from jsRoomConflicts
                            if (jsRoomConflicts.length() > 0) {
                                String excluded_dates = "";
                                String rc_date_start = "";
                                String rc_time_start = "";
                                for (int i = 0; i < jsRoomConflicts.length(); i++) {
                                    rc_date_start =
                                            jsRoomConflicts.getJSONObject(i)
                                                .getString("date_start");
                                    rc_time_start =
                                            jsRoomConflicts.getJSONObject(i)
                                                .getString("time_start");
                                    final Date tRcDateStart = getDateValue(context, rc_date_start);
                                    final Date tRcTimeStart = getTimeValue(context, rc_time_start);
                                    final Date RcDateStartGMT =
                                            new Date(tRcDateStart.getYear(),
                                                tRcDateStart.getMonth(), tRcDateStart.getDate(),
                                                tRcTimeStart.getHours(), tRcTimeStart.getMinutes(),
                                                tRcTimeStart.getSeconds());
                                    time = RcDateStartGMT.getTime();
                                    time += minutesoffset * 60 * 1000;
                                    RcDateStartGMT.setTime(time);
                                    rc_date_start = dateFormatter.format(RcDateStartGMT);
                                    excluded_dates += rc_date_start + ",";
                                }
                                if (!excluded_dates.equals("")) {
                                    excluded_dates =
                                            excluded_dates
                                                .substring(0, excluded_dates.length() - 1);
                                }
                                // this.log.info("Final Exclude dates: "+excluded_dates);
                                parametersValues.put("exDate", excluded_dates);
                            }
                        }// END If it's a recurrent reservation
                        
                        if (invitation_type.equals("new")) {
                            parametersValues.put("sequence", "0");
                        } else if (invitation_type.equals("cancel")) {
                            parametersValues.put("sequence", "1");
                        }
                        if (parentId.equals("0")) {
                            parametersValues.put("uid", resId);
                        } else {
                            parametersValues.put("uid", parentId);
                        }
                        // If we want to cancel only one date in the recurrent reservation
                        if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                                && (date_cancel != null) && (!date_cancel.equals(""))) {
                            // PC 3018035 We get from database the info on only the date to cancel
                            // from recurrence set so we already have the values to use as date and
                            // time start and end for the cancellation and don't need the parameters
                            parametersValues.put("recurrence-id",
                                dateFormatter.format(DateTimeStartGMT));
                            
                        }
                        parametersValues.put(
                            "location",
                            valuesToMail.get("reserve_rm.bl_id") + "-"
                                    + valuesToMail.get("reserve_rm.fl_id") + "-"
                                    + valuesToMail.get("reserve_rm.rm_id"));
                        line = messages.get("SUBJECT1") + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.date_start")) + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        // If it's a recurrent reservation
                        if (((invitation_type.equals("new")) || (invitation_type.equals("update")))
                                && (!parentId.equals("0"))) {
                            line += " - " + messages.get("SUBJECT4");
                        }
                        // Indicate that is cancelling the full recurrence set
                        if ((invitation_type.equals("cancel")) && (!parentId.equals("0"))
                                && (date_cancel == null || date_cancel.equals(""))) {
                            line += " - " + messages.get("SUBJECT4");
                        }
                        if (invitation_type.equals("update")) {
                            line += " - " + messages.get("SUBJECT2");
                        } else if (invitation_type.equals("cancel")) {
                            line += " - " + messages.get("SUBJECT3");
                        }
                        parametersValues.put("summary", subject);
                        if ((invitation_type.equals("new")) || (invitation_type.equals("update"))) {
                            line = messages.get("BODY1") + " ";
                        } else if (invitation_type.equals("cancel")) {
                            line = messages.get("BODY1_2") + " ";
                        }
                        line +=
                                String.valueOf(valuesToMail.get("reserve.user_requested_by"))
                                        + "\\n";
                        line +=
                                messages.get("BODY2") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.date_start"))
                                        + "\\n";
                        line +=
                                messages.get("BODY3") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY4") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_end"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY5") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.bl_id"))
                                        + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.fl_id")) + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.rm_id")) + "\\n";
                        if (!String.valueOf(valuesToMail.get("reserve.comments")).equals("")) {
                            line += messages.get("BODY6") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(valuesToMail.get("reserve.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                            if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                                line += "\\n";
                            }
                        }
                        if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                            line += messages.get("BODY6_2") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(
                                                valuesToMail.get("reserve_rm.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                        }
                        parametersValues.put("description", line);
                        // END: Create Config parameters
                        
                        absoluteFilename = createAttachments(context, parametersValues);
                        if (!absoluteFilename.equals("")) {
                            attachments.add(absoluteFilename);
                        }
                        
                        // kb#3035551: add different ics file for requested by email
                        absoluteFilename_requestedBy =
                                createAttachments_requestedBy(context, parametersValues);
                        if (!absoluteFilename_requestedBy.equals("")) {
                            attachments_requestedBy.add(absoluteFilename_requestedBy);
                        }
                        
                        // Cancelling ALL recurrent reservations whose dates were modified
                        if (invitation_type.equals("cancel") && !parentId.equals("0")
                                && (date_cancel == null || date_cancel.equals(""))) {
                            
                            sql =
                                    "SELECT date_start, time_start, time_end, reserve_rm.bl_id, fl_id, rm_id, reserve_rm.comments, rmres_id, city.timezone_id";
                            sql += " FROM reserve_rm, bl LEFT OUTER JOIN city ";
                            sql += " ON city.city_id=bl.city_id AND city.state_id=bl.state_id";
                            sql += " WHERE status = 'Cancelled'";
                            sql += " AND res_id IN";
                            sql += " (SELECT res_id FROM reserve";
                            sql +=
                                    " WHERE res_parent = " + parentId
                                            + " AND recurring_date_modified = 1)";
                            sql += " AND reserve_rm.bl_id=bl.bl_id";
                            sql += " ORDER BY date_start asc";
                            
                            final List RoomReservesToCancel = retrieveDbRecords(context, sql);
                            
                            // this.log.info(ACTIVITY_ID+"-"+RULE_ID+" [sql]: "+sql);
                            
                            if (!RoomReservesToCancel.isEmpty()) {
                                Date resToCancelDateStart;
                                Date resToCancelTimeStart;
                                Date resToCancelTimeEnd;
                                String resToCancelBl;
                                String resToCancelFl;
                                String resToCancelRm;
                                String resToCancelComments;
                                String resToCancelRmResID;
                                TimeZone tz3;
                                Date dateCheckTimezone3 = null;
                                int minutesoffset3 = 0;
                                String TimeZoneStr3 = "";
                                int absOffset3 = 0;
                                
                                for (final Iterator it = RoomReservesToCancel.iterator(); it
                                    .hasNext();) {
                                    final Map record = (Map) it.next();
                                    resToCancelDateStart =
                                            getDateValue(context, record.get("date_start"));
                                    resToCancelTimeStart =
                                            getTimeValue(context, record.get("time_start"));
                                    resToCancelTimeEnd =
                                            getTimeValue(context, record.get("time_end"));
                                    if (!(getString(record, "timezone_id").equals(""))) {
                                        tz3 =
                                                TimeZone.getTimeZone(getString(record,
                                                    "timezone_id"));
                                    } else {
                                        tz3 = TimeZone.getDefault();
                                    }
                                    // PC 3018035 - Each date can have a different GMT offset
                                    dateCheckTimezone3 =
                                            getDateValue(context, record.get("date_start"));
                                    minutesoffset3 =
                                            -(tz3.getOffset(dateCheckTimezone3.getTime()) / 60000);
                                    TimeZoneStr3 =
                                            (minutesoffset3 / 60) < 0 ? "+"
                                                    + timeZoneFormatter
                                                        .format(-minutesoffset3 / 60)
                                                    : timeZoneFormatter
                                                        .format(-minutesoffset3 / 60);
                                    absOffset3 =
                                            (minutesoffset3 > 0 ? minutesoffset3 : -minutesoffset3);
                                    TimeZoneStr3 +=
                                            (absOffset3 % 60) > 0 ? ":"
                                                    + timeZoneFormatter.format(absOffset3 % 60)
                                                    : "";
                                    
                                    resToCancelBl = getString(record, "bl_id");
                                    resToCancelFl = getString(record, "fl_id");
                                    resToCancelRm = getString(record, "rm_id");
                                    resToCancelComments = getString(record, "comments");
                                    resToCancelRmResID =
                                            getIntegerValue(context, record.get("rmres_id"))
                                                .toString();
                                    final Date resToCancelDateTimeStartGMT =
                                            new Date(resToCancelDateStart.getYear(),
                                                resToCancelDateStart.getMonth(),
                                                resToCancelDateStart.getDate(),
                                                resToCancelTimeStart.getHours(),
                                                resToCancelTimeStart.getMinutes(),
                                                resToCancelTimeStart.getSeconds());
                                    time = resToCancelDateTimeStartGMT.getTime();
                                    time += minutesoffset3 * 60 * 1000;
                                    resToCancelDateTimeStartGMT.setTime(time);
                                    final Date resToCancelDateTimeEndGMT =
                                            new Date(resToCancelDateStart.getYear(),
                                                resToCancelDateStart.getMonth(),
                                                resToCancelDateStart.getDate(),
                                                resToCancelTimeEnd.getHours(),
                                                resToCancelTimeEnd.getMinutes(),
                                                resToCancelTimeEnd.getSeconds());
                                    time = resToCancelDateTimeEndGMT.getTime();
                                    time += minutesoffset3 * 60 * 1000;
                                    resToCancelDateTimeEndGMT.setTime(time);
                                    
                                    filename = "reservation-" + resToCancelRmResID + "-";
                                    filename +=
                                            String.valueOf(resToCancelDateStart) + "-cancel.ics";
                                    
                                    // kb#3035551: add different ics file for requested by email
                                    filename_requestedBy =
                                            "reservation-" + resToCancelRmResID + "-";
                                    filename_requestedBy +=
                                            String.valueOf(resToCancelDateStart)
                                                    + "-cancel-requestedBy.ics";
                                    
                                    // BEGIN: Create Config parameters
                                    parametersValues.clear();
                                    parametersValues.put("path", path);
                                    parametersValues.put("filename", filename);
                                    
                                    // kb#3035551: add different ics file for requested by email
                                    parametersValues.put("filename_requestedBy",
                                        filename_requestedBy);
                                    parametersValues.put("attendeesSection_requestedBy",
                                        attendeesSection_requestedBy);
                                    
                                    parametersValues.put("method", "CANCEL");
                                    parametersValues.put("attendeesSection", AttendeesSection);
                                    parametersValues.put("mailTo",
                                        valuesToMail.get("reserve.requestedbymail"));
                                    parametersValues.put("dateStart",
                                        dateFormatter.format(resToCancelDateTimeStartGMT));
                                    parametersValues.put("timeStart",
                                        timeFormatter.format(resToCancelDateTimeStartGMT));
                                    parametersValues.put("dateEnd",
                                        dateFormatter.format(resToCancelDateTimeEndGMT));
                                    parametersValues.put("timeEnd",
                                        timeFormatter.format(resToCancelDateTimeEndGMT));
                                    parametersValues.put("sequence", "1");
                                    parametersValues.put("uid", parentId);
                                    parametersValues.put("recurrence-id",
                                        dateFormatter.format(resToCancelDateStart));
                                    parametersValues.put("location", resToCancelBl + "-"
                                            + resToCancelFl + "-" + resToCancelRm);
                                    line = messages.get("SUBJECT1") + " ";
                                    line += String.valueOf(resToCancelDateStart) + " ";
                                    line += String.valueOf(resToCancelTimeStart);
                                    line += " - " + messages.get("SUBJECT3");
                                    parametersValues.put("summary", line);
                                    line = messages.get("BODY1_2") + " ";
                                    line +=
                                            String.valueOf(valuesToMail
                                                .get("reserve.user_requested_by")) + "\\n";
                                    line +=
                                            messages.get("BODY2") + " "
                                                    + String.valueOf(resToCancelDateStart) + "\\n";
                                    line += messages.get("BODY3") + " ";
                                    // PC 3018035 - Each date can have a different time
                                    // line +=
                                    // String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                                    line += String.valueOf(resToCancelTimeStart);
                                    line += " GMT" + TimeZoneStr3 + "\\n";
                                    line += messages.get("BODY4") + " ";
                                    // PC 3018035 - Each date can have a different time
                                    // line +=
                                    // String.valueOf(valuesToMail.get("reserve_rm.time_end"))
                                    line +=
                                            String.valueOf(resToCancelTimeEnd) + " GMT"
                                                    + TimeZoneStr3 + "\\n";
                                    line +=
                                            messages.get("BODY5") + " " + resToCancelBl + "-"
                                                    + resToCancelFl + "-" + resToCancelRm + "\\n";
                                    if (!String.valueOf(valuesToMail.get("reserve.comments"))
                                        .equals("")) {
                                        line += messages.get("BODY6") + "\\n";
                                        // PC changed to solve KB item 3018280
                                        line +=
                                                "\t"
                                                        + String.valueOf(
                                                            valuesToMail.get("reserve.comments"))
                                                            .replaceAll("\r\n", "\\\\n");
                                        if (!resToCancelComments.equals("")) {
                                            line += "\\n";
                                        }
                                    }
                                    if (!resToCancelComments.equals("")) {
                                        line += messages.get("BODY6_2") + "\\n";
                                        // PC changed to solve KB item 3018280
                                        line +=
                                                "\t"
                                                        + resToCancelComments.replaceAll("\r\n",
                                                            "\\\\n");
                                    }
                                    parametersValues.put("description", line);
                                    // END: Create Config parameters
                                    
                                    absoluteFilename = createAttachments(context, parametersValues);
                                    if (!absoluteFilename.equals("")) {
                                        attachments.add(absoluteFilename);
                                    }
                                    
                                    // kb#3035551: add different ics file for requested by email
                                    absoluteFilename_requestedBy =
                                            createAttachments_requestedBy(context, parametersValues);
                                    if (!absoluteFilename_requestedBy.equals("")) {
                                        attachments_requestedBy.add(absoluteFilename_requestedBy);
                                    }
                                    
                                } // for
                            }// iif (!RoomReservesToCancel.isEmpty())
                        }// if (invitation_type.equals("cancel") && !parentId.equals("0") &&
                         // date_cancel = null)
                    }// if ( invitation_type.equals("new") || invitation_type.equals("cancel"))
                    
                    // Updating regular reservations or recurrent reservations where no date was
                    // changed manually by the user
                    if ((invitation_type.equals("update"))
                            && ((original_date == null) || (original_date.equals("")))) {
                        // this.log.info("Update without changing the original date");
                        filename = "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename += valuesToMail.get("reserve_rm.date_start") + ".ics";
                        
                        // kb#3035551: add different ics file for requested by email
                        filename_requestedBy =
                                "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename_requestedBy +=
                                valuesToMail.get("reserve_rm.date_start") + "-requestedBy.ics";
                        
                        // BEGIN: Create Config parameters
                        final TreeMap parametersValues = new TreeMap();
                        parametersValues.put("path", path);
                        parametersValues.put("filename", filename);
                        
                        // kb#3035551: add different ics file for requested by email
                        parametersValues.put("filename_requestedBy", filename_requestedBy);
                        parametersValues.put("attendeesSection_requestedBy",
                            attendeesSection_requestedBy);
                        
                        parametersValues.put("method", "REQUEST");
                        parametersValues.put("attendeesSection", AttendeesSection);
                        parametersValues.put("mailTo", valuesToMail.get("reserve.requestedbymail"));
                        parametersValues.put("dateStart", dateFormatter.format(DateTimeStartGMT));
                        parametersValues.put("timeStart", timeFormatter.format(DateTimeStartGMT));
                        parametersValues.put("dateEnd", dateFormatter.format(DateTimeEndGMT));
                        parametersValues.put("timeEnd", timeFormatter.format(DateTimeEndGMT));
                        parametersValues.put("sequence", "0");
                        if (parentId.equals("0")) {
                            line = "UID:" + resId;
                            parametersValues.put("uid", resId);
                        } else {
                            parametersValues.put("uid", parentId);
                            parametersValues.put("recurrence-id",
                                dateFormatter.format(DateTimeStartGMT));
                        }
                        parametersValues.put(
                            "location",
                            valuesToMail.get("reserve_rm.bl_id") + "-"
                                    + valuesToMail.get("reserve_rm.fl_id") + "-"
                                    + valuesToMail.get("reserve_rm.rm_id"));
                        line = messages.get("SUBJECT1") + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.date_start")) + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        // Indicate that's a recurrent reservation
                        if (!parentId.equals("0")) {
                            line += " - " + messages.get("SUBJECT4");
                        }
                        line += " - " + messages.get("SUBJECT2");
                        parametersValues.put("summary", line);
                        line = messages.get("BODY1") + " ";
                        line +=
                                String.valueOf(valuesToMail.get("reserve.user_requested_by"))
                                        + "\\n";
                        line +=
                                messages.get("BODY2") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.date_start"))
                                        + "\\n";
                        line +=
                                messages.get("BODY3") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY4") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_end"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY5") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.bl_id"))
                                        + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.fl_id")) + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.rm_id")) + "\\n";
                        if (!String.valueOf(valuesToMail.get("reserve.comments")).equals("")) {
                            line += messages.get("BODY6") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(valuesToMail.get("reserve.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                            if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                                line += "\\n";
                            }
                        }
                        if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                            line += messages.get("BODY6_2") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(
                                                valuesToMail.get("reserve_rm.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                        }
                        parametersValues.put("description", line);
                        // END: Create Config parameters
                        
                        absoluteFilename = createAttachments(context, parametersValues);
                        if (!absoluteFilename.equals("")) {
                            attachments.add(absoluteFilename);
                        }
                        
                        // kb#3035551: add different ics file for requested by email
                        absoluteFilename_requestedBy =
                                createAttachments_requestedBy(context, parametersValues);
                        if (!absoluteFilename_requestedBy.equals("")) {
                            attachments_requestedBy.add(absoluteFilename_requestedBy);
                        }
                        
                    } // if ( invitation_type.equals("update") && (parentId.equals("0") ||
                      // (!parentId.equals("0") && original_date == null)))
                    
                    // Updating recurrent reservations where a date has changed
                    if ((invitation_type.equals("update"))
                            && ((original_date != null) && (!original_date.equals("")))) {
                        
                        // cancel original reservation
                        filename = "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename += valuesToMail.get("reserve_rm.date_start") + "-cancel.ics";
                        
                        // kb#3035551: add different ics file for requested by email
                        filename_requestedBy =
                                "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename_requestedBy +=
                                valuesToMail.get("reserve_rm.date_start")
                                        + "-cancel-requestedBy.ics";
                        
                        // BEGIN: Create Config parameters
                        final TreeMap parametersValues = new TreeMap();
                        parametersValues.put("path", path);
                        parametersValues.put("filename", filename);
                        
                        // kb#3035551: add different ics file for requested by email
                        parametersValues.put("filename_requestedBy", filename_requestedBy);
                        parametersValues.put("attendeesSection_requestedBy",
                            attendeesSection_requestedBy);
                        
                        parametersValues.put("method", "CANCEL");
                        parametersValues.put("attendeesSection", AttendeesSection);
                        parametersValues.put("mailTo", valuesToMail.get("reserve.requestedbymail"));
                        final Date tOriginalDate = getDateValue(context, original_date);
                        // PC 3018035 we get original date and also the times as parameters
                        /*
                         * String tOriginalTimeStart_str = (String) valuesToMail
                         * .get("reserve_rm.time_start"); String tOriginalTimeEnd_str = (String)
                         * valuesToMail .get("reserve_rm.time_end"); Time tOriginalTimeStart =
                         * getTimeFromString(tOriginalTimeStart_str); Time tOriginalTimeEnd =
                         * getTimeFromString(tOriginalTimeEnd_str);
                         */
                        TimeZone tz4 = null;
                        final Date dateCheckTimezone4 = getDateValue(context, original_date);
                        if (!original_cityTimezone.equals("")) {
                            tz4 = TimeZone.getTimeZone(original_cityTimezone);
                        } else {
                            tz4 = TimeZone.getDefault();
                        }
                        final int minutesoffset4 =
                                -(tz4.getOffset(dateCheckTimezone4.getTime()) / 60000);
                        String TimeZoneStr4 =
                                (minutesoffset4 / 60) < 0 ? "+"
                                        + timeZoneFormatter.format(-minutesoffset4 / 60)
                                        : timeZoneFormatter.format(-minutesoffset4 / 60);
                        final int absOffset4 =
                                (minutesoffset4 > 0 ? minutesoffset4 : -minutesoffset4);
                        TimeZoneStr4 +=
                                (absOffset4 % 60) > 0 ? ":"
                                        + timeZoneFormatter.format(absOffset4 % 60) : "";
                        
                        final Time tOriginalTimeStart = getTimeFromString(original_time_start);
                        final Time tOriginalTimeEnd = getTimeFromString(original_time_end);
                        
                        final Date OriginalDateTimeStartGMT =
                                new Date(tOriginalDate.getYear(), tOriginalDate.getMonth(),
                                    tOriginalDate.getDate(), tOriginalTimeStart.getHours(),
                                    tOriginalTimeStart.getMinutes(),
                                    tOriginalTimeStart.getSeconds());
                        time = OriginalDateTimeStartGMT.getTime();
                        time += minutesoffset4 * 60 * 1000;
                        OriginalDateTimeStartGMT.setTime(time);
                        final Date OriginalDateTimeEndGMT =
                                new Date(tOriginalDate.getYear(), tOriginalDate.getMonth(),
                                    tOriginalDate.getDate(), tOriginalTimeEnd.getHours(),
                                    tOriginalTimeEnd.getMinutes(), tOriginalTimeEnd.getSeconds());
                        time = OriginalDateTimeEndGMT.getTime();
                        time += minutesoffset4 * 60 * 1000;
                        OriginalDateTimeEndGMT.setTime(time);
                        parametersValues.put("dateStart",
                            dateFormatter.format(OriginalDateTimeStartGMT));
                        parametersValues.put("timeStart",
                            timeFormatter.format(OriginalDateTimeStartGMT));
                        parametersValues.put("dateEnd",
                            dateFormatter.format(OriginalDateTimeEndGMT));
                        parametersValues.put("timeEnd",
                            timeFormatter.format(OriginalDateTimeEndGMT));
                        parametersValues.put("sequence", "1");
                        parametersValues.put("uid", parentId);
                        parametersValues.put("recurrence-id",
                            dateFormatter.format(OriginalDateTimeStartGMT));
                        parametersValues.put(
                            "location",
                            valuesToMail.get("reserve_rm.bl_id") + "-"
                                    + valuesToMail.get("reserve_rm.fl_id") + "-"
                                    + valuesToMail.get("reserve_rm.rm_id"));
                        line = messages.get("SUBJECT1") + " ";
                        line += String.valueOf(getDateValue(context, original_date)) + " ";
                        // PC 3018035 we get original date and also the times
                        // line += String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        line += String.valueOf(original_time_start);
                        line += " - " + messages.get("SUBJECT3");
                        parametersValues.put("summary", line);
                        line = messages.get("BODY1_2") + " ";
                        line +=
                                String.valueOf(valuesToMail.get("reserve.user_requested_by"))
                                        + "\\n";
                        line += messages.get("BODY2") + " "
                        // PC 3018035 we get original date and also the times
                        // + String.valueOf(valuesToMail.get("reserve_rm.date_start")) +
                        // "\\n";
                                + String.valueOf(getDateValue(context, original_date)) + "\\n";
                        line += messages.get("BODY3") + " "
                        // PC 3018035 we get original date and also the times
                        // + String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                                + String.valueOf(original_time_start);
                        line += " GMT" + TimeZoneStr4 + "\\n";
                        line += messages.get("BODY4") + " "
                        // PC 3018035 we get original date and also the times
                        // + String.valueOf(valuesToMail.get("reserve_rm.time_end"));
                                + String.valueOf(original_time_end);
                        line += " GMT" + TimeZoneStr4 + "\\n";
                        line +=
                                messages.get("BODY5") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.bl_id"))
                                        + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.fl_id")) + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.rm_id")) + "\\n";
                        if (!String.valueOf(valuesToMail.get("reserve.comments")).equals("")) {
                            line += messages.get("BODY6") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(valuesToMail.get("reserve.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                            if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                                line += "\\n";
                            }
                        }
                        if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                            line += messages.get("BODY6_2") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(
                                                valuesToMail.get("reserve_rm.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                        }
                        parametersValues.put("description", line);
                        // END: Create Config parameters
                        
                        absoluteFilename = createAttachments(context, parametersValues);
                        if (!absoluteFilename.equals("")) {
                            attachments.add(absoluteFilename);
                        }
                        
                        // kb#3035551: add different ics file for requested by email
                        absoluteFilename_requestedBy =
                                createAttachments_requestedBy(context, parametersValues);
                        if (!absoluteFilename_requestedBy.equals("")) {
                            attachments_requestedBy.add(absoluteFilename_requestedBy);
                        }
                        
                        // create new reservation to substitute the cancelled one
                        filename = "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename += valuesToMail.get("reserve_rm.date_start") + ".ics";
                        
                        // kb#3035551: add different ics file for requested by email
                        filename_requestedBy =
                                "reservation-" + valuesToMail.get("reserve_rm.rmres_id") + "-";
                        filename_requestedBy +=
                                valuesToMail.get("reserve_rm.date_start") + "-requestedBy.ics";
                        
                        // BEGIN: Create Config parameters
                        parametersValues.clear();
                        parametersValues.put("path", path);
                        parametersValues.put("filename", filename);
                        
                        // kb#3035551: add different ics file for requested by email
                        parametersValues.put("filename_requestedBy", filename_requestedBy);
                        parametersValues.put("attendeesSection_requestedBy",
                            attendeesSection_requestedBy);
                        
                        parametersValues.put("method", "REQUEST");
                        parametersValues.put("attendeesSection", AttendeesSection);
                        parametersValues.put("mailTo", valuesToMail.get("reserve.requestedbymail"));
                        parametersValues.put("dateStart", dateFormatter.format(DateTimeStartGMT));
                        parametersValues.put("timeStart", timeFormatter.format(DateTimeStartGMT));
                        parametersValues.put("dateEnd", dateFormatter.format(DateTimeEndGMT));
                        parametersValues.put("timeEnd", timeFormatter.format(DateTimeEndGMT));
                        parametersValues.put("sequence", "0");
                        parametersValues.put("uid", parentId);
                        parametersValues.put("recurrence-id",
                            dateFormatter.format(DateTimeStartGMT));
                        parametersValues.put(
                            "location",
                            valuesToMail.get("reserve_rm.bl_id") + "-"
                                    + valuesToMail.get("reserve_rm.fl_id") + "-"
                                    + valuesToMail.get("reserve_rm.rm_id"));
                        line = messages.get("SUBJECT1") + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.date_start")) + " ";
                        line += String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        // Indicate that's a recurrent reservation
                        if (!parentId.equals("0")) {
                            line += " - " + messages.get("SUBJECT4");
                        }
                        line += " - " + messages.get("SUBJECT2");
                        parametersValues.put("summary", line);
                        line = messages.get("BODY1") + " ";
                        line +=
                                String.valueOf(valuesToMail.get("reserve.user_requested_by"))
                                        + "\\n";
                        line +=
                                messages.get("BODY2") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.date_start"))
                                        + "\\n";
                        line +=
                                messages.get("BODY3") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_start"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY4") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.time_end"));
                        line += " GMT" + TimeZoneStr + "\\n";
                        line +=
                                messages.get("BODY5") + " "
                                        + String.valueOf(valuesToMail.get("reserve_rm.bl_id"))
                                        + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.fl_id")) + "-";
                        line += String.valueOf(valuesToMail.get("reserve_rm.rm_id")) + "\\n";
                        if (!String.valueOf(valuesToMail.get("reserve.comments")).equals("")) {
                            line += messages.get("BODY6") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(valuesToMail.get("reserve.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                            if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                                line += "\\n";
                            }
                        }
                        if (!String.valueOf(valuesToMail.get("reserve_rm.comments")).equals("")) {
                            line += messages.get("BODY6_2") + "\\n";
                            // PC changed to solve KB item 3018280
                            line +=
                                    "\t"
                                            + String.valueOf(
                                                valuesToMail.get("reserve_rm.comments"))
                                                .replaceAll("\r\n", "\\\\n");
                        }
                        parametersValues.put("description", line);
                        // END: Create Config parameters
                        
                        absoluteFilename = createAttachments(context, parametersValues);
                        if (!absoluteFilename.equals("")) {
                            attachments.add(absoluteFilename);
                        }
                        
                        // kb#3035551: add different ics file for requested by email
                        absoluteFilename_requestedBy =
                                createAttachments_requestedBy(context, parametersValues);
                        if (!absoluteFilename_requestedBy.equals("")) {
                            attachments_requestedBy.add(absoluteFilename_requestedBy);
                        }
                        
                    }// if (invitation_type.equals("update") && !parentId.equals("0") &&
                     // original_date != null)
                    
                } catch (final Throwable e) {
                    handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID
                            + ": Failed sending invitations " + e.getMessage(), errMessage, e, "");
                    log.error("Failed sending invitations", e);
                }
                // END: ics files creation
                
                // sending email
                String from = String.valueOf(valuesToMail.get("reserve.requestedbymail"));
                final String host = getEmailHost(context);
                // PC changed to solve KB 3016618
                final String port = getEmailPort(context);
                final String userId = getEmailUserId(context);
                final String password = getEmailPassword(context);
                
                // PC changed to solve KB 3016618
                
                // if 'to' address exists
                if (to != null && !to.equals("")) {
                    try {
                        sendEmail(body, from, host, port, subject, to, null, null, userId,
                            password, attachments, CONTENT_TYPE_TEXT_UFT8, ACTIVITY_ID);
                    } catch (final Throwable e) {
                        handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID
                                + ": SENDEMAIL ERROR", errMessage, e, to);
                        log.error("Failed sending invitations", e);
                    }
                    
                }
                
                // kb#3035551: add different ics file for requested by email, so don't need below
                // code
                from = getActivityParameterString(context, ACTIVITY_ID, "InternalServicesEmail");
                try {
                    sendEmail(body, from, host, port, subject,
                        String.valueOf(valuesToMail.get("reserve.requestedbymail")), null, null,
                        userId, password, attachments_requestedBy, CONTENT_TYPE_TEXT_UFT8,
                        ACTIVITY_ID);
                } catch (final Throwable e) {
                    handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID
                            + ": SENDEMAIL ERROR", errMessage, e,
                        String.valueOf(valuesToMail.get("reserve.requestedbymail")));
                    log.error("Failed sending invitations", e);
                }
                
            } // if ( !resId.equals("") )
        } catch (final Throwable e) {
            handleNotificationError(context, ACTIVITY_ID + "-" + RULE_ID
                    + ": Failed sending invitations " + e.getMessage(), errMessage, e, "");
            log.error("Failed sending invitations", e);
            
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END sendEmailInvitations
    // ---------------------------------------------------------------------------------------------
    
    public static Time parseTimeString(final EventHandlerContext context, final String timeString) {
        Time time = null;
        if (timeString.length() < 10) {
            final String[] timeArray = timeString.split(":");
            final String hours = timeArray[0];
            final String minutes = timeArray[1];
            time = new Time(new Integer(hours).intValue(), new Integer(minutes).intValue(), 0);
        } else {
            time = getTimeValue(context, timeString);
        }
        return time;
    }
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN getTimelineLimits
    // ---------------------------------------------------------------------------------------------
    /**
     * Gets the afm_activity_params values for timelineStartTime and timelineEndTime
     */
    public void getTimelineLimits() {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        // activityparameter error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "LOADTIMELINE_WFR", "INVALIDPARAMETERERROR",
                    null);
        
        final String strTimelineStart =
                getActivityParameterString(context, "AbWorkplaceReservations", "TimelineStartTime");
        final String strTimelineEnd =
                getActivityParameterString(context, "AbWorkplaceReservations", "TimelineEndTime");
        
        String strStart = "", strEnd = "";
        Date dateStart, dateEnd;
        
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm.ss");
        
        if (StringUtil.notNullOrEmpty(strTimelineStart)) {
            /*
             * SK: the code below has compile errors, so I'm commenting it. Why do we need to handle
             * integers anyway? // First see if it's an integer try { valStart = new
             * Integer(strTimelineStart); // NEXT LINE HAS A COMPILE ERROR dateStart = new
             * Date(valStart); strStart = sdf.format(dateStart); valEnd = new
             * Integer(strTimelineEnd); // NEXT LINE HAS A COMPILE ERROR dateEnd = new Date(valEnd);
             * strEnd = sdf.format(dateEnd); } catch (NumberFormatException ne) {
             */
            // Not an int, see if it's a valid Time value
            try {
                dateStart = sdf.parse(strTimelineStart);
                strStart = sdf.format(dateStart);
                dateEnd = sdf.parse(strTimelineEnd);
                strEnd = sdf.format(dateEnd);
            } catch (final Throwable e) {
                // Invalid format - log error
                context.addResponseParameter("message", errMessage
                        + " TimelineStartTime, TimelineEndTime");
            }
            /*
             * SK: see the comment above. }
             */
        }
        
        final JSONObject timelineLimits = new JSONObject();
        timelineLimits.put("TimelineStartTime", strStart);
        timelineLimits.put("TimelineEndTime", strEnd);
        
        // this.log.info(ACTIVITY_ID + "- getTimelineLimits: Expression: " +
        // timelineLimits.toString());
        context.addResponseParameter("jsonExpression", timelineLimits.toString());
        
    }
    
    // ---------------------------------------------------------------------------------------------
    // END getTimelineLimits
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN getNumberPendingReservations
    // ---------------------------------------------------------------------------------------------
    /**
     * Gets the number of pending reservations for a room using specified field values.
     */
    public void getNumberPendingReservations(final String xmlRecord) {
        final String RULE_ID = "getNumberPendingReservations";
        // get input parameter containing <record> XML string
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        final String recordXmlString = xmlRecord;
        // this.log.info("Input parameter: " + recordXmlString);
        String bl_id = "";
        String fl_id = "";
        String rm_id = "";
        String config_id = "";
        String rm_arrange_type_id = "";
        Integer reservable = new Integer(0);
        
        String sql = "";
        
        // getNumberPendingReservations rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "GETNUMBERPENDINGRESERVATIONS_WFR",
                    "GETNUMBERPENDINGRESERVATIONSERROR", null);
        
        try {
            if (!recordXmlString.equals("")) {
                // parse XML string into DOM document
                final Document recordXmlDoc =
                        new SAXReader().read(new StringReader(recordXmlString));
                // parse record XML into a Map
                final Map recordValues = parseRecord(context, recordXmlDoc.getRootElement());
                // obtain room primary key values and the reservable field
                bl_id = (String) recordValues.get("rm_arrange.bl_id");
                fl_id = (String) recordValues.get("rm_arrange.fl_id");
                rm_id = (String) recordValues.get("rm_arrange.rm_id");
                config_id = (String) recordValues.get("rm_arrange.config_id");
                rm_arrange_type_id = (String) recordValues.get("rm_arrange.rm_arrange_type_id");
                reservable = (Integer) recordValues.get("rm_arrange.reservable");
            }
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed parse XML", errMessage, e);
        }
        
        if ((!bl_id.equals("")) && (!fl_id.equals("")) && (!rm_id.equals(""))
                && (!config_id.equals("")) && (!rm_arrange_type_id.equals(""))
                && ((reservable.intValue()) == 0)) {
            sql =
                    " SELECT COUNT(*) as pending FROM reserve_rm WHERE status <> 'Cancelled' "
                            + " AND status <> 'Rejected' AND bl_id = " + literal(context, bl_id)
                            + " AND fl_id = " + literal(context, fl_id) + " AND rm_id = "
                            + literal(context, rm_id) + " AND config_id = "
                            + literal(context, config_id) + " AND rm_arrange_type_id = "
                            + literal(context, rm_arrange_type_id);
            
            try {
                final List reservePending = retrieveDbRecords(context, sql);
                if (!reservePending.isEmpty()) {
                    final Map record = (Map) reservePending.get(0);
                    final JSONObject pendingReservations = new JSONObject();
                    pendingReservations.put("numberPendingRes", getString(record, "pending"));
                    context.addResponseParameter("jsonExpression", pendingReservations.toString());
                }
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                    errMessage, e);
            }
        } else {
            context.addResponseParameter("message", errMessage);
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END getNumberPendingReservations
    // ---------------------------------------------------------------------------------------------
    
    // ---------------------------------------------------------------------------------------------
    // BEGIN getNumberPendingResourceReservations
    // ---------------------------------------------------------------------------------------------
    /**
     * Gets the number of pending reservations for a resource using specified field values.
     */
    public void getNumberPendingResourceReservations(final String xmlRecord) {
        final String RULE_ID = "getNumberPendingResourceReservations";
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        // get input parameter containing <record> XML string
        final String recordXmlString = xmlRecord;
        // this.log.info("Input parameter: " + recordXmlString);
        String resource_id = "";
        Integer reservable = new Integer(0);
        
        String sql = "";
        
        // getNumberPendingResourceReservations rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "GETNUMBERPENDINGRESOURCERESERVATIONS_WFR",
                    "GETNUMBERPENDINGRESOURCERESERVATIONSERROR", null);
        
        try {
            if (!recordXmlString.equals("")) {
                // parse XML string into DOM document
                final Document recordXmlDoc =
                        new SAXReader().read(new StringReader(recordXmlString));
                // parse record XML into a Map
                final Map recordValues = parseRecord(context, recordXmlDoc.getRootElement());
                // obtain resource primary key values and the reservable field
                resource_id = (String) recordValues.get("resources.resource_id");
                reservable = (Integer) recordValues.get("resources.reservable");
            }
        } catch (final Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed parse XML", errMessage, e);
        }
        
        if ((!resource_id.equals("")) && ((reservable.intValue()) == 0)) {
            sql =
                    " SELECT COUNT(*) as pending FROM reserve_rs WHERE status <> 'Cancelled' "
                            + " AND status <> 'Rejected' AND resource_id = "
                            + literal(context, resource_id);
            
            try {
                final List reservePending = retrieveDbRecords(context, sql);
                if (!reservePending.isEmpty()) {
                    final Map record = (Map) reservePending.get(0);
                    final JSONObject pendingReservations = new JSONObject();
                    pendingReservations.put("numberPendingRes", getString(record, "pending"));
                    context.addResponseParameter("jsonExpression", pendingReservations.toString());
                }
            } catch (final Throwable e) {
                handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed sql: " + sql,
                    errMessage, e);
            }
        } else {
            context.addResponseParameter("message", errMessage);
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    // END getNumberPendingResourcesReservations
    // ---------------------------------------------------------------------------------------------
        
    // ---------------------------------------------------------------------------------------------
    // BEGIN notifyApprover
    // ---------------------------------------------------------------------------------------------
    /**
     * This rule gets the identifier of a room or resource reservation which approval time has
     * expired and notifies the selected user Inputs: context (EventHandlerContext); res_id (String)
     * the res_id value; rmrsres_id (String) can be a rmres_id or a rsres_id value; res_type
     * (String) can be "room" or "resource"; user_to_notify (String) Outputs: message error message
     * in necesary case
     * 
     * @param context Event handler context.
     */
    public void notifyApprover(final EventHandlerContext context, final String res_type) {
        
        // this.log.info("Executing '" + ACTIVITY_ID + "-" + RULE_ID + "' .... ");
        // Get input parameters
        final String resId = (String) context.getParameter("res_id");
        final String rmrsResId = (String) context.getParameter("rmrsres_id");
        final String userToNotify = (String) context.getParameter("user_to_notify");
        // this.log.info("'" + ACTIVITY_ID + "-" + RULE_ID + "' [res_id]: " + resId + " [res_type]:
        // " + res_type + " [user_to_notify]: " + userToNotify);
        
        String sql = "";
        boolean allQueriesOk = true;
        final TreeMap valuesToMail = new TreeMap();
        
        // notification rule error message
        final String errMessage =
                localizeMessage(context, ACTIVITY_ID, "NOTIFYAPPROVER_WFR", "NOTIFYAPPROVERERROR",
                    null);
        
        try {
            
            // BEGIN: Only enter if exists a room or resource reservation identifier and a user to
            // notify.
            if ((!rmrsResId.equals("")) && (!userToNotify.equals(""))) {
                
                // Search the email and locale of the user to notify
                sql =
                        " SELECT email, locale FROM afm_users WHERE user_name = "
                                + literal(context, userToNotify);
                
                final List recordsSql1 = retrieveDbRecords(context, sql);
                
                // If the email and locale is found
                if (!recordsSql1.isEmpty()) {
                    final Map recordOfSql1 = (Map) recordsSql1.get(0);
                    valuesToMail.put("locale", getString(recordOfSql1, "locale"));
                    valuesToMail.put("approveremail", getString(recordOfSql1, "email"));
                    allQueriesOk = true;
                } else {
                    allQueriesOk = false;
                }
                
                // BEGIN: In case that we have the email direction to notify
                if (!((String) valuesToMail.get("approveremail")).equals("")) {
                    
                    // If we can't find the locale of the user to notify, search the locale of the
                    // creator of the reservation
                    if (((String) valuesToMail.get("locale")).equals("")) {
                        
                        // BEGIN: Get reservation info and the creator to search for its locale
                        sql = " SELECT locale FROM reserve, em, afm_users ";
                        sql += " WHERE reserve.res_id= " + resId;
                        sql += " AND reserve.user_created_by = em.em_id ";
                        sql += " AND afm_users.email = em.email ";
                        
                        final List recordsSql = retrieveDbRecords(context, sql);
                        
                        // If locale is found
                        if (!recordsSql.isEmpty()) {
                            final Map recordOfSql = (Map) recordsSql.get(0);
                            valuesToMail.put("locale", getString(recordOfSql, "locale"));
                            allQueriesOk = true;
                        } else {
                            allQueriesOk = false;
                        }
                    }
                    
                    // In case that we still haven't found the locale we'll take english
                    if (((String) valuesToMail.get("locale")).equals("")) {
                        valuesToMail.put("locale", "en_US");
                    }
                    
                    allQueriesOk = false;
                    
                    // Get room reservation info if it's a room reservation
                    if (res_type.equals("room")) {
                        
                        sql =
                                "SELECT rmres_id,reserve_rm.date_start,reserve_rm.time_start,reserve_rm.time_end,"
                                        + " user_requested_for,bl_id,fl_id,rm_id,config_id,rm_arrange_type_id"
                                        + " FROM reserve, reserve_rm "
                                        + " WHERE reserve.res_id=reserve_rm.res_id"
                                        + " AND reserve_rm.rmres_id=" + rmrsResId;
                        
                        final List recordsSql2 = retrieveDbRecords(context, sql);
                        
                        if (!recordsSql2.isEmpty()) {
                            final Map recordOfSql2 = (Map) recordsSql2.get(0);
                            valuesToMail.put("rmres_id", getString(recordOfSql2, "rmres_id"));
                            valuesToMail.put("user_requested_for",
                                getString(recordOfSql2, "user_requested_for"));
                            valuesToMail.put("date_start",
                                getDateValue(context, recordOfSql2.get("date_start")).toString());
                            valuesToMail.put("time_start",
                                getTimeValue(context, recordOfSql2.get("time_start")).toString());
                            valuesToMail.put("time_end",
                                getTimeValue(context, recordOfSql2.get("time_end")).toString());
                            valuesToMail.put("bl_id", getString(recordOfSql2, "bl_id"));
                            valuesToMail.put("fl_id", getString(recordOfSql2, "fl_id"));
                            valuesToMail.put("rm_id", getString(recordOfSql2, "rm_id"));
                            valuesToMail.put("config_id", getString(recordOfSql2, "config_id"));
                            valuesToMail.put("rm_arrange_type_id",
                                getString(recordOfSql2, "rm_arrange_type_id"));
                            allQueriesOk = true;
                        }
                    }
                    
                    // Get resource reservation info if it's a resource reservation
                    else if (res_type.equals("resource")) {
                        
                        sql =
                                "SELECT rsres_id,reserve_rs.date_start,reserve_rs.time_start,reserve_rs.time_end,"
                                        + " user_requested_for,resource_id,quantity"
                                        + " FROM reserve, reserve_rs "
                                        + " WHERE reserve.res_id=reserve_rs.res_id"
                                        + " AND reserve_rs.rsres_id=" + rmrsResId;
                        
                        final List recordsSql2 = retrieveDbRecords(context, sql);
                        
                        if (!recordsSql2.isEmpty()) {
                            final Map recordOfSql2 = (Map) recordsSql2.get(0);
                            valuesToMail.put("rsres_id", getString(recordOfSql2, "rsres_id"));
                            valuesToMail.put("user_requested_for",
                                getString(recordOfSql2, "user_requested_for"));
                            valuesToMail.put("date_start",
                                getDateValue(context, recordOfSql2.get("date_start")).toString());
                            valuesToMail.put("time_start",
                                getTimeValue(context, recordOfSql2.get("time_start")).toString());
                            valuesToMail.put("time_end",
                                getTimeValue(context, recordOfSql2.get("time_end")).toString());
                            valuesToMail.put("resource_id", getString(recordOfSql2, "resource_id"));
                            valuesToMail.put("quantity", getString(recordOfSql2, "quantity"));
                            allQueriesOk = true;
                        }
                    }
                    
                    // this.log.info("'"+ACTIVITY_ID+"-"+RULE_ID+"' [allOk]: "+allQueriesOk);
                    
                    if (allQueriesOk) {
                        // Get all mesages to Mail in a TreeMap (It is more easy to write)
                        final TreeMap messages = new TreeMap();
                        final int maxBody = 17;
                        messages.put(
                            "SUBJECT",
                            localizeMessage(context, ACTIVITY_ID, "NOTIFYAPPROVER_WFR",
                                "NOTIFYAPPROVER_SUBJECT", (String) valuesToMail.get("locale")));
                        for (int i = 1; i <= maxBody; i++) {
                            messages.put(
                                "BODY" + i,
                                localizeMessage(context, ACTIVITY_ID, "NOTIFYAPPROVER_WFR",
                                    "NOTIFYAPPROVER_BODY_PART" + i,
                                    (String) valuesToMail.get("locale")));
                        }
                        
                        // BEGIN: create message email
                        String subject = "";
                        String message = "";
                        
                        // BEGIN: subject
                        subject += messages.get("SUBJECT");
                        // END: subject
                        
                        // BEGIN: message
                        
                        message +=
                                (res_type.equals("room") ? messages.get("BODY1") : messages
                                    .get("BODY2")) + "\n\n";
                        
                        message += getWebCentralPath(context);
                        message +=
                                "/schema/ab-system/html/url-proxy.htm?viewName=ab-rr-approve-reservations.axvw";
                        message += "\n\n";
                        
                        message +=
                                (res_type.equals("room") ? messages.get("BODY3") : messages
                                    .get("BODY4")) + "\n\n";
                        
                        message +=
                                (res_type.equals("room") ? messages.get("BODY5") + " "
                                        + valuesToMail.get("rmres_id") : messages.get("BODY6")
                                        + " " + valuesToMail.get("rsres_id"))
                                        + "\n";
                        
                        message +=
                                messages.get("BODY7") + " " + valuesToMail.get("date_start") + "\n";
                        message +=
                                messages.get("BODY8") + " " + valuesToMail.get("time_start") + "\n";
                        message +=
                                messages.get("BODY9") + " " + valuesToMail.get("time_end") + "\n";
                        message +=
                                messages.get("BODY10") + " "
                                        + valuesToMail.get("user_requested_for") + "\n";
                        
                        if (res_type.equals("room")) {
                            message +=
                                    messages.get("BODY11") + " " + valuesToMail.get("bl_id") + "\n";
                            message +=
                                    messages.get("BODY12") + " " + valuesToMail.get("fl_id") + "\n";
                            message +=
                                    messages.get("BODY13") + " " + valuesToMail.get("rm_id") + "\n";
                            message +=
                                    messages.get("BODY14") + " " + valuesToMail.get("config_id")
                                            + "\n";
                            message +=
                                    messages.get("BODY15") + " "
                                            + valuesToMail.get("rm_arrange_type_id") + "\n\n";
                        } else {
                            message +=
                                    messages.get("BODY16") + " " + valuesToMail.get("resource_id")
                                            + "\n";
                            message +=
                                    messages.get("BODY17") + " " + valuesToMail.get("quantity")
                                            + "\n\n";
                        }
                        
                        message +=
                                getActivityParameterString(context, ACTIVITY_ID,
                                    "InternalServicesName");
                        
                        // END: message
                        
                        // END: Create message email
                        
                        // Get email, from and host
                        final String from =
                                getActivityParameterString(context, ACTIVITY_ID,
                                    "InternalServicesEmail");
                        final String host = getEmailHost(context);
                        final String port = getEmailPort(context);
                        final String userId = getEmailUserId(context);
                        final String password = getEmailPassword(context);
                        
                        // Send the email
                        try {
                            // PC changed to solve KB 3016618
                            this.sendEmail(message, from, host, port, subject,
                                (String) valuesToMail.get("approveremail"), null, null, userId,
                                password, null, CONTENT_TYPE_TEXT_UFT8, ACTIVITY_ID);
                        } catch (final Throwable e) {
                            // KB#3036675: only log the email notification error of notifyApprover
                            // during execution of schedule rule.
                            this.log.error(errMessage, e);
                        }
                    }
                } // END: In case that we have the email direction to notify
            }// END: Only enter if exists a room or resource reservation identifier and a user to
             // notify.
            
        } catch (final Throwable e) {
            // KB#3036675: only log the email notification error of notifyApprover during execution
            // of schedule rule.
            this.log.error(errMessage, e);
        }
        
    }
    
    // ---------------------------------------------------------------------------------------------
    // END notifyApprover
    // ---------------------------------------------------------------------------------------------
    
}// Class
