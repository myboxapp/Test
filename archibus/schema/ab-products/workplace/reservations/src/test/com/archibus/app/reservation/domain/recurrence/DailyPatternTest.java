package com.archibus.app.reservation.domain.recurrence;

import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.ReservationException;

/**
 * Test for DailyPattern.
 */
public class DailyPatternTest extends RecurrencePatternTestBase {
    
    /** The recurrence pattern under test. */
    private DailyPattern pattern;
    
    /** Number of occurrences in the pattern when interval == 1. */
    private int defaultNumberOfOccurrences;
    
    /**
     * Number of repeats in the pattern when interval == 1. This is one less than the number of
     * occurrence because the first occurrence doesn't count as a repeat.
     */
    private int defaultNumberOfRepeats;
    
    /**
     * Occurrence action used for testing the loopThroughRepeats().
     */
    private TestOccurrenceAction testAction;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        
        final Date startDate = dateFormatter.parse("2011-11-09");
        final Date endDate = dateFormatter.parse("2011-11-19");
        // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
        this.defaultNumberOfOccurrences = 11;
        // CHECKSTYLE:ON
        this.defaultNumberOfRepeats = this.defaultNumberOfOccurrences - 1;
        
        this.pattern = new DailyPattern(startDate, endDate, 1);
        
        this.testAction = new TestOccurrenceAction();
    }
    
    /**
     * Test for DailyPattern.loopThroughRepeats() with only an end date specified.
     */
    public void testLoopThroughRepeatsEndDate() {
        try {
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(this.defaultNumberOfRepeats,
                this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            int interval = 2;
            this.pattern.setInterval(interval);
            this.pattern.loopThroughRepeats(this.testAction);
            // CHECKSTYLE:OFF Justification: these 'magic numbers' are used for testing.
            Assert.assertEquals(5, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            /*
             * The end date in the recurrence pattern indicates when the loop should stop: there is
             * exactly one occurrence of the pattern on or after the specified end date.
             */
            interval = 3;
            this.pattern.setInterval(interval);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(3, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            interval = 4;
            this.pattern.setInterval(interval);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(2, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            this.pattern.setInterval(5);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(2, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            for (interval = 6; interval <= this.defaultNumberOfRepeats; ++interval) {
                // CHECKSTYLE:ON
                this.pattern.setInterval(interval);
                this.pattern.loopThroughRepeats(this.testAction);
                Assert.assertEquals("Number of repeats for interval = " + interval, 1,
                    this.testAction.getNumberOfVisitedDates());
                this.testAction.clearVisitedDates();
            }
            interval = this.defaultNumberOfRepeats + 1;
            this.pattern.setInterval(interval);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals("interval = " + interval, 0,
                this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Test DailyPattern.loopThroughRepeats with number of occurrences and end date specified.
     */
    public void testLoopThroughRepeatsOccurrences() {
        try {
            /*
             * When the number of occurrences is specified in the pattern, this number is never
             * exceeded. The number of repeats is smaller if the end date is reached beforehand.
             */
            this.pattern.setInterval(1);
            this.pattern.setNumberOfOccurrences(1);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals("One occurrence means no repeats.", 0,
                this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            int numberOfOccurrences = this.defaultNumberOfOccurrences / 2;
            this.pattern.setNumberOfOccurrences(numberOfOccurrences);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals("Number of occurrences limits the number of visited dates.",
                numberOfOccurrences - 1, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            numberOfOccurrences = 2 * this.defaultNumberOfOccurrences;
            this.pattern.setNumberOfOccurrences(numberOfOccurrences);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(
                "Larger number of occurrences is ignored when end date is specified.",
                this.defaultNumberOfRepeats, this.testAction.getNumberOfVisitedDates());
            this.testAction.clearVisitedDates();
            
            this.pattern.setEndDate(null);
            this.pattern.loopThroughRepeats(this.testAction);
            Assert.assertEquals(
                "Larger number of occurrences is applied when no end date is specified.",
                numberOfOccurrences - 1, this.testAction.getNumberOfVisitedDates());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Test DailyPattern.loopThroughRepeats with no stop condition.
     * 
     * <p>
     * Suppress warning "PMD.JUnitTestsShouldIncludeAssert".
     * <p>
     * Justification: false positive, asserts are included in the called method.
     */
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testLoopThroughRepeatsNoStopCondition() {
        this.loopThroughRepeatsNoStopCondition(new DailyPattern(null, 2));
        this.loopThroughRepeatsNoStopCondition(new DailyPattern(null, 1));
    }
    
    /**
     * Test for DailyPattern.toString().
     */
    public void testToString() {
        Assert
            .assertEquals(
                "<recurring type=\"day\" value1=\"1\" value2=\"\" value3=\"\" value4=\"\" total=\"\" />",
                this.pattern.toString());
        
        this.pattern.setNumberOfOccurrences(this.defaultNumberOfOccurrences);
        
        Assert.assertEquals(
            "<recurring type=\"day\" value1=\"1\" value2=\"\" value3=\"\" value4=\"\" total=\""
                    + this.defaultNumberOfOccurrences + "\" />", this.pattern.toString());
    }
    
}
