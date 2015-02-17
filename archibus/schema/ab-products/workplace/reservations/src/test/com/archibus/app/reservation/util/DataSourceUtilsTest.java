package com.archibus.app.reservation.util;

import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.*;

/**
 * Test for DataSourceUtils.
 */
public class DataSourceUtilsTest extends DataSourceTestBase {
    
    /**
     * Expected difference in days to use for testing.
     */
    private static final int EXPECTED_DAYS_DIFFERENCE = 24;
    
    /**
     * Combined date and time value of the requestor.
     */
    private Date requestorDateTime;
    
    /**
     * Set up for a test case for data source utils.
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
        final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        this.requestorDateTime = timeFormatter.parse("2012-05-02 18:30:00");
    }
    
    /**
     * Test method for DataSourceUtils.IsEmployeeEmail().
     */
    public void testIsEmployeeEmail() {
        assertTrue("This is an employee email.", DataSourceUtils.isEmployeeEmail("ai@tgd.com"));
        assertFalse("This is not an employee email.",
            DataSourceUtils.isEmployeeEmail("unknown@example.com"));
    }
    
    /**
     * Test method for DataSourceUtils.getDaysDifference().
     */
    public void testGetDaysDifference() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.requestorDateTime);
        calendar.add(Calendar.DATE, EXPECTED_DAYS_DIFFERENCE);
        
        final ResourceAllocation allocation = new ResourceAllocation();
        allocation.setStartDate(calendar.getTime());
        Assert.assertEquals(EXPECTED_DAYS_DIFFERENCE,
            DataSourceUtils.getDaysDifference(allocation,
                TimePeriod.clearTime(this.requestorDateTime)));
        Assert.assertEquals(0, DataSourceUtils.getDaysDifference(allocation, calendar.getTime()));
    }
    
}
