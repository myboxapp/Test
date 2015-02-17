package com.archibus.app.reservation.domain;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.*;

/**
 * Test for TimePeriod.
 */
public class TimePeriodTest extends TestCase {
    
    /**
     * Time zone ID used for testing.
     */
    private static final String TIMEZONE_ID = "Europe/Brussels";
    
    /** The start date used for testing. */
    private Date startDate;
    
    /** The start time used for testing. */
    private Date endDate;
    
    /** The end date used for testing. */
    private Time startTime;
    
    /** The end time used for testing. */
    private Time endTime;
    
    /** The combination of start date and start time. */
    private Date startDateTime;
    
    /** The combination of end date and end time. */
    private Date endDateTime;
    
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: setup test
    /**
     * Set up for a test case.
     * 
     * @throws Exception when setup fails
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        this.startDate = dateFormatter.parse("2011-11-09");
        this.endDate = dateFormatter.parse("2011-11-19");
        
        this.startTime = new Time(timeFormatter.parse("1899-12-30 10:00:00").getTime());
        this.endTime = new Time(timeFormatter.parse("1899-12-30 14:00:00").getTime());
        
        this.startDateTime = timeFormatter.parse("2011-11-09 10:00:00");
        this.endDateTime = timeFormatter.parse("2011-11-19 14:00:00");
    }
    
    /**
     * Test for the no-argument constructor.
     */
    public void testTimePeriod() {
        final TimePeriod timePeriod = new TimePeriod();
        Assert.assertNull(timePeriod.getStartDate());
        Assert.assertNull(timePeriod.getStartTime());
        Assert.assertNull(timePeriod.getEndDate());
        Assert.assertNull(timePeriod.getEndTime());
        Assert.assertNull(timePeriod.getTimeZone());
    }
    
    /**
     * Test for the four-argument constructor.
     */
    public void testTimePeriodDateDateTimeTime() {
        final TimePeriod timePeriod =
                new TimePeriod(this.startDate, this.endDate, this.startTime, this.endTime);
        Assert.assertEquals(this.startDate, timePeriod.getStartDate());
        Assert.assertEquals(this.endDate, timePeriod.getEndDate());
        Assert.assertEquals(this.startTime, timePeriod.getStartTime());
        Assert.assertEquals(this.endTime, timePeriod.getEndTime());
        Assert.assertNull(timePeriod.getTimeZone());
    }
    
    /**
     * Test for the five-argument constructor.
     */
    public void testTimePeriodDateDateTimeTimeString() {
        final TimePeriod timePeriod =
                new TimePeriod(this.startDate, this.endDate, this.startTime, this.endTime,
                    TIMEZONE_ID);
        Assert.assertEquals(this.startDate, timePeriod.getStartDate());
        Assert.assertEquals(this.endDate, timePeriod.getEndDate());
        Assert.assertEquals(this.startTime, timePeriod.getStartTime());
        Assert.assertEquals(this.endTime, timePeriod.getEndTime());
        Assert.assertEquals(TIMEZONE_ID, timePeriod.getTimeZone());
    }
    
    /**
     * Test for the copy constructor.
     */
    public void testTimePeriodTimePeriod() {
        final TimePeriod timePeriod1 =
                new TimePeriod(this.startDate, this.endDate, this.startTime, this.endTime,
                    TIMEZONE_ID);
        final TimePeriod timePeriod2 = new TimePeriod(timePeriod1);
        Assert.assertEquals(timePeriod1.getStartDate(), timePeriod2.getStartDate());
        Assert.assertEquals(timePeriod1.getEndDate(), timePeriod2.getEndDate());
        Assert.assertEquals(timePeriod1.getStartTime(), timePeriod2.getStartTime());
        Assert.assertEquals(timePeriod1.getEndTime(), timePeriod2.getEndTime());
        Assert.assertEquals(timePeriod1.getTimeZone(), timePeriod2.getTimeZone());
    }
    
    /**
     * Test for TimePeriod.setStartDateTime().
     */
    public void testSetStartDateTime() {
        final TimePeriod timePeriod = new TimePeriod();
        timePeriod.setStartDateTime(this.startDateTime);
        Assert.assertEquals(this.startDate, timePeriod.getStartDate());
        Assert.assertEquals(this.startTime, timePeriod.getStartTime());
        
        final Date dateTime = timePeriod.getStartDateTime();
        Assert.assertEquals(this.startDateTime, dateTime);
    }
    
    /**
     * Test for TimePeriod.setEndDateTime().
     */
    public void testSetEndDateTime() {
        final TimePeriod timePeriod = new TimePeriod();
        timePeriod.setEndDateTime(this.endDateTime);
        Assert.assertEquals(this.endDate, timePeriod.getEndDate());
        Assert.assertEquals(this.endTime, timePeriod.getEndTime());
        
        final Date dateTime = timePeriod.getEndDateTime();
        Assert.assertEquals(this.endDateTime, dateTime);
    }
    
    /**
     * Test for TimePeriod.clearDate().
     */
    public void testClearDate() {
        Assert.assertEquals(this.startTime, TimePeriod.clearDate(this.startDateTime));
        Assert.assertEquals(this.endTime, TimePeriod.clearDate(this.endDateTime));
    }
    
    /**
     * Test for TimePeriod.clearTime().
     */
    public void testClearTime() {
        Assert.assertEquals(this.startDate, TimePeriod.clearTime(this.startDateTime));
        Assert.assertEquals(this.endDate, TimePeriod.clearTime(this.endDateTime));
    }
    
    /**
     * Test for TimePeriod.copyFrom().
     */
    public void testCopyFrom() {
        final TimePeriod timePeriod1 =
                new TimePeriod(this.startDate, this.endDate, this.startTime, this.endTime,
                    TIMEZONE_ID);
        final TimePeriod timePeriod2 = new TimePeriod();
        timePeriod2.copyFrom(timePeriod1);
        Assert.assertEquals(timePeriod1.getStartDate(), timePeriod2.getStartDate());
        Assert.assertEquals(timePeriod1.getEndDate(), timePeriod2.getEndDate());
        Assert.assertEquals(timePeriod1.getStartTime(), timePeriod2.getStartTime());
        Assert.assertEquals(timePeriod1.getEndTime(), timePeriod2.getEndTime());
        Assert.assertEquals(timePeriod1.getTimeZone(), timePeriod2.getTimeZone());
    }
    
}
