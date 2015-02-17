package com.archibus.app.reservation.domain.recurrence;

import java.text.ParseException;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.ReservationException;

/**
 * Test for MonthlyPattern.
 */
public class MonthlyPatternTest extends RecurrencePatternTestBase {
    
    /** First part of a recurring monthly pattern xml. */
    private static final String TAG_TYPE_MONTH = "<recurring type=\"month\" value1=\"";
    
    /** Last part of a recurring monthly pattern xml when total is null. */
    private static final String TAG_TOTAL_EMPTY = "\" value4=\"\" total=\"\" />";
    
    /** Part of the monthly pattern XML that closes value3 and opens the total attribute. */
    private static final String TAG_TOTAL = "\" value4=\"\" total=\"";
    
    /** Part of the monthly pattern XML for empty value2 and value3. */
    private static final String EMPTY_VALUE2_VALUE3 = "\" value2=\"\" value3=\"";
    
    /** Closing tag for recurrence pattern XML. */
    private static final String CLOSE_TAG = "\" />";
    
    /** The Week of month used for testing. */
    private static final int WEEK_OF_MONTH = 3;
    
    /** The test action that loops through the pattern. */
    private TestOccurrenceAction testAction;
    
    /** The pattern under test. */
    private MonthlyPattern pattern;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        this.endDate = this.dateFormatter.parse("2012-11-30");
        this.testAction = new TestOccurrenceAction();
    }
    
    /**
     * Test MonthlyPattern.loopThroughRepeats() using a fixed monthly date.
     */
    public void testLoopThroughRepeatsFixedDate() {
        // Create a new pattern occurring on a fixed day of the month.
        this.pattern = new MonthlyPattern(this.startDate, 1, this.dayOfTheMonth);
        this.pattern.setEndDate(this.endDate);
        
        try {
            this.pattern.loopThroughRepeats(this.testAction);
            
            // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
            Assert.assertEquals(6, this.testAction.getNumberOfVisitedDates());
            // CHECKSTYLE:ON
            final Date juneDate = this.dateFormatter.parse("2012-06-17");
            final Date julyDate = this.dateFormatter.parse("2012-07-17");
            final Date augustDate = this.dateFormatter.parse("2012-08-17");
            final Date septemberDate = this.dateFormatter.parse("2012-09-17");
            final Date octoberDate = this.dateFormatter.parse("2012-10-17");
            final Date novemberDate = this.dateFormatter.parse("2012-11-17");
            
            checkDates(juneDate, julyDate, augustDate, septemberDate, octoberDate, novemberDate);
        } catch (final ParseException exception) {
            fail(exception.toString());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Test MonthlyPattern.loopThroughRepeats() using a specified monthly instance of a week day.
     */
    public void testLoopThroughRepeatsWeekDay() {
        // Create a new pattern occurring on a specific instance of a weekday day each month.
        final DayOfTheWeek dayOfTheWeek = DayOfTheWeek.Thursday;
        this.pattern =
                new MonthlyPattern(this.startDate, this.endDate, 1, WEEK_OF_MONTH, dayOfTheWeek);
        try {
            this.pattern.loopThroughRepeats(this.testAction);
            
            // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
            Assert.assertEquals(6, this.testAction.getNumberOfVisitedDates());
            // CHECKSTYLE:ON
            final Date juneDate = this.dateFormatter.parse("2012-06-21");
            final Date julyDate = this.dateFormatter.parse("2012-07-19");
            final Date augustDate = this.dateFormatter.parse("2012-08-16");
            final Date septemberDate = this.dateFormatter.parse("2012-09-20");
            final Date octoberDate = this.dateFormatter.parse("2012-10-18");
            final Date novemberDate = this.dateFormatter.parse("2012-11-15");
            
            checkDates(juneDate, julyDate, augustDate, septemberDate, octoberDate, novemberDate);
        } catch (final ParseException exception) {
            fail(exception.toString());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Test MonthlyPattern.loopThroughRepeats() using the last monthly instance of a week day.
     * <p>
     * Suppress warning avoid final local variable
     * <p>
     * Justification: Turning local variable into field is not useful in this case.
     */
    @SuppressWarnings("PMD.AvoidFinalLocalVariable")
    public void testLoopThroughRepeatsWeekDayLastOfMonth() {
        // Create a new pattern occurring on a specific instance of a weekday day each month.
        final DayOfTheWeek dayOfTheWeek = DayOfTheWeek.Thursday;
        final int weekOfMonth = 5;
        this.pattern =
                new MonthlyPattern(this.startDate, this.endDate, 1, weekOfMonth, dayOfTheWeek);
        try {
            this.pattern.loopThroughRepeats(this.testAction);
            
            // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
            Assert.assertEquals(6, this.testAction.getNumberOfVisitedDates());
            // CHECKSTYLE:ON
            final Date juneDate = this.dateFormatter.parse("2012-06-28");
            final Date julyDate = this.dateFormatter.parse("2012-07-26");
            final Date augustDate = this.dateFormatter.parse("2012-08-30");
            final Date septemberDate = this.dateFormatter.parse("2012-09-27");
            final Date octoberDate = this.dateFormatter.parse("2012-10-25");
            final Date novemberDate = this.dateFormatter.parse("2012-11-29");
            
            checkDates(juneDate, julyDate, augustDate, septemberDate, octoberDate, novemberDate);
        } catch (final ParseException exception) {
            fail(exception.toString());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Test MonthlyPattern.loopThroughRepeats with no stop condition.
     * <p>
     * Suppress warning "PMD.JUnitTestsShouldIncludeAssert".
     * <p>
     * Justification: false positive, asserts are included in the called method.
     */
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testLoopThroughRepeatsNoStopCondition() {
        this.loopThroughRepeatsNoStopCondition(new MonthlyPattern(this.startDate, this.endDate, 1,
            WEEK_OF_MONTH, DayOfTheWeek.WeekendDay));
        this.loopThroughRepeatsNoStopCondition(new MonthlyPattern(this.startDate, 2,
            this.dayOfTheMonth));
    }
    
    /**
     * Test MonthlyPattern.toString().
     * <p>
     * Suppress warning avoid final local variable
     * <p>
     * Justification: Turning local variable into field is not useful in this case.
     */
    @SuppressWarnings("PMD.AvoidFinalLocalVariable")
    public void testToString() {
        // for every last Thursday every 7 months
        final int weekOfMonth = 5;
        final int numberOfOccurrences = 6;
        final int interval = 7;
        this.pattern =
                new MonthlyPattern(this.startDate, this.endDate, interval, weekOfMonth,
                    DayOfTheWeek.Thursday);
        Assert.assertEquals("<recurring type=\"month\" value1=\"last\" value2=\"thu\" value3=\""
                + interval + TAG_TOTAL_EMPTY, this.pattern.toString());
        
        this.pattern.setDayOfTheWeek(DayOfTheWeek.Wednesday);
        this.pattern.setNumberOfOccurrences(numberOfOccurrences);
        Assert.assertEquals("<recurring type=\"month\" value1=\"last\" value2=\"wed\" value3=\""
                + interval + TAG_TOTAL + numberOfOccurrences + CLOSE_TAG, this.pattern.toString());
        
        // for a fixed date every 3 months
        this.pattern = new MonthlyPattern(this.startDate, interval, this.dayOfTheMonth);
        this.pattern.setEndDate(this.endDate);
        Assert.assertEquals(TAG_TYPE_MONTH + this.dayOfTheMonth + EMPTY_VALUE2_VALUE3 + interval
                + TAG_TOTAL_EMPTY, this.pattern.toString());
        
        this.pattern.setNumberOfOccurrences(numberOfOccurrences);
        Assert.assertEquals(TAG_TYPE_MONTH + this.dayOfTheMonth + EMPTY_VALUE2_VALUE3 + interval
                + TAG_TOTAL + numberOfOccurrences + CLOSE_TAG, this.pattern.toString());
    }
    
    /**
     * Check the dates of the normal loop with interval = 1 and then retest with a larger interval.
     * 
     * @param juneDate the date in june
     * @param julyDate the date in july
     * @param augustDate the date in august
     * @param septemberDate the date in september
     * @param octoberDate the date in october
     * @param novemberDate the date in november
     * @throws ReservationException when the loop method fails
     */
    private void checkDates(final Date juneDate, final Date julyDate, final Date augustDate,
            final Date septemberDate, final Date octoberDate, final Date novemberDate)
            throws ReservationException {
        List<Date> visitedDates = this.testAction.getVisitedDates();
        Assert.assertEquals(juneDate, visitedDates.get(0));
        Assert.assertEquals(julyDate, visitedDates.get(1));
        Assert.assertEquals(augustDate, visitedDates.get(2));
        // CHECKSTYLE:OFF Justification: these 'magic numbers' are used for testing.
        Assert.assertEquals(septemberDate, visitedDates.get(3));
        Assert.assertEquals(octoberDate, visitedDates.get(4));
        Assert.assertEquals(novemberDate, visitedDates.get(5));
        this.testAction.clearVisitedDates();
        
        // increase the interval
        this.pattern.setInterval(3);
        // CHECKSTYLE:ON
        this.pattern.loopThroughRepeats(this.testAction);
        Assert.assertEquals(2, this.testAction.getNumberOfVisitedDates());
        visitedDates = this.testAction.getVisitedDates();
        Assert.assertEquals(augustDate, visitedDates.get(0));
        Assert.assertEquals(novemberDate, visitedDates.get(1));
    }
    
}
