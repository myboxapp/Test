package com.archibus.app.reservation.dao.datasource;

import java.sql.Date;
import java.util.Calendar;

/**
 * 
 * Utility class for testing.
 * 
 * @author Bart Vanderschoot
 * @since 20.1
 * 
 */
public final class Utils {
    
    /**
     * 
     * Private default constructor: utility class is non-instantiable.
     * 
     * @throws InstantiationException InstantiationException
     */
    private Utils() throws InstantiationException {
        throw new InstantiationException("Never instantiate " + this.getClass().getName()
                + "; use static methods!");
        
    }
    
    /**
     * Gets the date.
     * 
     * @param offsetDays the offset days
     * @return the date as SQL date
     */
    public static Date getDate(final int offsetDays) {
        final Calendar cal = Calendar.getInstance();
        cal.clear(Calendar.HOUR_OF_DAY);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.add(Calendar.DATE, offsetDays);
        
        return new Date(cal.getTimeInMillis());
    }
    
}