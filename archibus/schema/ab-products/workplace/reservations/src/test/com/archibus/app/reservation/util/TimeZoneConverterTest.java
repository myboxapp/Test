package com.archibus.app.reservation.util;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.datasource.DataSourceTestBase;
import com.archibus.utility.LocalDateTimeUtil;

/**
 * Test for TimeZoneConverter.
 */
public class TimeZoneConverterTest extends DataSourceTestBase {
    
    /**
     * Site ID of the building to use for testing.
     */
    private static final String HQ_SITE_ID = "MARKET";
    
    /**
     * Building ID used for testing.
     */
    private static final String HQ_BUILDING_ID = "HQ";
    
    /**
     * Time zone identifier of the HQ building on MARKET site.
     */
    private static final String HQ_TIMEZONE_ID = "EST";
    
    /**
     * Time zone identifier of the requestor.
     */
    private static final String REQUESTOR_TIMEZONE_ID = "Europe/Brussels";
    
    /**
     * Date of the requestor to be converted to local time.
     */
    private Date requestorDate;
    
    /**
     * Time of the requestor to be converted to local time.
     */
    private Time requestorTime;
    
    /**
     * Combined date and time value of the requestor.
     */
    private Date requestorDateTime;
    
    /**
     * Time zone offset from requestor time zone to hq time zone.
     */
    private int timeZoneOffset;
    
    /**
     * Set up for a test case for TimeZoneConverter utils.
     * 
     * @throws Exception when setup fails
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        this.requestorDate = dateFormatter.parse("2012-05-02");
        this.requestorTime = new Time(timeFormatter.parse("1899-12-30 18:30:00").getTime());
        this.requestorDateTime = timeFormatter.parse("2012-05-02 18:30:00");
        
        final TimeZone requestorTimeZone = TimeZone.getTimeZone(REQUESTOR_TIMEZONE_ID);
        final TimeZone hqTimeZone = TimeZone.getTimeZone(HQ_TIMEZONE_ID);
        this.timeZoneOffset =
                hqTimeZone.getOffset(this.requestorDate.getTime())
                        - requestorTimeZone.getOffset(this.requestorDate.getTime());
    }
    
    /**
     * Test getting the location time zone of a building and site.
     */
    public void testLocationTimeZone() {
        
        String timeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, HQ_SITE_ID, null);
        
        Assert.assertEquals(HQ_TIMEZONE_ID, timeZone);
        
        timeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, null, HQ_BUILDING_ID);
        
        Assert.assertEquals(HQ_TIMEZONE_ID, timeZone);
    }
    
    /**
     * Test method for TimeZoneConverter.calculateDateTimeForSite().
     */
    public void testCalculateDateTimeForSite() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.requestorDateTime);
        calendar.add(Calendar.MILLISECOND, this.timeZoneOffset);
        final Date expectedSiteTime = calendar.getTime();
        
        final Date actualSiteTime =
                TimeZoneConverter.calculateDateTimeForSite(HQ_SITE_ID, this.requestorDate,
                    this.requestorTime, REQUESTOR_TIMEZONE_ID, true);
        Assert.assertEquals(expectedSiteTime, actualSiteTime);
        final Date calculatedRequestorDateTime =
                TimeZoneConverter.calculateDateTimeForSite(HQ_SITE_ID, actualSiteTime,
                    REQUESTOR_TIMEZONE_ID, false);
        Assert.assertEquals(this.requestorDateTime, calculatedRequestorDateTime);
    }
    
    /**
     * Test method for TimeZoneConverter.calculateDateTimeForBuilding().
     */
    public void testCalculateDateTimeForBuilding() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.requestorDateTime);
        calendar.add(Calendar.MILLISECOND, this.timeZoneOffset);
        final Date expectedBuildingTime = calendar.getTime();
        
        final Date actualBuildingTime =
                TimeZoneConverter.calculateDateTimeForBuilding(HQ_BUILDING_ID, this.requestorDate,
                    this.requestorTime, REQUESTOR_TIMEZONE_ID, true);
        Assert.assertEquals(expectedBuildingTime, actualBuildingTime);
        
        // backwards
        final Date calculatedRequestorDateTime =
                TimeZoneConverter.calculateDateTimeForBuilding(HQ_BUILDING_ID, actualBuildingTime,
                    REQUESTOR_TIMEZONE_ID, false);
        Assert.assertEquals(this.requestorDateTime, calculatedRequestorDateTime);
    }
    
    /**
     * Test method for TimeZoneConverter.calculateRequestorDateTime().
     */
    public void testCalculateRequestorDateTime() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.requestorDateTime);
        calendar.add(Calendar.MILLISECOND,
            -TimeZone.getTimeZone(REQUESTOR_TIMEZONE_ID)
                .getOffset(this.requestorDateTime.getTime()));
        final Date expectedUtcDate = calendar.getTime();
        final Date actualUtcDate =
                TimeZoneConverter.calculateRequestorDateTime(this.requestorDate,
                    this.requestorTime, REQUESTOR_TIMEZONE_ID, false);
        Assert.assertEquals(expectedUtcDate, actualUtcDate);
        
        // backwards
        final Date calculatedRequestorDateTime =
                TimeZoneConverter.calculateRequestorDateTime(actualUtcDate,
                    new Time(actualUtcDate.getTime()), REQUESTOR_TIMEZONE_ID, true);
        Assert.assertEquals(this.requestorDateTime, calculatedRequestorDateTime);
    }
    
    /**
     * Test method for TimeZoneConverter.getDateValue().
     */
    public void testGetDateValue() {
        Assert.assertEquals(this.requestorDate,
            TimeZoneConverter.getDateValue(this.requestorDateTime));
    }
    
    /**
     * Test method for TimeZoneConverter.getTimeValue().
     */
    public void testGetTimeValue() {
        Assert.assertEquals(this.requestorTime,
            TimeZoneConverter.getTimeValue(this.requestorDateTime));
    }
    
    /**
     * Test Calendar and Date behavior when converting a date to UTC time zone.
     */
    public void testConvertToUtc() {
        final TimeZone requestedTimeZone = TimeZone.getTimeZone(HQ_TIMEZONE_ID);
        final Calendar localCalendar = Calendar.getInstance(requestedTimeZone);
        
        localCalendar.setTime(this.requestorDate);
        final Date windowStart =
                TimeZoneConverter.calculateDateTime(localCalendar.getTime(),
                    Constants.TIMEZONE_UTC, requestedTimeZone.getID(), false);
        
        Assert.assertTrue(windowStart.after(this.requestorDate));
    }
    
}
