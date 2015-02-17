package com.archibus.eventhandler.reservations;

import java.io.*;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import com.archibus.eventhandler.EventHandlerBase;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.ExceptionBase;
import com.archibus.utility.StringUtil;

public class ReservationsEventHandlerBase extends EventHandlerBase {

    protected static Logger Classlog = Logger.getLogger(ReservationsEventHandlerBase.class);

    static final String ACTIVITY_ID = "AbWorkplaceReservations";

    /**
     * Put all messages of mail in a treemap
     * 
     * @param context
     * @param Std : it can be "By" or "For" only
     * @param locale : locale of user
     * @return TreeMap with messages
     */
    public TreeMap getMailMessages(EventHandlerContext context, String Std, String locale) {
        TreeMap messages = new TreeMap();
        int maxSubject = 4;
        int maxBody = 13;
        Std = Std.toUpperCase();
        for (int i = 1; i <= maxSubject; i++) {
            messages.put("SUBJECT" + i, localizeMessage(context, ACTIVITY_ID, "NOTIFYREQUESTED"
                    + Std + "_WFR", "NOTIFYREQUESTED" + Std + "_SUBJECT_PART" + i, locale));
        }
        for (int i = 1; i <= maxBody; i++) {
            messages.put("BODY" + i, localizeMessage(context, ACTIVITY_ID, "NOTIFYREQUESTED" + Std
                    + "_WFR", "NOTIFYREQUESTED" + Std + "_BODY_PART" + i, locale));
        }

        messages.put("BODY11_2", localizeMessage(context, ACTIVITY_ID, "NOTIFYREQUESTED" + Std
                + "_WFR", "NOTIFYREQUESTED" + Std + "_BODY_PART11_2", locale));
       
        //EC - KB 3040163 - use other message content based on reservation status
        messages.put("BODY_PART2_CANCEL", localizeMessage(context,ACTIVITY_ID, "NOTIFYREQUESTED" + Std+ "_WFR","NOTIFYREQUESTED" + Std + "_BODY_PART2_CANCEL",locale));
        messages.put("BODY_PART2_REJECT", localizeMessage(context,ACTIVITY_ID, "NOTIFYREQUESTED" + Std+ "_WFR","NOTIFYREQUESTED" + Std + "_BODY_PART2_REJECT",locale));

        return messages;
    }

    /**
     * create a attachment cite for a mail
     * 
     * @param parametersValues configuration parameters
     * @return String with route of created file.
     */
    public String createAttachments(EventHandlerContext context, TreeMap parametersValues) {

        String result = "";
        String errMessage = localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                            "SENDEMAILINVITATIONSERROR", null);
        final String RULE_ID = "createAttachments";

        try {

            Date curDateTime;
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
            String line = "";
            
            //kb#3034925: change encoding of ics file from default ansi to utf-8
            /*File file = new File((String) parametersValues.get("path"), (String) parametersValues
                    .get("filename"));
            BufferedWriter out = new BufferedWriter(new FileWriter(file));*/
            String outfilename = (String) (parametersValues.get("path") + File.separator + (String) parametersValues.get("filename"));
            FileOutputStream file = new FileOutputStream(outfilename);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(file, "UTF-8"));

            line = "BEGIN:VCALENDAR";
            out.write(line);
            out.newLine();

            line = "PRODID:-//hacksw/handcal//NONSGML v1.0//EN";
            out.write(line);
            out.newLine();

            line = "VERSION:2.0";
            out.write(line);
            out.newLine();

            line = "METHOD:" + (String) parametersValues.get("method");
            out.write(line);
            out.newLine();

            line = "BEGIN:VEVENT";
            out.write(line);
            out.newLine();

            line = (String) parametersValues.get("attendeesSection");
            out.write(line);
            out.newLine();

            line = "ORGANIZER:MAILTO:" + (String) parametersValues.get("mailTo");
            out.write(line);
            out.newLine();

            line = "DTSTART:" + (String) parametersValues.get("dateStart") + "T"
                    + (String) parametersValues.get("timeStart") + "Z";
            out.write(line);
            out.newLine();

            line = "DTEND:" + (String) parametersValues.get("dateEnd") + "T"
                    + (String) parametersValues.get("timeEnd") + "Z";
            out.write(line);
            out.newLine();

