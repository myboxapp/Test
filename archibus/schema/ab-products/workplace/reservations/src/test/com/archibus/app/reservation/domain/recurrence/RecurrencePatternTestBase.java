package com.archibus.app.reservation.domain.recurrence;

import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.*;

import com.archibus.app.reservation.domain.ReservationException;
import com.archibus.app.reservation.service.RecurrenceService;

/**
 * Base classe for testing recurrences.
 * 
 * @author bv
 * 
 * @since 20.1
 * 
 *        <p>
 *        Suppress warning "PMD.TestClassWithoutTestCases".
 *        <p>
 *        Justification: this is a suite that groups other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class RecurrencePatternTestBase extends TestCase {
    
    /** Start date of the pattern. */
    protected Date startDate;
    
    /** End date of the pattern. */
    protected Date endDate;
    
    /** The date formatter to convert between strings and dates. */
    protected SimpleDateFormat dateFormatter;
    
    /** The day of the month for the start date. */
    protected int dayOfTheMonth;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        
        this.startDate = this.dateFormatter.parse("2012-05-17");
        this.endDate = this.dateFormatter.parse("2018-05-31");
        // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
        this.dayOfTheMonth = 17;
        // CHECKSTYLE:ON
    }
    
    /**
     * Test loopThroughRepeats with no stop condition. This method first removes the stop conditions
     * from the pattern and then verifies the number of occurrences visited.
     * 
     * @param pattern the recurrence pattern to test
     */
    protected void loopThroughRepeatsNoStopCondition(final AbstractIntervalPattern pattern) {
        try {
            final TestOccurrenceAction testAction = new TestOccurrenceAction();
            pattern.setInterval(1);
            pattern.setNumberOfOccurrences(null);
            pattern.setEndDate(null);
            pattern.loopThroughRepeats(testAction);
            Assert.assertEquals("No limit means 499 repeats",
                RecurrenceService.getMaxOccurrences() - 1, testAction.getNumberOfVisitedDates());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
}
