package com.archibus.app.reservation.domain.recurrence;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.ReservationException;

/**
 * Weekly pattern test.
 * 
 * @author Yorik Gerlo
 */
public class WeeklyPatternTest extends RecurrencePatternTestBase {
    
    /** The year used for testing. */
    private static final int YEAR = 2011;
    
    /** The day of the month used for testing. */
    private static final int DAY_OF_MONTH = 29;
    
    /** The interval pattern under test. */
    private AbstractIntervalPattern pattern;
    
    /** A calendar instance used for testing. */
    private Calendar calendar;
    
    /**
     * Set up for a test case.
     * 
     * @throws Exception when setup fails
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        this.pattern = new WeeklyPattern();
        this.calendar = Calendar.getInstance();
        this.calendar.clear();
        
        this.calendar.set(YEAR, Calendar.NOVEMBER, DAY_OF_MONTH);
    }
    
    /**
     * Test looping through a weekly pattern.
     * <p>
     * Suppress warning avoid final local variable
     * <p>
     * Justification: Turning local variable into field is not useful
     */
    @SuppressWarnings("PMD.AvoidFinalLocalVariable")
    public void testLoopThroughRepeats() {
        final int endDay = 20;
        final Calendar endDate = Calendar.getInstance();
        endDate.clear();
        endDate.set(YEAR, Calendar.DECEMBER, endDay);
        final List<DayOfTheWeek> dayOfTheWeek = new ArrayList<DayOfTheWeek>();
        dayOfTheWeek.add(DayOfTheWeek.Tuesday);
        final WeeklyPattern weeklyPattern =
                new WeeklyPattern(this.calendar.getTime(), endDate.getTime(), 1, dayOfTheWeek);
        final TestOccurrenceAction testAction = new TestOccurrenceAction();
        try {
            weeklyPattern.loopThroughRepeats(testAction);
        } catch (final ReservationException exception) {
            fail("Looping through a valid pattern shouldn't cause an exception: " + exception);
        }
        final List<Date> visitedDates = testAction.getVisitedDates();
        final int expectedVisits = 3;
        
        Assert.assertEquals(expectedVisits, visitedDates.size());
        final int daysInWeek = 7;
        this.calendar.add(Calendar.DATE, daysInWeek);
        Assert.assertEquals(this.calendar.getTime(), visitedDates.get(0));
        this.calendar.add(Calendar.DATE, daysInWeek);
        Assert.assertEquals(this.calendar.getTime(), visitedDates.get(1));
        this.calendar.add(Calendar.DATE, daysInWeek);
        Assert.assertEquals(this.calendar.getTime(), visitedDates.get(2));
    }
    
    /**
     * Test WeeklyPattern.loopThroughRepeats with no stop condition.
     * <p>
     * Suppress warning "PMD.JUnitTestsShouldIncludeAssert".
     * <p>
     * Justification: false positive, asserts are included in the called method.
     */
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testLoopThroughRepeatsNoStopCondition() {
        this.loopThroughRepeatsNoStopCondition(new WeeklyPattern(null, 1, DayOfTheWeek.WeekendDay));
        this.loopThroughRepeatsNoStopCondition(new WeeklyPattern(null, 2, DayOfTheWeek.Friday));
    }
    
    /**
     * Test WeeklyPattern.toString().
     * <p>
     * Suppress warning avoid final local variable
     * <p>
     * Justification: Turning local variable into field is not useful
     */
    @SuppressWarnings("PMD.AvoidFinalLocalVariable")
    public void testToString() {
        final int endDay = 20;
        final Calendar endDate = Calendar.getInstance();
        endDate.clear();
        endDate.set(YEAR, Calendar.DECEMBER, endDay);
        final List<DayOfTheWeek> dayOfTheWeek = new ArrayList<DayOfTheWeek>();
        dayOfTheWeek.add(DayOfTheWeek.Tuesday);
        dayOfTheWeek.add(DayOfTheWeek.Saturday);
        dayOfTheWeek.add(DayOfTheWeek.Friday);
        final int interval = 7;
        this.pattern =
                new WeeklyPattern(this.calendar.getTime(), endDate.getTime(), interval,
                    dayOfTheWeek);
        Assert.assertEquals("<recurring type=\"week\" value1=\"0,1,0,0,1,1,0\" value2=\""
                + interval + "\" value3=\"\" value4=\"\" total=\"\" />", this.pattern.toString());
        
        final int numberOfOccurrences = 3;
        this.pattern.setNumberOfOccurrences(numberOfOccurrences);
        dayOfTheWeek.add(DayOfTheWeek.Monday);
        
        Assert.assertEquals("<recurring type=\"week\" value1=\"1,1,0,0,1,1,0\" value2=\""
                + interval + "\" value3=\"\" value4=\"\" total=\"" + numberOfOccurrences + "\" />",
            this.pattern.toString());
    }
}
