package com.archibus.app.reservation.exchange.util;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.domain.CalendarException;
import com.archibus.app.reservation.domain.recurrence.*;

/**
 * The ExchangeRecurrenceConverter: converts between Reservations recurrence patterns and the
 * corresponding EWS patterns.
 * 
 * Managed by Spring.
 */
public class ExchangeRecurrenceConverter {
    
    /** Conversion error message. */
    // @translatable
    private static final String CONVERSION_ERROR = "Error converting recurrence to Exchange format";
    
    /** Converts DayOfTheWeek and weekOfMonth values to Exchange format. */
    private ExchangeDayOfTheWeekConverter dayOfTheWeekConverter;
    
    /**
     * Convert recurrence.
     * 
     * @param recurrence the recurrence
     * @return the microsoft.exchange.webservices.data. recurrence
     */
    public microsoft.exchange.webservices.data.Recurrence convertToExchangeRecurrence(
            final com.archibus.app.reservation.domain.recurrence.Recurrence recurrence) {
        microsoft.exchange.webservices.data.Recurrence exchangeRecurrence = null;
        
        try {
            if (recurrence instanceof DailyPattern) {
                final DailyPattern dailyPattern = (DailyPattern) recurrence;
                
                exchangeRecurrence =
                        new microsoft.exchange.webservices.data.Recurrence.DailyPattern(
                            dailyPattern.getStartDate(), dailyPattern.getInterval());
                
            } else if (recurrence instanceof WeeklyPattern) {
                final WeeklyPattern weeklyPattern = (WeeklyPattern) recurrence;
                
                final microsoft.exchange.webservices.data.DayOfTheWeek[] daysOfWeek =
                        this.dayOfTheWeekConverter.convertDaysOfTheWeek(weeklyPattern
                            .getDaysOfTheWeek());
                
                exchangeRecurrence =
                        new microsoft.exchange.webservices.data.Recurrence.WeeklyPattern(
                            weeklyPattern.getStartDate(), weeklyPattern.getInterval(), daysOfWeek);
                
            } else if (recurrence instanceof MonthlyPattern) {
                final MonthlyPattern monthlyPattern = (MonthlyPattern) recurrence;
                exchangeRecurrence =
                        ExchangeRecurrenceHelper.convertMonthlyPattern(monthlyPattern,
                            this.dayOfTheWeekConverter);
            } else if (recurrence instanceof YearlyPattern) {
                final YearlyPattern yearlyPattern = (YearlyPattern) recurrence;
                exchangeRecurrence =
                        ExchangeRecurrenceHelper.convertYearlyPattern(yearlyPattern,
                            this.dayOfTheWeekConverter);
            } else {
                // @translatable
                throw new CalendarException("No valid recurrence pattern",
                    ExchangeRecurrenceConverter.class);
            }
            
            /*
             * Only one of end date or number of occurrences can be set in the Exchange Recurrence
             * Pattern. Even when number of occurrences is null, the end date will still indicate
             * the date of the last reservation. The end date is computed based on the number of
             * occurrences requested by the user, or based on the limit imposed by the application.
             * Hence the end date should take precedence.
             */
            if (recurrence.getEndDate() == null) {
                exchangeRecurrence.setNumberOfOccurrences(recurrence.getNumberOfOccurrences());
            } else {
                exchangeRecurrence.setEndDate(recurrence.getEndDate());
            }
        } catch (ArgumentOutOfRangeException exception) {
            throw new CalendarException(CONVERSION_ERROR, exception,
                ExchangeRecurrenceConverter.class);
        } catch (ArgumentException exception) {
            throw new CalendarException(CONVERSION_ERROR, exception,
                ExchangeRecurrenceConverter.class);
        }
        
        return exchangeRecurrence;
    }
    
    /**
     * Set the day of the week converter to be used.
     * 
     * @param dayOfTheWeekConverter the new day of the week converter
     */
    public void setDayOfTheWeekConverter(final ExchangeDayOfTheWeekConverter dayOfTheWeekConverter) {
        this.dayOfTheWeekConverter = dayOfTheWeekConverter;
    }
    
}