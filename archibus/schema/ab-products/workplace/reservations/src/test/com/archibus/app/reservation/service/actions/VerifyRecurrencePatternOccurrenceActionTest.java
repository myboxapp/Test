package com.archibus.app.reservation.service.actions;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.*;

import com.archibus.app.reservation.TestData;
import com.archibus.app.reservation.domain.*;

/**
 * Test for SaveRecurringReservationOccurrenceAction.
 */
public class VerifyRecurrencePatternOccurrenceActionTest extends TestCase {
    // Disable StrictDuplicate CHECKSTYLE warning. Justification: setup test
    
    /** The occurrence action under test. */
    private VerifyRecurrencePatternOccurrenceAction action;
    
    /** Map of reservations that must be verified by the occurrence action. */
    private Map<Date, RoomReservation> reservationMap;
    
    /** The first date of the reservation series. */
    private Date firstDate;
    
    /**
     * Set up for test case.
     * 
     * @throws Exception when setup fails
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.reservationMap = new HashMap<Date, RoomReservation>();
        
        // create new dummy reservation
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        Date startDate = dateFormatter.parse("2011-11-09");
        Date endDate = startDate;
        this.firstDate = startDate;
        
        final Time startTime = new Time(timeFormatter.parse("1899-12-30 10:00:00").getTime());
        final Time endTime = new Time(timeFormatter.parse("1899-12-30 14:00:00").getTime());
        
        // CHECKSTYLE:OFF Justification: this 'magic number' is used for testing.
        for (int daysToAdd = 0; daysToAdd < 5; ++daysToAdd) {
            // CHECKSTYLE:ON
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.firstDate);
            calendar.add(Calendar.DATE, daysToAdd);
            startDate = calendar.getTime();
            endDate = startDate;
            final RoomReservation reservation =
                    new RoomReservation(new TimePeriod(startDate, endDate, startTime, endTime),
                        TestData.BL_ID, TestData.FL_ID, TestData.RM_ID, TestData.CONFIG_ID,
                        TestData.ARRANGE_TYPE_ID);
            reservation.setReservationName("Test");
            reservation.setRequestedBy(TestData.USER_ID);
            reservation.setRequestedFor(TestData.USER_ID);
            reservation.setCreatedBy(TestData.USER_ID);
            reservation.setUniqueId(TestData.UNIQUE_ID);
            this.reservationMap.put(startDate, reservation);
        }
        
        this.action =
                new VerifyRecurrencePatternOccurrenceAction(startTime, endTime, this.reservationMap);
    }
    
    /**
     * Test method for SaveRecurringReservationOccurrenceAction.handleOccurrence().
     */
    public void testHandleOccurrence() {
        try {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.firstDate);
            for (int daysToAdd = 0; daysToAdd < this.reservationMap.size(); ++daysToAdd) {
                calendar.setTime(this.firstDate);
                calendar.add(Calendar.DATE, daysToAdd);
                final Date occurrenceDate = calendar.getTime();
                Assert.assertTrue(this.action.handleOccurrence(occurrenceDate));
                Assert.assertNull(this.action.getFirstDateWithoutReservation());
            }
            // check the next day: this should return false
            calendar.add(Calendar.DATE, 1);
            final Date dateOutsidePattern = calendar.getTime();
            Assert.assertFalse(this.action.handleOccurrence(dateOutsidePattern));
            Assert.assertEquals(dateOutsidePattern, this.action.getFirstDateWithoutReservation());
        } catch (final ReservationException exception) {
            fail(exception.toString());
        }
    }
}
