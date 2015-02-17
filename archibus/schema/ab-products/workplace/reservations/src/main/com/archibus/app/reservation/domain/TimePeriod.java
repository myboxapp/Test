package com.archibus.app.reservation.domain;

import java.sql.Time;
import java.util.*;

import javax.xml.bind.annotation.XmlRootElement;

import com.archibus.app.reservation.dao.datasource.Constants;

/**
 * Time Period.
 * 
 * Represents the time period for a reservation, having a start and end.
 * 
 * @author Bart Vanderschoot
 * 
 */
@XmlRootElement(name = "TimePeriod")
public class TimePeriod {

    /** minute in milliseconds. */
    public static final int MINUTE_MILLISECONDS = 60 * 1000;

    /** hour in milliseconds. */
    public static final int HOUR_MILLISECONDS = 60 * 60 * 1000;

    /** day in milliseconds. */
    public static final int DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

    /** end date. */
    private Date endDate;

    /** end time. */
    private Time endTime;

    /** start date. */
    private Date startDate;

    /** start time. */
    private Time startTime;

    /** The time zone. */
    private String timeZone;

    /**
     * Default constructor.
     */
    public TimePeriod() {
        super();
    }

    /**
     * Constructor with parameters.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param startTime start time
     * @param endTime end time
     */
    public TimePeriod(final Date startDate, final Date endDate, final Time startTime,
            final Time endTime) {
        this(startDate, endDate, startTime, endTime, null);
    }

