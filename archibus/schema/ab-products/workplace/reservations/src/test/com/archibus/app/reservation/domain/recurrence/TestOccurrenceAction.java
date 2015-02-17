package com.archibus.app.reservation.domain.recurrence;

import java.util.*;

import com.archibus.app.reservation.domain.ReservationException;

/**
 * Basic occurrence action that registers which dates are visited.
 * 
 * @author Yorik Gerlo
 * 
 */
public class TestOccurrenceAction implements AbstractIntervalPattern.OccurrenceAction {
    
    /** The list of visited dates. */
    private final List<Date> visitedDates = new ArrayList<Date>();
    
    /**
     * {@inheritDoc}
     */
    public boolean handleOccurrence(final Date date) throws ReservationException {
        this.visitedDates.add(date);
        return true;
    }
    
    /**
     * Get the list of visited dates.
     * 
     * @return the list of visited dates maintained by this TestOccurrenceAction.
     */
    public List<Date> getVisitedDates() {
        return this.visitedDates;
    }
    
    /**
     * Get the number of visited dates.
     * 
     * @return number of visited dates
     */
    public int getNumberOfVisitedDates() {
        return this.visitedDates.size();
    }
    
    /**
     * Clear the list of visited dates maintained by this TestOccurrenceAction.
     */
    public void clearVisitedDates() {
        this.visitedDates.clear();
    }
    
}
