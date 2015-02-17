package com.archibus.app.reservation.service;

import java.sql.Time;
import java.text.*;
import java.util.*;

import com.archibus.app.reservation.ConfiguredDataSourceTestBase;
import com.archibus.app.reservation.domain.TimePeriod;
import com.archibus.datasource.data.DataRecord;
import com.archibus.utility.Utility;

/**
 * Base class for DataRecord based Reservation tests.
 *<p>
 * @author Yorik Gerlo
 * @since 21.2
 */
public abstract class AbstractReservationServiceTestBase extends ConfiguredDataSourceTestBase {
    
    /** Reservation type field in reserve table. */
    protected static final String RESERVE_RES_TYPE = "reserve.res_type";
    
    /** The bl id. */
    protected static final String BL_ID = "HQ";
    /** The fl id. */
    protected static final String FL_ID = "19";
    /** The rm id. */
    protected static final String RM_ID = "110";
    /** The arrange type id. */
    protected static final String ARRANGE_TYPE_ID = "THEATER";
    /** The config id. */
    protected static final String CONFIG_ID = "A1";
    
    /** A user name that exists in Web Central. */
    protected static final String USER_NAME = "AFM";
    
    /** Constant: 5. */
    protected static final int FIVE = 5;
    
    /** A full week has 7 days. */
    protected static final int DAYS_IN_WEEK = 7;

    /** The start time for testing. */
    protected Time startTime;
    /** The end time for testing. */
    protected Time endTime;
    /** The start date used for testing. */
    protected Date startDate;
    /** The end date used for testing. */
    protected Date endDate;

    /** The email. */
    protected String email = "tim.jones@procos1.onmicrosoft.com";
    /** The time formatter. */
    protected final SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.ENGLISH);
    
    /** The recurrence pattern. */
    protected String recurrencePattern = ""; 
    
    /**
     * Set up time values for a Room Reservation Service test.
     * 
     * @throws Exception when setup fails, e.g. because of bad time formatting
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(Utility.currentDate());
        cal.add(Calendar.DATE, DAYS_IN_WEEK);
        
        this.startDate = cal.getTime();
        this.startDate = new java.sql.Date(TimePeriod.clearTime(this.startDate).getTime());
        this.endDate = this.startDate;
        this.startTime = createTime("1899-12-30 09:00:00");
        this.endTime = createTime("1899-12-30 11:00:00");
    }
    
    /**
     * Create a time object representing the given time string.
     * @param formattedTime the time as 1899-12-30 HH:MM:SS
     * @return the time object
     * @throws ParseException when the parameter is an invalid time
     */
    protected Time createTime(final String formattedTime) throws ParseException {
        return new Time(this.timeFormatter.parse(formattedTime).getTime());
    }
    
    /**
     * Creates the reservation.
     * 
     * @param reservation the reservation record
     * @param recurrent the recurrent
     * @return the data record
     */
    protected DataRecord createReservation(final DataRecord reservation, final boolean recurrent) {
        if (recurrent) {             
            reservation
                .setValue("reserve.recurring_rule",
                    "<recurring type=\"week\" value1=\"0,1,0,0,0,0,0\" value2=\"1\" value3=\"\" total=\"6\"/>"); 
            reservation.setValue(RESERVE_RES_TYPE, "RECURRING");
            // modify the startDate/endDate to match the recurrence pattern
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.startDate);
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            this.startDate = calendar.getTime();
            calendar.add(Calendar.DATE, DAYS_IN_WEEK * FIVE);
            this.endDate = calendar.getTime();
        } else {
            reservation.setValue(RESERVE_RES_TYPE, "REGULAR");
        }
        
        reservation.setValue("reserve.date_start", this.startDate);
        reservation.setValue("reserve.date_end", this.endDate);
        reservation.setValue("reserve.time_start", this.startTime);
        reservation.setValue("reserve.time_end", this.endTime);
        reservation.setValue("reserve.email", this.email);
        reservation.setValue("reserve.reservation_name", "test name"); 
        reservation.setValue("reserve.user_created_by", USER_NAME);
        reservation.setValue("reserve.user_requested_for", USER_NAME);
        reservation.setValue("reserve.user_requested_by", USER_NAME);
        reservation.setValue("reserve.comments", "Comments for reservation created in test.");
        return reservation;
    }
}
