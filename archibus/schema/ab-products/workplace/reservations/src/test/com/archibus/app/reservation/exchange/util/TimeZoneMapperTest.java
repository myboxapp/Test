package com.archibus.app.reservation.exchange.util;

import junit.framework.*;

/**
 * Test for the TimeZoneMapper.
 * 
 * @author Yorik Gerlo
 */
public class TimeZoneMapperTest extends TestCase {
    
    /** A Windows time zone ID used for testing. */
    private static final String UTC = "UTC";
    
    /** An Olson time zone ID used for testing. */
    private static final String ETC_GMT = "Etc/GMT";
    
    /** A Windows time zone ID used for testing. */
    private static final String ROMANCE_STANDARD_TIME = "Romance Standard Time";
    
    /** A Windows time zone ID used for testing. */
    private static final String TOKYO_STANDARD_TIME = "Tokyo Standard Time";
    
    /** A Windows time zone ID used for testing. */
    private static final String EASTERN_STANDARD_TIME = "Eastern Standard Time";
    
    /**
     * Test the constructor.
     */
    public void testTimeZoneMapper() {
        Assert.assertNotNull(new TimeZoneMapper());
    }
    
    /**
     * Test converting from Windows to Olson IDs.
     */
    public void testGetOlsonId() {
        final TimeZoneMapper mapper = new TimeZoneMapper();
        Assert.assertEquals("America/New_York", mapper.getOlsonId(EASTERN_STANDARD_TIME));
        Assert.assertEquals("America/Los_Angeles", mapper.getOlsonId("Pacific Standard Time"));
        Assert.assertEquals("Asia/Katmandu", mapper.getOlsonId("Nepal Standard Time"));
        Assert.assertEquals("Asia/Tokyo", mapper.getOlsonId(TOKYO_STANDARD_TIME));
        Assert.assertEquals("Asia/Shanghai", mapper.getOlsonId("China Standard Time"));
        Assert.assertEquals("Europe/Paris", mapper.getOlsonId(ROMANCE_STANDARD_TIME));
        Assert.assertEquals(ETC_GMT, mapper.getOlsonId(UTC));
        
        Assert.assertNull(mapper.getOlsonId(""));
        Assert.assertNull(mapper.getOlsonId(null));
    }
    
    /**
     * Test converting from Olson to Windows IDs.
     */
    public void testGetWindowsId() {
        final TimeZoneMapper mapper = new TimeZoneMapper();
        
        Assert.assertEquals(ROMANCE_STANDARD_TIME, mapper.getWindowsId("Europe/Brussels"));
        Assert.assertEquals(EASTERN_STANDARD_TIME, mapper.getWindowsId("America/Detroit"));
        Assert.assertEquals(TOKYO_STANDARD_TIME, mapper.getWindowsId("Japan"));
        Assert.assertEquals(EASTERN_STANDARD_TIME, mapper.getWindowsId("EST"));
        Assert.assertEquals("Morocco Standard Time", mapper.getWindowsId("Africa/Casablanca"));
        Assert.assertEquals("UTC+12", mapper.getWindowsId("Pacific/Kwajalein"));
        Assert.assertEquals(UTC, mapper.getWindowsId(ETC_GMT));
        
        Assert.assertEquals("Greenwich Standard Time", mapper.getWindowsId(""));
        Assert.assertNull(mapper.getWindowsId(null));
    }
    
}