            if (parametersValues.containsKey("rruleFreq")) {
                line = "RRULE:FREQ=" + (String) parametersValues.get("rruleFreq");
                line += ";UNTIL=" + (String) parametersValues.get("rruleUntil");
                line += ";INTERVAL=" + (String) parametersValues.get("rruleInternal");
                if (parametersValues.containsKey("rruleBySetPos")) {
                    line += ";BYSETPOS=" + (String) parametersValues.get("rruleBySetPos");
                }
                if (parametersValues.containsKey("rruleByDay")) {
                    line += ";BYDAY=" + (String) parametersValues.get("rruleByDay");
                }
                //BV 
                if (parametersValues.containsKey("rruleByMonth")) {
                    if (parametersValues.containsKey("rruleByMonthDay")) {
                        // this is yearly pattern on specific date
                        // RRULE:FREQ=YEARLY;COUNT=5;BYMONTHDAY=5;BYMONTH=6
                        line += ";BYMONTHDAY=" + (String) parametersValues.get("rruleByMonthDay");
                        line += ";BYMONTH=" + (String) parametersValues.get("rruleByMonth");
                    } else {
                        // this is yearly pattern on a day of the week
                        line += ";BYMONTH=" + (String) parametersValues.get("rruleByMonth");
                    }                    
                } 
                // 
                
                line += ";WKST=" + (String) parametersValues.get("WKST");
                out.write(line);
                out.newLine();
            }
            if (parametersValues.containsKey("exDate")) {
                line = "EXDATE:" + (String) parametersValues.get("exDate");
                out.write(line);
                out.newLine();
            }

            if (parametersValues.containsKey("sequence")) {
                line = "SEQUENCE:" + (String) parametersValues.get("sequence");
                out.write(line);
                out.newLine();
            }
            line = "UID:" + (String) parametersValues.get("uid");
            out.write(line);
            out.newLine();

            if (parametersValues.containsKey("recurrence-id")) {
                line = "RECURRENCE-ID:" + (String) parametersValues.get("recurrence-id");
                out.write(line);
                out.newLine();
            }
            line = "LOCATION:" + (String) parametersValues.get("location");
            out.write(line);
            out.newLine();

            curDateTime = new Date();
            long time = curDateTime.getTime();
            int minutesoffset = curDateTime.getTimezoneOffset();
            DecimalFormat timeZoneFormatter = new DecimalFormat("00");
            String TimeZone = (minutesoffset / 60) < 0 ? "+"
                    + timeZoneFormatter.format(-minutesoffset / 60) : timeZoneFormatter
                    .format(-minutesoffset / 60);
            int absOffset = (minutesoffset > 0 ? minutesoffset : -minutesoffset);
            TimeZone += (absOffset % 60) > 0 ? ":" + timeZoneFormatter.format(absOffset % 60) : "";
            time += minutesoffset * 60 * 1000;
            curDateTime.setTime(time);
            line = "DTSTAMP:" + dateFormatter.format(curDateTime) + "T"
                    + timeFormatter.format(curDateTime);
            out.write(line);
            out.newLine();

            line = "SUMMARY:" + (String) parametersValues.get("summary");
            out.write(line);
            out.newLine();

            line = "DESCRIPTION:" + (String) parametersValues.get("description");
            out.write(line);
            out.newLine();

            line = "CLASS:PUBLIC";
            out.write(line);
            out.newLine();

            line = "END:VEVENT";
            out.write(line);
            out.newLine();

            line = "END:VCALENDAR";
            out.write(line);
            out.newLine();

            //kb#3034925: change encoding of ics file from default ansi to utf-8
            out.flush();
            
            out.close();

