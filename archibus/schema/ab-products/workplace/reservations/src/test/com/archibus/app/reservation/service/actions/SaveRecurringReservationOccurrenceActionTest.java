package com.archibus.app.reservation.service.actions;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.TestData;
import com.archibus.app.reservation.dao.IRoomArrangementDataSource;
import com.archibus.app.reservation.dao.datasource.RoomReservationDataSource;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.DataSourceTestBase;

/**
 * Test for SaveRecurringReservationOccurrenceAction.
 */
public class SaveRecurringReservationOccurrenceActionTest extends DataSourceTestBase {
    
    /** The room reservation data source used for testing. */
    private RoomReservationDataSource roomReservationDataSource;
    
    /** The room arrangement data source used for testing. */
    private IRoomArrangementDataSource roomArrangementDataSource;
    
    /** The occurrence action under test. */
    private SaveRecurringReservationOccurrenceAction action;
    
    /** List to store the saved reservation created by the occurrence action. */
    private List<RoomReservation> savedReservations;
    
    /** The first reservation to be duplicated by the occurrence action. */
    private RoomReservation firstReservation;
    
    /**
     * Set up for a Room Reservation test case.
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
        this.savedReservations = new ArrayList<RoomReservation>();
        
        // create new dummy reservation
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        final Date startDate = dateFormatter.parse("2011-11-09");
        final Date endDate = startDate;
        
        final Time startTime = new Time(timeFormatter.parse("1899-12-30 10:00:00").getTime());
        final Time endTime = new Time(timeFormatter.parse("1899-12-30 14:00:00").getTime());
        
        this.firstReservation =
                new RoomReservation(new TimePeriod(startDate, endDate, startTime, endTime),
                    TestData.BL_ID, TestData.FL_ID, TestData.RM_ID, TestData.CONFIG_ID,
                    TestData.ARRANGE_TYPE_ID);
        this.firstReservation.setReservationName("Test");
        this.firstReservation.setRequestedBy(TestData.USER_ID);
        this.firstReservation.setRequestedFor(TestData.USER_ID);
        this.firstReservation.setCreatedBy(TestData.USER_ID);
        this.firstReservation.setUniqueId(TestData.UNIQUE_ID);
        this.roomReservationDataSource.save(this.firstReservation);
        
        this.action =
                new SaveRecurringReservationOccurrenceAction(this.savedReservations,
                    this.roomReservationDataSource, this.roomArrangementDataSource,
                    this.firstReservation);
    }
    
    /**
     * Test method for SaveRecurringReservationOccurrenceAction.handleOccurrence().
     */
    public void testHandleOccurrence() {
        try {
            // test saving an additional occurrence on a different date
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, TestData.DAYS_IN_ADVANCE);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            final Date occurrenceDate = calendar.getTime();
            Assert.assertTrue(this.action.handleOccurrence(occurrenceDate));
            Assert.assertEquals(1, this.savedReservations.size());
            final RoomReservation savedReservation = this.savedReservations.get(0);
            Assert.assertEquals(occurrenceDate, savedReservation.getStartDate());
            Assert.assertEquals(occurrenceDate, savedReservation.getEndDate());
            Assert.assertEquals(this.firstReservation.getStartTime(),
                savedReservation.getStartTime());
            Assert.assertEquals(this.firstReservation.getEndTime(), savedReservation.getEndTime());
            Assert
                .assertNotNull("The saved reservation is available via the data source.",
                    this.roomReservationDataSource.getActiveReservation(savedReservation
                        .getReserveId()));
            
            // try booking the same time period again, this shouldn't work.
            try {
                this.action.handleOccurrence(occurrenceDate);
                Assert.fail("Double booking should throw an exception.");
            } catch (final ReservableNotAvailableException exception) {
                Assert.assertEquals(TestData.RM_ID,
                    ((RoomArrangement) exception.getReservable()).getRmId());
            }
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
    
    /**
     * Setter for the roomReservationDataSource property.
     * 
     * @see roomReservationDataSource
     * @param roomReservationDataSource the roomReservationDataSource to set
     */
    public void setRoomReservationDataSource(
            final RoomReservationDataSource roomReservationDataSource) {
        this.roomReservationDataSource = roomReservationDataSource;
    }
    
    /**
     * Setter for the roomArrangementDataSource property.
     * 
     * @see roomArrangementDataSource
     * @param roomArrangementDataSource the roomArrangementDataSource to set
     */
    public void setRoomArrangementDataSource(
            final IRoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "reservation-service-actions.xml" };
    }
    
}
