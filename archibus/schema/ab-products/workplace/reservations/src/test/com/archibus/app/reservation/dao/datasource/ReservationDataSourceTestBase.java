package com.archibus.app.reservation.dao.datasource;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.DataSourceTestBase;
import com.archibus.utility.Utility;

/**
 * Test class for RoomAllocationDataSource.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a base class that provides a framework for other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class ReservationDataSourceTestBase extends DataSourceTestBase {
    
    /** The user creating the reservation and allocation. */
    protected static final String USER_ID = "AFM";
    
    /** Building identifier for the reservation. */
    protected static final String BL_ID = "HQ";
    
    /** Floor identifier for the reservation. */
    protected static final String FL_ID = "17";
    
    /** Room identifier for the reservation. */
    protected static final String RM_ID = "101";
    
    /**
     * The configuration identifier of the room arrangement.
     */
    protected static final String CONFIG_ID = "C";
    
    /** Arrangement type identifier for the reservation. */
    protected static final String ARRANGE_TYPE_ID = "CONFERENCE";
    
    /** unique key. */
    protected static final String UNIQUE_ID = "0003FFABCDDE00040B";
    
    /**
     * The number of days in advance to test saving room allocations for.
     */
    protected static final int DAYS_IN_ADVANCE = 7;
    
    /** Comment string for room allocation. */
    protected static final String ALLOCATION_COMMENTS = "Initial allocation comments.";
    
    /** Alternative comment string for room allocation. */
    protected static final String ALLOCATION_COMMENTS_CHANGED = "changed";
    
    /** time formatter. */
    protected final SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
        Locale.ENGLISH);
    
    /**
     * The RoomAllocationDataSource instance under test.
     */
    protected RoomAllocationDataSource roomAllocationDataSource;
    
    /** The room arrangement data source. */
    protected RoomArrangementDataSource roomArrangementDataSource;
    
    /** The resources data source. */
    protected ResourceDataSource resourceDataSource;
    
    /**
     * The RoomAllocationDataSource instance under test.
     */
    protected ResourceAllocationDataSource resourceAllocationDataSource;
    
    /** The data source used for testing. */
    protected RoomReservationDataSource roomReservationDataSource;
    
    /**
     * The existing room reservation used for testing.
     */
    protected RoomReservation existingReservation;
    
    /**
     * Set up for a ReservationDataSource test case.
     * 
     * @throws Exception when setup fails
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method throws the same exception.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        
        final Calendar cal = Calendar.getInstance();
        cal.setTime(Utility.currentDate());
        cal.add(Calendar.DATE, DAYS_IN_ADVANCE);
        
        final Date startDate = TimePeriod.clearTime(cal.getTime());
        
        final Time startTime = new Time(this.timeFormatter.parse("1899-12-30 10:00:00").getTime());
        final Time endTime = new Time(this.timeFormatter.parse("1899-12-30 14:00:00").getTime());
        
        final RoomArrangement roomArrangement =
                this.roomArrangementDataSource.get(BL_ID, FL_ID, RM_ID, CONFIG_ID, ARRANGE_TYPE_ID);
        
        Assert.assertTrue(roomArrangement.getAnnounceDays() <= DAYS_IN_ADVANCE);
        
        this.existingReservation =
                createReservation(new TimePeriod(startDate, startDate, startTime, endTime),
                    roomArrangement);
    }
    
    /**
     * Sets the room allocation data source for verifying the room reservation data source behavior.
     * 
     * @param roomAllocationDataSource the room allocation data source for this test
     */
    public void setRoomAllocationDataSource(final RoomAllocationDataSource roomAllocationDataSource) {
        this.roomAllocationDataSource = roomAllocationDataSource;
    }
    
    /**
     * Set the ResourceAllocationDataSource.
     * 
     * @param resourceAllocationDataSource the data source
     */
    public void setResourceAllocationDataSource(
            final ResourceAllocationDataSource resourceAllocationDataSource) {
        this.resourceAllocationDataSource = resourceAllocationDataSource;
    }
    
    /**
     * Set the room arrangement data source for testing.
     * 
     * @param roomArrangementDataSource the room arrangement data source for this test
     */
    public void setRoomArrangementDataSource(
            final RoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    }
    
    /**
     * Set the RoomReservationDataSource.
     * 
     * @param roomReservationDataSource the data source to set
     */
    public void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }
    
    /**
     * Set the ResourceDataSource.
     * 
     * @param resourceDataSource the data source to set
     */
    public void setResourceDataSource(final ResourceDataSource resourceDataSource) {
        this.resourceDataSource = resourceDataSource;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "reservation-datasources.xml" };
    }
    
    /**
     * Create a reservation for the given time period and room arrangement.
     * 
     * @param timePeriod the time period
     * @param roomArrangement the room arrangement to reserve
     * @return the newly created room reservation
     */
    protected RoomReservation createReservation(final TimePeriod timePeriod,
            final RoomArrangement roomArrangement) {
        final RoomReservation reservation = new RoomReservation(timePeriod, roomArrangement);
        reservation.setReservationName("Test");
        reservation.setRequestedBy(USER_ID);
        reservation.setRequestedFor(USER_ID);
        reservation.setCreatedBy(USER_ID);
        reservation.setResourceAllocations(new ArrayList<ResourceAllocation>());
        reservation.getRoomAllocations().get(0).setComments(ALLOCATION_COMMENTS);
        
        reservation.setUniqueId(UNIQUE_ID);
        this.roomReservationDataSource.save(reservation);
        return this.roomReservationDataSource.get(reservation.getReserveId());
    }
    
}