            //result = file.getAbsolutePath();
            result = outfilename;

        } catch (Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed creating attachments "
                    + e.getMessage(), errMessage, e);
            // log.info(ACTIVITY_ID+"-createAttachments: "+e);
        }

        return result;

    }

    /**
     * Put all mesages of mail for send mail in a treemap
     * 
     * @param context
     * @param locale : locale of user
     * @return TreeMap with messages
     */
    public TreeMap getSendMailMessages(EventHandlerContext context, String locale) {
        TreeMap messages = new TreeMap();
        int maxSubject = 4;
        int maxBody = 8;
        for (int i = 1; i <= maxSubject; i++) {
            messages.put("SUBJECT" + i, localizeMessage(context, ACTIVITY_ID,
                                                        "SENDEMAILINVITATIONS_WFR",
                                                        "SENDEMAILINVITATIONS_SUBJECT_PART" + i,
                                                        locale));
        }
        for (int i = 1; i <= maxBody; i++) {
            messages.put("BODY" + i, localizeMessage(context, ACTIVITY_ID,
                                                     "SENDEMAILINVITATIONS_WFR",
                                                     "SENDEMAILINVITATIONS_BODY_PART" + i, locale));
        }

        messages.put("BODY1_2", localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                                "SENDEMAILINVITATIONS_BODY_PART1_2", locale));
        messages.put("BODY1_3", localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                                "SENDEMAILINVITATIONS_BODY_PART1_3", locale));
        messages.put("BODY2_2", localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                                "SENDEMAILINVITATIONS_BODY_PART2_2", locale));
        messages.put("BODY2_3", localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                                "SENDEMAILINVITATIONS_BODY_PART2_3", locale));
        messages.put("BODY6_2", localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                                "SENDEMAILINVITATIONS_BODY_PART6_2", locale));

        return messages;
    }

    /**
     * This function will log the error and throw a new Exception with the desired description
     * 
     * @param context
     * @param logMessage
     * @param exceptionMessage
     * @param originalException
     * @return void
     */
    protected static void handleError(EventHandlerContext context, String logMessage,
            String exceptionMessage, Throwable originalException) {
        context.addResponseParameter("message", exceptionMessage);
        throw new ExceptionBase(null, exceptionMessage, originalException);
    }
    
    
    /**
     * This function will store the error of Email Notification to context with the desired description
     * 
     * @param context
     * @param logMessage
     * @param exceptionMessage
     * @param originalException
     * @param address
     * @return void
     */
    protected static void handleNotificationError(EventHandlerContext context, String logMessage,
            String exceptionMessage, Throwable originalException, String address) {                        
        String errorMessage;
        if(StringUtil.notNullOrEmpty(address)) {
            errorMessage = address+": "+exceptionMessage;
        } else {
            
            errorMessage = exceptionMessage;
        }

        context.addResponseParameter("message", errorMessage);
    }
    
    /**
     * return a value from Map. If this value not exist, return empty
     * 
     * @param record
     * @param name
     * @return String
     */
    protected static String getString(Map record, String name) {
        String s = (String) record.get(name);
        if (s == null) {
            s = "";
        }
        return s;
    }

    /**
     * Changing HH:MM PM and am format into HH:MM:SS format
     * 
     * @param date
     * @return String
     */
    protected static String transformDate(String date) {
        String result = date;
        if (date.toUpperCase().indexOf("AM") > -1 || date.toUpperCase().indexOf("PM") > -1) {

            String hour = date.substring(0, date.indexOf(":"));
            String minute = date.substring(date.indexOf(":") + 1, date.indexOf(" "));
            if (date.indexOf("AM") > -1) {
                hour = (hour.equals("12") ? "00" : hour);
            }
            if (date.indexOf("PM") > -1) {
                hour = (hour.equals("12") ? hour : String.valueOf(Integer.parseInt(hour) + 12));
            }
            result = hour + ":" + minute + ":00";
        }
        return result;
    }

    /*
     * This function transform a String into a Time, with the correct format @param t @return Time
     */
    protected static Time getTimeFromString(String t) {
        String[] l1 = t.split(":");
        int h1 = new Integer(l1[0].toString()).intValue();
        int m1 = new Integer(l1[1].toString().substring(0, 2)).intValue();
        Time d1 = new Time(h1, m1, 0);

        return d1;
    }
    
    //kb#3035551: add different ics file to email attachment for requested_by 
    public String createAttachments_requestedBy(EventHandlerContext context, TreeMap parametersValues) {

        String result = "";
        String errMessage = localizeMessage(context, ACTIVITY_ID, "SENDEMAILINVITATIONS_WFR",
                                            "SENDEMAILINVITATIONSERROR", null);
        final String RULE_ID = "createAttachments";

        try {

            Date curDateTime;
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
            String line = "";
            
            //kb#3034925: change encoding of ics file from default ansi to utf-8
            /*File file = new File((String) parametersValues.get("path"), (String) parametersValues
                    .get("filename_requestedBy"));
            BufferedWriter out = new BufferedWriter(new FileWriter(file));*/
            String outfilename = (String) (parametersValues.get("path") + (String) parametersValues.get("filename_requestedBy"));
            FileOutputStream file = new FileOutputStream(outfilename);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(file, "UTF-8"));
            //end kb3034925

            line = "BEGIN:VCALENDAR";
            out.write(line);
            out.newLine();

            line = "PRODID:-//hacksw/handcal//NONSGML v1.0//EN";
            out.write(line);
            out.newLine();

            line = "VERSION:2.0";
            out.write(line);
            out.newLine();

            line = "METHOD:" + (String) parametersValues.get("method");
            out.write(line);
            out.newLine();

            line = "BEGIN:VEVENT";
            out.write(line);
            out.newLine();

            line = (String) parametersValues.get("attendeesSection_requestedBy");
            out.write(line);
            out.newLine();
            
            line = "ORGANIZER:MAILTO:"+ getActivityParameterString(context, ACTIVITY_ID, "InternalServicesEmail");
            out.write(line);
            out.newLine();

            line = "DTSTART:" + (String) parametersValues.get("dateStart") + "T"
                    + (String) parametersValues.get("timeStart") + "Z";
            out.write(line);
            out.newLine();

            line = "DTEND:" + (String) parametersValues.get("dateEnd") + "T"
                    + (String) parametersValues.get("timeEnd") + "Z";
            out.write(line);
            out.newLine();

            if (parametersValues.containsKey("rruleFreq")) {
                line = "RRULE:FREQ=" + (String) parametersValues.get("rruleFreq");
                line += ";UNTIL=" + (String) parametersValues.get("rruleUntil");
                line += ";INTERVAL=" + (String) parametersValues.get("rruleInternal");
                if (parametersValues.containsKey("rruleBySetPos")) {
                    line += ";BYSETPOS=" + (String) parametersValues.get("rruleBySetPos");
                }
                if (parametersValues.containsKey("rruleByDay")) {
                    line += ";BYDAY=" + (String) parametersValues.get("rruleByDay");
                }
                line += ";WKST=" + (String) parametersValues.get("WKST");
                out.write(line);
                out.newLine();
            }
            if (parametersValues.containsKey("exDate")) {
                line = "EXDATE:" + (String) parametersValues.get("exDate");
                out.write(line);
                out.newLine();
            }

            if (parametersValues.containsKey("sequence")) {
                line = "SEQUENCE:" + (String) parametersValues.get("sequence");
                out.write(line);
                out.newLine();
            }
            line = "UID:" + (String) parametersValues.get("uid");
            out.write(line);
            out.newLine();

            if (parametersValues.containsKey("recurrence-id")) {
                line = "RECURRENCE-ID:" + (String) parametersValues.get("recurrence-id");
                out.write(line);
                out.newLine();
            }
            line = "LOCATION:" + (String) parametersValues.get("location");
            out.write(line);
            out.newLine();

            curDateTime = new Date();
            long time = curDateTime.getTime();
            int minutesoffset = curDateTime.getTimezoneOffset();
            DecimalFormat timeZoneFormatter = new DecimalFormat("00");
            String TimeZone = (minutesoffset / 60) < 0 ? "+"
                    + timeZoneFormatter.format(-minutesoffset / 60) : timeZoneFormatter
                    .format(-minutesoffset / 60);
            int absOffset = (minutesoffset > 0 ? minutesoffset : -minutesoffset);
            TimeZone += (absOffset % 60) > 0 ? ":" + timeZoneFormatter.format(absOffset % 60) : "";
            time += minutesoffset * 60 * 1000;
            curDateTime.setTime(time);
            line = "DTSTAMP:" + dateFormatter.format(curDateTime) + "T"
                    + timeFormatter.format(curDateTime);
            out.write(line);
            out.newLine();

            line = "SUMMARY:" + (String) parametersValues.get("summary");
            out.write(line);
            out.newLine();

            line = "DESCRIPTION:" + (String) parametersValues.get("description");
            out.write(line);
            out.newLine();

            line = "CLASS:PUBLIC";
            out.write(line);
            out.newLine();

            line = "END:VEVENT";
            out.write(line);
            out.newLine();

            line = "END:VCALENDAR";
            out.write(line);
            out.newLine();

            //kb#3034925: change encoding of ics file from default ansi to utf-8
            out.flush();
            
            out.close();

            result = outfilename;

        } catch (Throwable e) {
            handleError(context, ACTIVITY_ID + "-" + RULE_ID + ": Failed creating attachments "
                    + e.getMessage(), errMessage, e);
            // log.info(ACTIVITY_ID+"-createAttachments: "+e);
        }

        return result;
    }
    

}
