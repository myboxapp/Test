package com.archibus.app.reservation.domain.recurrence;

import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * The Class RecurrenceTest.
 * 
 * Test parsing the recurrence patterns with start date and number of occurrences.
 * 
 */
public class RecurrenceTest extends TestCase {
    
    /** Example daily pattern. */
    private static final String DAILY_PATTERN =
            "<recurring type=\"day\" value1=\"1\" value2=\"\" value3=\"\" value4=\"\" total=\"5\" />";
    
    /** Example weekly pattern. */
    private static final String WEEKLY_PATTERN =
            "<recurring type=\"week\" value1=\"1,1,0,0,0,1,1\" value2=\"1\" value3=\"\" value4=\"\" total=\"5\" />";
    
    /** Example fixed monthly pattern. */
    private static final String MONTHLY_PATTERN =
            "<recurring type=\"month\" value1=\"8\" value2=\"\" value3=\"1\" value4=\"\" total=\"5\" />";
    
    /** Example relative monthly pattern. */
    private static final String MONTHLY_PATTERN2 =
            "<recurring type=\"month\" value1=\"3rd\" value2=\"sat\" value3=\"1\" value4=\"\" total=\"5\" />";
    
    /** Example fixed yearly pattern. */
    private static final String YEARLY_PATTERN =
            "<recurring type=\"year\" value1=\"9\" value2=\"mar\" value3=\"1\" value4=\"\" total=\"3\" />";
    
    /** Example relative yearly pattern. */
    private static final String YEARLY_PATTERN2 =
            "<recurring type=\"year\" value1=\"last\" value2=\"thu\" value3=\"may\" value4=\"2\" total=\"3\" />";

    /**
     * Test parse weekly recurrence.
     */
    public void testParseDailyRecurrence() {
        final Date startDate = new Date();
        
        final Recurrence dailyRecurrence =
                Recurrence.parseRecurrence(startDate, null, DAILY_PATTERN);
        
        Assert.assertEquals(DAILY_PATTERN, dailyRecurrence.toString());
    }
    
    /**
     * Test parse weekly recurrence.
     */
    public void testParseWeeklyRecurrence() {
        final Date startDate = new Date();
        
        final Recurrence weeklyRecurrence =
                Recurrence.parseRecurrence(startDate, null, WEEKLY_PATTERN);
        
        Assert.assertEquals(WEEKLY_PATTERN, weeklyRecurrence.toString());
    }
    
    /**
     * Test parse monthly recurrence.
     */
    public void testParseMonthlyRecurrence() {
        final Date startDate = new Date();
        
        final Recurrence monthlyRecurrence =
                Recurrence.parseRecurrence(startDate, null, MONTHLY_PATTERN);
        
        Assert.assertEquals(MONTHLY_PATTERN, monthlyRecurrence.toString());
        
    }
    
    /**
     * Test parse monthly recurrence2.
     */
    public void testParseMonthlyRecurrence2() {
        final Date startDate = new Date();
        
        final Recurrence monthlyRecurrence =
                Recurrence.parseRecurrence(startDate, null, MONTHLY_PATTERN2);
        
        Assert.assertEquals(MONTHLY_PATTERN2, monthlyRecurrence.toString());
    }
    
    /**
     * Test parse yearly recurrence.
     */
    public void testParseYearlyRecurrence() {
        final Date startDate = new Date();
        
        final Recurrence yearlyRecurrence =
                Recurrence.parseRecurrence(startDate, null, YEARLY_PATTERN);
        
        Assert.assertEquals(YEARLY_PATTERN, yearlyRecurrence.toString());
    }
    
    /**
     * Test parse yearly recurrence2.
     */
    public void testParseYearlyRecurrence2() {
        final Date startDate = new Date();
        
        final Recurrence yearlyRecurrence =
                Recurrence.parseRecurrence(startDate, null, YEARLY_PATTERN2);
        
        Assert.assertEquals(YEARLY_PATTERN2, yearlyRecurrence.toString());
    }

}
