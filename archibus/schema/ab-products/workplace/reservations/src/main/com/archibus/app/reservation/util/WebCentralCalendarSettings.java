package com.archibus.app.reservation.util;

import org.json.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.IReservation;
import com.archibus.app.reservation.domain.recurrence.*;
import com.archibus.jobmanager.EventHandlerContext;


/**
 * Utility class for WebCentralCalendarService. Provides methods to prepare the event handler
 * context for sending invitations regarding a recurring reservation and holds configuration
 * properties.
 * 
 * This class contains one non-static method: getResourceAccount
 * Returns null, because this is only used for Exchange integration.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class WebCentralCalendarSettings implements ICalendarSettings {

    /** String value of zero. */
    private static final String STRING_ZERO = "0";

    /** The Constant STRING_ONE. */
    private static final String STRING_ONE = "1";
    
    /** The Constant INDEX_SUNDAY. */
    private static final int INDEX_SUNDAY = 0;

    /** The Constant INDEX_SATURDAY. */
    private static final int INDEX_SATURDAY = 6;
  
    /** Recurrence type. */
    private static final String RECUR_TYPE = "recur_type";

    /** Recurrence value 1. */
    private static final String RECUR_VAL1 = "recur_val1";

    /** Recurrence value 2. */
    private static final String RECUR_VAL2 = "recur_val2";
    
    /** Recurrence value 3. */
    private static final String RECUR_VAL3 = "recur_val3";
    
    /** Recurrence value 4. */
    private static final String RECUR_VAL4 = "recur_val4";

    /** The number of days in a week. */
    private static final int DAYS_IN_WEEK = 7;

    /** The right square bracket. */
    private static final String RIGHT_SQUARE_BRACKET = "]";

    /** The left square bracket. */
    private static final String LEFT_SQUARE_BRACKET = "[";

    /**
     * Prepare the event handler context for sending an invitation about a recurring reservation.
     * 
     * @param reservation the reservation to send invites for
     * @param context the event handler context to populate
     * @param json the JSON container to populate with recurrence info
     */
    public static void prepareRecurringInvitations(final IReservation reservation,
            final EventHandlerContext context, final JSONObject json) {
        final Recurrence recurrence =
                Recurrence.parseRecurrence(reservation.getStartDate(), reservation.getEndDate(),
                        reservation.getRecurringRule());

        // recurrence.getNumberOfOccurrences();
        if (recurrence instanceof DailyPattern) {
            final DailyPattern pattern = (DailyPattern) recurrence;
            json.put(RECUR_TYPE, "day");
            json.put(RECUR_VAL1, LEFT_SQUARE_BRACKET + pattern.getInterval() + RIGHT_SQUARE_BRACKET);
            json.put(RECUR_VAL2, "");
            json.put(RECUR_VAL3, pattern.getInterval());
            
        } else if (recurrence instanceof WeeklyPattern) {
            final WeeklyPattern pattern = (WeeklyPattern) recurrence;
            json.put(RECUR_TYPE, "week");
            // "recur_val1":[null,null,null,"3",null,null,null], "recur_val2":[null] (SUN =
            // 0, SA = 6)

            final JSONArray recurVal1 = new JSONArray();

            for (int i = 0; i < DAYS_IN_WEEK; i++) {
                recurVal1.put("null");
            }

            // update the week days
            setRecurrenceValue1(pattern, recurVal1);

            json.put(RECUR_VAL1, recurVal1.toString());
            json.put(RECUR_VAL2, "[null]");
            json.put(RECUR_VAL3, pattern.getInterval());
            
        } else if (recurrence instanceof MonthlyPattern) {
            prepareMonthlyInvitations((MonthlyPattern) recurrence, json);
        } else if (recurrence instanceof YearlyPattern) {    
            prepareYearlyInvitations((YearlyPattern) recurrence, json);
        } else {
            json.put(RECUR_TYPE, "");
            json.put(RECUR_VAL1, "");
            json.put(RECUR_VAL2, ""); 
        }

        // when recurring reservation, we have to pass zero 
        context.addResponseParameter(Constants.RES_ID, STRING_ZERO);
          
        if (reservation.getParentId() == null) {
            context.addResponseParameter(Constants.RES_PARENT, reservation.getReserveId()
                    .toString());
        } else {
            context.addResponseParameter(Constants.RES_PARENT, reservation.getParentId()
                    .toString());
        }
    }

    /**
     * Put the ics properties for monthly invitations in the json object.
     * 
     * @param pattern the recurrence pattern
     * @param json the context object
     */
    private static void prepareMonthlyInvitations(final MonthlyPattern pattern,
            final JSONObject json) {
        json.put(RECUR_TYPE, "month");
        // "recur_val1":["2"],"recur_val2":["3"], recur_val2=day (SUN = 0, SA = 6)
        // recur_val1=week of month
        
        // if occurrence is on day of week              
        if (pattern.getWeekOfMonth() != null) {
            json.put(RECUR_VAL1, LEFT_SQUARE_BRACKET + pattern.getWeekOfMonth().intValue()
                    + RIGHT_SQUARE_BRACKET);
            
            // starts at 1 for Sunday
            final int dayIndex = pattern.getDayOfTheWeek().getIntValue() - 1;
            json.put(RECUR_VAL2, LEFT_SQUARE_BRACKET + dayIndex
                    + RIGHT_SQUARE_BRACKET);
        }

        // if occurrence is on day of month    
        if (pattern.getDayOfMonth() != null) {
            json.put(RECUR_VAL1, LEFT_SQUARE_BRACKET + pattern.getDayOfMonth().intValue()
                    + RIGHT_SQUARE_BRACKET);
        }
        
        // recur_value3 is always the interval
        json.put(RECUR_VAL3, pattern.getInterval());
    }

    /**
     * Put the ics properties for yearly invitations in the json object.
     * 
     * @param pattern the recurrence pattern
     * @param json the context object
     */
    private static void prepareYearlyInvitations(final YearlyPattern pattern, final JSONObject json) {
        json.put(RECUR_TYPE, "year");
        
        // if occurrence is on day of week in a specific month
        if (pattern.getWeekOfMonth() != null) {
            json.put(RECUR_VAL1, LEFT_SQUARE_BRACKET + pattern.getWeekOfMonth().intValue()
                    + RIGHT_SQUARE_BRACKET);
            
            // starts at 1 for Sunday
            final int dayIndex = pattern.getDayOfTheWeek().getIntValue() - 1;
            json.put(RECUR_VAL2, LEFT_SQUARE_BRACKET + dayIndex + RIGHT_SQUARE_BRACKET);
            
            final int monthValue = pattern.getMonth().getIntValue() + 1;
            json.put(RECUR_VAL4, LEFT_SQUARE_BRACKET + monthValue + RIGHT_SQUARE_BRACKET);
        }
        
        // if occurrence is on day of month
        if (pattern.getDayOfMonth() != null) {
            json.put(RECUR_VAL1, LEFT_SQUARE_BRACKET + pattern.getDayOfMonth().intValue()
                + RIGHT_SQUARE_BRACKET);
            
            final int monthValue = pattern.getMonth().getIntValue() + 1;
            json.put(RECUR_VAL2, LEFT_SQUARE_BRACKET + monthValue + RIGHT_SQUARE_BRACKET);
        }
        // recur_value3 is always the interval
        json.put(RECUR_VAL3, pattern.getInterval());
    }

   
    /**
     * Prepare the event handler context for sending an invitation about a single reservation.
     * 
     * @param reservation the reservation to send invites for
     * @param context the event handler context to populate
     * @param json the JSON container to populate with additional info
     */
    public static void prepareSingleInvitation(final IReservation reservation,
            final EventHandlerContext context, final JSONObject json) {
        // no recurrence
        json.put(RECUR_TYPE, "");
        json.put(RECUR_VAL1, "");
        json.put(RECUR_VAL2, "");

        context.addResponseParameter(Constants.RES_ID, reservation.getReserveId().toString());
        if (reservation.getParentId() == null) {
            context.addResponseParameter(Constants.RES_PARENT, STRING_ZERO);
        } else {
            //bv when editing recurrence, have to keep the reference to the parent for the UID
            context.addResponseParameter(Constants.RES_PARENT, reservation.getParentId().toString());
        }
    }

    /**
     * {@inheritDoc} This type of calendar doesn't use a resource account.
     */
    public String getResourceAccount() {
        return null;
    }
    
    
    /**
     * Sets the recurrence value1.
     *
     * @param pattern the weekly pattern
     * @param recurVal1 the array of the days to be set, starting from Sunday as first index.
     */
    private static void setRecurrenceValue1(final WeeklyPattern pattern, final JSONArray recurVal1) {
        for (final DayOfTheWeek dayOfWeek : pattern.getDaysOfTheWeek()) {
            // intValue starts at Sunday = 1                
            if (dayOfWeek.equals(DayOfTheWeek.Day)) {
                // all days
                for (int i = 0; i < DAYS_IN_WEEK; i++) {
                    recurVal1.put(STRING_ONE);
                }
            } else if (dayOfWeek.equals(DayOfTheWeek.Weekday)) {
                for (int i = 1; i < DAYS_IN_WEEK - 1; i++) {
                    recurVal1.put(i, STRING_ONE);
                }
            } else if (dayOfWeek.equals(DayOfTheWeek.WeekendDay)) {
                // (SUN = 0, SA = 6)
                recurVal1.put(INDEX_SUNDAY, STRING_ONE);
                recurVal1.put(INDEX_SATURDAY, STRING_ONE);
            } else {
                // starts at 1
                final int value = dayOfWeek.getIntValue() - 1;
                recurVal1.put(value, String.valueOf(value));
            }
        }
    }


}
