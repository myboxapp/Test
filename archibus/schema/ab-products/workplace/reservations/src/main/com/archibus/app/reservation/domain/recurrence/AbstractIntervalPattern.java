package com.archibus.app.reservation.domain.recurrence;

import java.util.*;

import javax.xml.bind.annotation.*;

import com.archibus.app.reservation.domain.ReservationException;
import com.archibus.app.reservation.service.RecurrenceService;
import com.archibus.app.reservation.service.actions.SaveRecurringReservationOccurrenceAction;

/**
 * Interval pattern.
 * 
 * @author Bart Vanderschoot
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IntervalPattern")
public abstract class AbstractIntervalPattern extends Recurrence {
    
    /**
     * Interface to implement for looping through recurrent reservations.
     * 
     * @author Bart Vanderschoot
     */
    public interface OccurrenceAction {
        /**
         * Handle a single occurrence of the interval pattern.
         * 
         * @param date the date of the occurrence to handle
         * 
         * @return true if the loop should continue, false to stop
         * 
         * @throws ReservationException reservation exception
         */
        boolean handleOccurrence(Date date) throws ReservationException;
    }
    
    /** The interval. */
    protected Integer interval = 1;
    
    /**
     * Default constructor.
     */
    protected AbstractIntervalPattern() {
        super();
    }
    
    /**
     * Constructor with parameters.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param interval interval
     */
    public AbstractIntervalPattern(final Date startDate, final Date endDate, final Integer interval) {
        super();
        setInterval(interval);
        setStartDate(startDate);
        setEndDate(endDate);
    }
    
    /**
     * Constructor with parameters.
     * 
     * @param startDate start date
     * @param interval interval
     */
    public AbstractIntervalPattern(final Date startDate, final Integer interval) {
        super();
        setInterval(interval);
        setStartDate(startDate);
    }
    
    /**
     * Loop through all repeats of the pattern, thus excluding the first instance. End the loop if
     * the OccurrenceAction return value is false.
     * 
     * @param action the action to perform on each repeat
     * @throws ReservationException reservation exception
     */
    public final void loopThroughRepeats(final OccurrenceAction action) throws ReservationException {
        final List<Date> dateList =
                RecurrenceService.getDateList(getStartDate(), getEndDate(), this.toString());
        
        if (action instanceof SaveRecurringReservationOccurrenceAction && getEndDate() != null
                && !dateList.isEmpty()) {
            final Date requestedEndDate = getEndDate();
            final Date actualEndDate = dateList.get(dateList.size() - 1);
            if (requestedEndDate.after(actualEndDate)) {
                // @translatable
                throw new ReservationException(
                    "You can only create reservations for up to {0} occurrences. Use the recurrence dialog to enter a smaller number of occurrences.",
                    AbstractIntervalPattern.class, dateList.size());
            }
        }

        int index = 1;
        boolean userWantsToContinue = true;
        while (userWantsToContinue && index < dateList.size()) {
            userWantsToContinue = action.handleOccurrence(dateList.get(index));
            index++;
        }
    }
    
    /**
     * Get the interval.
     * 
     * @return interval
     */
    public int getInterval() {
        return this.interval;
    }
    
    /**
     * Set the interval.
     * 
     * @param interval interval
     */
    public final void setInterval(final int interval) {
        this.interval = interval;
    }
    
}
