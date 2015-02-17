package com.archibus.app.reservation.domain.recurrence;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * The Class DayOfTheWeekTest.
 */
public class DayOfTheWeekTest extends TestCase {
    
    /**
     * Test day of the week integer value.
     */
    public void testDayOfTheWeekIntValue() {
        int index = 1;
        Assert.assertEquals(index++, DayOfTheWeek.Sunday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Monday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Tuesday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Wednesday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Thursday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Friday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Saturday.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Day.getIntValue());
        Assert.assertEquals(index++, DayOfTheWeek.Weekday.getIntValue());
        Assert.assertEquals(index, DayOfTheWeek.WeekendDay.getIntValue());
    }
    
    /**
     * Test day of the week ordinal.
     */
    public void testDayOfTheWeekOrdinal() {
        int index = 0;
        Assert.assertEquals(index++, DayOfTheWeek.Sunday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Monday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Tuesday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Wednesday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Thursday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Friday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Saturday.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Day.ordinal());
        Assert.assertEquals(index++, DayOfTheWeek.Weekday.ordinal());
        Assert.assertEquals(index, DayOfTheWeek.WeekendDay.ordinal());
    }
    
    /**
     * Test get day of the week using short string.
     */
    public void testGetDayOfTheWeekString() {
        DayOfTheWeek dayOfTheWeek = DayOfTheWeek.get("mon");
        Assert.assertEquals(DayOfTheWeek.Monday, dayOfTheWeek);
        
        dayOfTheWeek = DayOfTheWeek.get("wed");
        Assert.assertEquals(DayOfTheWeek.Wednesday, dayOfTheWeek);
        
        dayOfTheWeek = DayOfTheWeek.get("sun");
        Assert.assertEquals(DayOfTheWeek.Sunday, dayOfTheWeek);
    }
    
}
