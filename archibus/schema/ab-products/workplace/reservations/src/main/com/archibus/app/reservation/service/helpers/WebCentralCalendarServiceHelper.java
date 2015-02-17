package com.archibus.app.reservation.service.helpers;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.archibus.app.reservation.domain.IReservation;
import com.archibus.app.reservation.domain.RoomAllocation;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.util.ReservationsContextHelper;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.LocalDateTimeUtil;
import com.archibus.utility.StringUtil;

/**
 * The Class WebCentralCalendarServiceHelper.
 * 
 * This class contains static methods only.
 */
public final class WebCentralCalendarServiceHelper {     

    /** The Constant ORIGINAL_DATE. */
    private static final String ORIGINAL_DATE = "original_date";

    /** The Constant ORIGINAL_TIME_END. */
    private static final String ORIGINAL_TIME_END = "original_time_end";

    /** The Constant ORIGINAL_TIME_START. */
    private static final String ORIGINAL_TIME_START = "original_time_start";
    
    /** The Constant ORIGINAL_CITY_TIMEZONE. */
    private static final String ORIGINAL_CITY_TIMEZONE = "original_cityTimezone";

    /** The Constant ISO_DATE_FORMAT. */
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    
    /** The Constant TIME_FORMAT. */
    private static final String TIME_FORMAT = "HH:mm:ss"; 
  
    /**
     * Prevent instantiation of a new web central calendar service helper.
     */
    private WebCentralCalendarServiceHelper() {  
    } 

    /**
     * Sets the result message.
     *
     * @param context the context
     * @param resultMessage the result message
     */
    public static void setResultMessage(final EventHandlerContext context,
            final String resultMessage) {
        if (StringUtil.notNullOrEmpty(resultMessage)
                && context.parameterExistsNotEmpty(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER)) {
            // Check whether the result message has changed.
            final String newResultMessage = context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER);
            if (!resultMessage.equals(newResultMessage)) {
                // Concatenate the two messages, separated by newline.
                context.addResponseParameter(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER, resultMessage + '\n'
                        + newResultMessage);
            }
        }
    }  
    
    /**
     * Adds the response parameters update.
     *
     * @param reservation the new / modified reservation reservation
     * @param orginalReservation the original reservation
     * @param context the context
     */
    public static void addResponseParametersUpdate(final IReservation reservation,
            final IReservation orginalReservation, final EventHandlerContext context) {
        // when updating occurrences, we need the original date and time to cancel
        if (orginalReservation != null && reservation.getRecurringDateModified() == 1) {
            // don't include when the date didn't change (no cancel .ics will be generated)     
            context.addResponseParameter(ORIGINAL_DATE,
                    getDateFormatted(orginalReservation.getStartDate()));
            
            context.addResponseParameter(ORIGINAL_TIME_START,
                    getTimeFormatted(orginalReservation.getStartTime()));  
              
            context.addResponseParameter(ORIGINAL_TIME_END,
                    getTimeFormatted(orginalReservation.getEndTime()));
            
            // make sure the time zone is set
            if (StringUtil.notNullOrEmpty(orginalReservation.getTimeZone())) {
                context.addResponseParameter(ORIGINAL_CITY_TIMEZONE, orginalReservation.getTimeZone());
            } else {
                // lookup the time zone
                lookupTimeZone(orginalReservation, context);
            }  
           // System.err.println("Setting orginal reservation");
        } else {
            context.removeResponseParameter(ORIGINAL_DATE);
            context.removeResponseParameter(ORIGINAL_TIME_START);
            context.removeResponseParameter(ORIGINAL_TIME_END);
            context.removeResponseParameter(ORIGINAL_CITY_TIMEZONE);
          //  System.err.println("Clearing orginal reservation");
        }
        
    } 
    
    /**
     * Gets the date formatted.
     *
     * @param date the date
     * @return the date formatted
     */
    public static String getDateFormatted(final Date date) {
        /** For formatting date strings. */
        final DateFormat dateFormatter = new SimpleDateFormat(ISO_DATE_FORMAT, Locale.ENGLISH);

        return dateFormatter.format(date);        
    }
    
    /**
     * Gets the time formatted.
     *
     * @param time the time
     * @return the time formatted
     */
    public static String getTimeFormatted(final Time time) {
        /** For formatting time string. */
        final DateFormat timeFormatter = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH); 
   
        return timeFormatter.format(time);        
    }
    
    /**
     * Lookup time zone.
     *
     * @param reservation the reservation
     * @param context the context
     */
    private static void lookupTimeZone(final IReservation reservation,
            final EventHandlerContext context) {
        if (reservation instanceof RoomReservation) {
            final RoomReservation roomReservation = (RoomReservation) reservation;

            if (!roomReservation.getRoomAllocations().isEmpty()) {
                // get the first room
                final RoomAllocation roomAllocation =
                        roomReservation.getRoomAllocations().get(0);
                // get the time zone
                final String originalCityTimeZoneId =
                        LocalDateTimeUtil.getLocationTimeZone(null, null, null,
                                roomAllocation.getBlId());
                // add parameter
                context.addResponseParameter(ORIGINAL_CITY_TIMEZONE,
                        originalCityTimeZoneId);

                reservation.setTimeZone(originalCityTimeZoneId);
            }
        }
    } 
    
    /**
     * Check result message.
     *
     * @param context the context
     * @return the string
     */
    public static String checkResultMessage(final EventHandlerContext context) {
        String resultMessage = null;
        if (context.parameterExistsNotEmpty(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER)) {
            resultMessage = context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER);
        }
        return resultMessage;
    }

}