    /**
     * Constructor with parameters.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param startTime start time
     * @param endTime end time
     * @param timeZone the time zone
     */
    public TimePeriod(final Date startDate, final Date endDate, final Time startTime,
            final Time endTime, final String timeZone) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeZone = timeZone;
        this.validate();
    }

    /**
     * Create a time period with given start date, time and duration.
     * 
     * @param startDate start date
     * @param startTime start time
     * @param durationMinutes duration in minutes
     * @param timeZone the time zone
     */
    public TimePeriod(final Date startDate, final Time startTime, final int durationMinutes,
            final String timeZone) {
        super();
        this.startDate = startDate;
        this.startTime = startTime;
        this.timeZone = timeZone;

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.getStartDateTime());
        calendar.add(Calendar.MINUTE, durationMinutes);
        this.setEndDateTime(calendar.getTime());
    }
    
    /**
     * Create a new Time Period from date-time values.
     * @param startDateTime start date and time
     * @param endDateTime end date and time
     * @param timeZoneId time zone identifier
     */
    public TimePeriod(final Date startDateTime, final Date endDateTime, final String timeZoneId) {
        this.setStartDateTime(startDateTime);
        this.setEndDateTime(endDateTime);
        this.setTimeZone(timeZoneId);
    }

    /**
     * Copy constructor.
     * 
     * @param timePeriod the time period to copy
     */
    public TimePeriod(final TimePeriod timePeriod) {
        this(timePeriod.getStartDate(), timePeriod.getEndDate(), timePeriod.getStartTime(),
                timePeriod.getEndTime(), timePeriod.getTimeZone());
    }

    // Disable StrictDuplicate CHECKSTYLE warning. Justification: this class has common properties.
    /**
     * Get end date.
     * 
     * @return end date
     */
    public final Date getEndDate() {
        return this.endDate;
    }

    /**
     * Get end time.
     * 
     * @return end time
     */
    public final Time getEndTime() {
        return this.endTime;
    }

    /**
     * Get start date.
     * 
     * @return start date
     */
    public final Date getStartDate() {
        return this.startDate;
    }

    /**
     * Get start time.
     * 
     * @return start time
     */
    public final Time getStartTime() {
        return this.startTime;
    }

    /**
     * Set end date.
     * 
     * @param endDate end date
     */
    public final void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Set end time.
     * 
     * @param endTime end time
     */
    public final void setEndTime(final Time endTime) {
        this.endTime = endTime;
    }

    /**
     * Set start date.
     * 
     * @param startDate start date
     */
    public final void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Sets the start time.
     * 
     * @param startTime start time
     */
    public final void setStartTime(final Time startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the time zone.
     * 
     * @return the time zone
     */
    public final String getTimeZone() {
        return this.timeZone;
    }

    /**
     * Sets the time zone.
     * 
     * @param timeZone the new time zone
     */
    public final void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Sets the start date and time.
     * 
     * @param startDateTime the new start date/time
     */
    public final void setStartDateTime(final Date startDateTime) {
        if (startDateTime == null) {
            this.setStartDate(null);
            this.setStartTime(null);
        } else {
            this.setStartDate(TimePeriod.clearTime(startDateTime));
            this.setStartTime(new Time(TimePeriod.clearDate(startDateTime).getTime()));
        }
    }

    /**
     * Get the start date/time. This is null if either start date or start time is null.
     * 
     * @return start date/time.
     */
    public final Date getStartDateTime() {
        Date startDateTime = null;
        if (this.startTime != null && this.startDate != null) {
            final Calendar time = Calendar.getInstance();
            time.setTime(this.startTime);

            final Calendar cal = Calendar.getInstance();
            cal.setTime(this.startDate);
            cal.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            cal.set(Calendar.SECOND, time.get(Calendar.SECOND));

            startDateTime = cal.getTime();
        }
        return startDateTime;
    }

    /**
     * Get the end date/time. This is null if either end date or end time is null.
     * 
     * @return end date/time.
     */
    public final Date getEndDateTime() {
        Date endDateTime = null;
        if (this.endTime != null && this.endDate != null) {
            final Calendar time = Calendar.getInstance();
            time.setTime(this.endTime);

            final Calendar cal = Calendar.getInstance();
            cal.setTime(this.endDate);
            cal.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            cal.set(Calendar.SECOND, time.get(Calendar.SECOND));
            endDateTime = cal.getTime();
        }

        return endDateTime;
    }

    /**
     * Sets the end time.
     * 
     * @param endDateTime the new end time
     */
    public final void setEndDateTime(final Date endDateTime) {
        if (endDateTime == null) {
            this.setEndDate(null);
            this.setEndTime(null);
        } else {
            this.setEndDate(TimePeriod.clearTime(endDateTime));
            this.setEndTime(new Time(TimePeriod.clearDate(endDateTime).getTime()));
        }
    }

    /**
     * Clear date.
     * 
     * @param date the date
     * @return the date
     */
    public static Date clearDate(final Date date) {
        Date result = null;
        if (date != null)  {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date.getTime());
            cal.set(Calendar.YEAR, Constants.INIT_YEAR);
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DATE, Constants.INIT_DATE);

            result = cal.getTime(); 
        }

        return result;
    }

    /**
     * Clear time.
     * 
     * @param date the date
     * @return the date
     */
    public static Date clearTime(final Date date) {
        Date result = null;
        if (date != null)  {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date.getTime());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            // cal.setTimeZone(TimeZone.getTimeZone("UTC"));

            result = cal.getTime(); 
        }

        return result;
    }

    /**
     * Copy properties from the provided time period to this time period instance.
     * 
     * @param timePeriod the time period to copy from
     */
    public final void copyFrom(final TimePeriod timePeriod) {
        this.timeZone = timePeriod.getTimeZone();
        this.startDate = timePeriod.getStartDate();
        this.endDate = timePeriod.getEndDate();
        this.startTime = timePeriod.getStartTime();
        this.endTime = timePeriod.getEndTime();
    }

    /**
     * Get difference between start and end time in hours.
     * 
     * @return hours
     */
    public double getHoursDifference() {
        return (this.endTime.getTime() - this.startTime.getTime()) * 1.0 / HOUR_MILLISECONDS;
    }

    /**
     * Get difference between start and end time in hours.
     * 
     * @return hours
     */
    public double getMinutesDifference() {
        return (this.endTime.getTime() - this.startTime.getTime()) * 1.0 / MINUTE_MILLISECONDS;
    }

    /**
     * Get the number of days between start date and end date.
     * 
     * @return days
     */
    public double getDaysDifference() {
        return (this.endDate.getTime() - this.startDate.getTime()) * 1.0 / DAY_MILLISECONDS;
    }

    /**
     * Validate fields.
     */
    private void validate() {
        if (this.startDate != null && this.endDate != null && this.startTime != null
                && this.endTime != null) {

            if (this.startDate.after(this.endDate)) {
                // @translatable
                throw new ReservationException("Start date is after end date", TimePeriod.class);
            }
            if (this.startTime.after(this.endTime)) {
                // @translatable
                throw new ReservationException("Start time is after end time", TimePeriod.class);
            }
        }

    }

}
