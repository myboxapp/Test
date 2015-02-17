package com.archibus.app.reservation.service;

import java.sql.Time;
import java.text.ParseException;
import java.util.*;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.*;

import org.json.JSONObject;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.utility.ExceptionBase;

/**
 * Test for the RoomReservationService class.
 */ 
public class RoomReservationServiceTest extends RoomReservationServiceTestBase {
    
    /** The Constant END_TIME. */
    private static final String END_TIME = "1899-12-30 12:30:00";

    /** The Constant START_TIME. */
    private static final String START_TIME = "1899-12-30 10:00:00";

    /** The Constant RESERVATION_NAME_TEST_UPDATE. */
    private static final String RESERVATION_NAME_TEST_UPDATE = "test update";

    /** The Constant RESERVE_RM_TIME_END. */
    private static final String RESERVE_RM_TIME_END = "reserve_rm.time_end";

    /** The Constant RESERVE_RM_TIME_START. */
    private static final String RESERVE_RM_TIME_START = "reserve_rm.time_start";

    /** The Constant RESERVE_TIME_END. */
    private static final String RESERVE_TIME_END = "reserve.time_end";

    /** The Constant RESERVE_TIME_START. */
    private static final String RESERVE_TIME_START = "reserve.time_start";

    /** The Constant RESERVE_RESERVATION_NAME. */
    private static final String RESERVE_RESERVATION_NAME = "reserve.reservation_name"; 

    /** The Constant RESERVE_OUTLOOK_UNIQUE_ID. */
    private static final String RESERVE_OUTLOOK_UNIQUE_ID = "reserve.outlook_unique_id";

    /** The Constant RESERVE_RES_ID. */
    private static final String RESERVE_RES_ID = "reserve.res_id";

    /** The Constant RESERVE_DATE_END. */
    private static final String RESERVE_DATE_END = "reserve.date_end";

    /** The Constant RESERVE_DATE_START. */
    private static final String RESERVE_DATE_START = "reserve.date_start";

    /** The Constant CANCEL_MESSAGE. */
    private static final String CANCEL_MESSAGE = "Cancel message";

    /** Constant: 4. */
    private static final int FOUR = 4; 
        
    /** The reservation service. */
    protected ReservationService reservationService; 

    /**
     * Test save new room reservation.
     */
    public void testSaveNewRoomReservation() {
        final DataRecord record = createAndSaveRoomReservation(false);

        Assert.assertTrue(record.getInt(RESERVE_RES_ID) > 0);
        Assert.assertNotNull(record.getString(RESERVE_OUTLOOK_UNIQUE_ID));
    }

    /**
     * Test save new room reservation.
     */
    public void testSaveNewRecurringRoomReservation() {
        final DataRecord record = createAndSaveRoomReservation(true);

        Assert.assertTrue(record.getInt(RESERVE_RES_ID) > 0);
        final String iCalUid = record.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        Assert.assertNotNull(iCalUid);

        try {
            final List<RoomReservation> reservations =
                    this.roomReservationDataSource.getByUniqueId(iCalUid, Constants.TIMEZONE_UTC);
            for (final RoomReservation roomReservation : reservations) {
                final Appointment appointment =
                        this.appointmentHelper.bindToOccurrence(this.email, iCalUid,
                                roomReservation.getStartDateTime());
                Assert.assertNotNull(appointment);
                Assert.assertEquals(AppointmentType.Occurrence, appointment.getAppointmentType());
                checkEquivalence(roomReservation, appointment);
            }
        } catch (final ServiceLocalException exception) {
            throw new ExceptionBase(exception.toString(), exception);
        }
    }

    /**
     * Test updating a room reservation.
     */
    public void testUpdateRoomReservation() {
        final DataRecord reservation = createAndSaveRoomReservation(false);

        final int reservationId = reservation.getInt(RESERVE_RES_ID);
        final String uniqueId = reservation.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        Assert.assertTrue(reservationId > 0);
        Assert.assertNotNull(uniqueId);

        // Modify some properties.
        reservation.setValue(RESERVE_RESERVATION_NAME, RESERVATION_NAME_TEST_UPDATE);
        
        final DataRecord roomAllocation = createRoomAllocation();        
        this.checkAfterUpdate(reservation, roomAllocation, reservationId, uniqueId); 
    } 

    /**
     * Test updating a room reservation.
     */
    public void testUpdateReservationOccurrence() {
        final DataRecord reservation = createAndSaveRoomReservation(true);

        final int reservationId = reservation.getInt(RESERVE_RES_ID);
        final String uniqueId = reservation.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        reservation.setValue("reserve.res_parent", reservationId);
        Assert.assertTrue(reservationId > 0);
        Assert.assertNotNull(uniqueId);

        // Modify some properties.
        reservation.setValue(RESERVE_RESERVATION_NAME, RESERVATION_NAME_TEST_UPDATE);
        // Set the end date to the same date as the start date, so only that occurrence is modified.
        reservation.setValue(RESERVE_DATE_END, reservation.getValue(RESERVE_DATE_START));
        final DataRecord roomAllocation = createRoomAllocation();
        this.checkAfterUpdate(reservation, roomAllocation, reservationId, uniqueId); 

        // Check that the occurrence was updated and the others left unmodified.
        try {
            final List<RoomReservation> reservations =
                    this.roomReservationDataSource.getByUniqueId(uniqueId, Constants.TIMEZONE_UTC);
            for (final RoomReservation roomReservation : reservations) {
                Assert.assertEquals(Integer.valueOf(reservationId), roomReservation.getParentId());
                final Appointment appointment =
                        this.appointmentHelper.bindToOccurrence(this.email, uniqueId,
                                roomReservation.getStartDateTime());
                Assert.assertNotNull(appointment);

                // The modified occurrences are now of type Exception, the others are of type
                // Occurrence.
                // The reservations with an end time not rounded to the hour are the modified ones.
                final Calendar calendar = Calendar.getInstance();
                calendar.setTime(roomReservation.getEndTime());
                if (calendar.get(Calendar.MINUTE) == 0) {
                    Assert.assertEquals(AppointmentType.Occurrence, appointment.getAppointmentType());
                } else {
                    Assert.assertEquals(AppointmentType.Exception, appointment.getAppointmentType());
                }
                checkEquivalence(roomReservation, appointment);
            }
        } catch (final ServiceLocalException exception) {
            throw new ExceptionBase(exception.toString(), exception);
        }
    }

    /**
     * Test canceling a room reservation.
     */
    public void testCancelRoomReservation() {
        final DataRecord reservation = createAndSaveRoomReservation(false);

        final int reservationId = reservation.getInt(RESERVE_RES_ID);
        final String uniqueId = reservation.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        Assert.assertTrue(reservationId > 0);
        Assert.assertNotNull(uniqueId);

        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(this.email, uniqueId));

        this.roomReservationService.cancelRoomReservation(reservationId, CANCEL_MESSAGE);

        Assert.assertNull(this.reservationService.getActiveReservation(reservationId, null));
        Assert.assertNull(this.appointmentHelper.bindToAppointment(this.email, uniqueId));
    }

    /**
     * Test canceling a single reservation that is part of a recurring reservation.
     */
    public void testCancelSingleOcurrence() {
        try {
            // Create a simple recurring appointment.
            final DataRecord reservation = createAndSaveRoomReservation(true);
            final int reservationId = reservation.getInt(RESERVE_RES_ID);
            final String uniqueId = reservation.getString(RESERVE_OUTLOOK_UNIQUE_ID);
            Assert.assertTrue(reservationId > 0);
            Assert.assertNotNull(uniqueId);

            // Cancel a single occurrence: the first one
            this.roomReservationService.cancelRoomReservation(reservationId, CANCEL_MESSAGE);
            Assert.assertEquals(Constants.STATUS_CANCELLED,
                    this.roomReservationDataSource.get(reservationId).getStatus());
            Appointment appointment =
                    this.appointmentHelper.bindToAppointment(this.email, uniqueId);
            Assert.assertNotNull(appointment);
            Assert.assertEquals(AppointmentType.RecurringMaster, appointment.getAppointmentType());
            Assert.assertEquals(1, appointment.getDeletedOccurrences().getCount());
            Assert.assertEquals(
                    this.startDate,
                    TimePeriod.clearTime(appointment.getDeletedOccurrences().getPropertyAtIndex(0)
                            .getOriginalStart()));

            // Cancel a single occurrence: the fourth one.
            final List<RoomReservation> reservations =
                    this.roomReservationDataSource.getByParentId(reservationId, null, this.startDate);
            final int otherReservationId = reservations.get(FOUR - 1).getReserveId();
            this.roomReservationService.cancelRoomReservation(otherReservationId, "cancel");
            Assert.assertEquals(Constants.STATUS_CANCELLED,
                    this.roomReservationDataSource.get(otherReservationId).getStatus());
            appointment = this.appointmentHelper.bindToAppointment(this.email, uniqueId);
            Assert.assertNotNull(appointment);
            Assert.assertEquals(AppointmentType.RecurringMaster, appointment.getAppointmentType());
            Assert.assertEquals(2, appointment.getDeletedOccurrences().getCount());
            try {
                Appointment.bindToOccurrence(appointment.getService(), appointment.getId(), FOUR);
                Assert.fail("Should not be able to bind to a cancelled occurrence.");
            } catch (final ServiceResponseException e) {
                // OK if this specific error code is given, otherwise rethrow.
                if (!ServiceError.ErrorCalendarOccurrenceIsDeletedFromRecurrence.equals(e
                        .getResponse().getErrorCode())) {
                    throw e;
                }
            }

            // Cancel the same occurrence again.
            this.roomReservationService.cancelRoomReservation(otherReservationId, CANCEL_MESSAGE);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
    }

    /**
     * Test canceling a recurring reservation as a whole.
     */
    public void testCancelRecurringRoomReservation() {
        // Create a simple recurring reservation.
        final DataRecord reservation = createAndSaveRoomReservation(true);
        final int reservationId = reservation.getInt(RESERVE_RES_ID);
        final String uniqueId = reservation.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        Assert.assertTrue(reservationId > 0);
        Assert.assertNotNull(uniqueId);

        // Cancel the entire series.
        this.roomReservationService.cancelRecurringRoomReservation(reservationId, CANCEL_MESSAGE);

        Assert.assertNull(this.reservationService.getActiveReservation(reservationId, null));
        Assert.assertNull(this.appointmentHelper.bindToAppointment(this.email, uniqueId));
    }

    /**
     * Test getting attendees response status.
     */
    public void testGetAttendeesResponseStatus() {
        final DataRecord reservation = createAndSaveRoomReservation(false);
        final int reservationId = reservation.getInt(RESERVE_RES_ID);
        final List<JSONObject> responses =
                this.roomReservationService.getAttendeesResponseStatus(reservationId);
        Assert.assertFalse(responses.isEmpty());
    }
 
    
    /**
     * Sets the reservation service.
     *
     * @param reservationService the new reservation service
     */
    public void setReservationService(final ReservationService reservationService) {
        this.reservationService = reservationService;
    } 
    
    
    /**
     * Sets the date and time.
     *
     * @param reservation the reservation
     * @param roomAllocation the room allocation
     * @param reservationId the reservation id
     * @param uniqueId the unique id
     */
    private void checkAfterUpdate(final DataRecord reservation,
            final DataRecord roomAllocation, final int reservationId, final String uniqueId) {
        try {
            final Time startTime =
                    new Time(this.timeFormatter.parse(START_TIME).getTime());
            final Time endTime =
                    new Time(this.timeFormatter.parse(END_TIME).getTime());
            reservation.setValue(RESERVE_TIME_START, startTime);
            reservation.setValue(RESERVE_TIME_END, endTime);
            roomAllocation.setValue(RESERVE_RM_TIME_START, startTime);
            roomAllocation.setValue(RESERVE_RM_TIME_END, endTime);
        } catch (final ParseException exception) {
            Assert.fail(exception.toString());
        }
        
        final DataRecord reservationAfterUpdate =
                this.roomReservationService.saveRoomReservation(reservation, roomAllocation, null,
                        null);
        // Check that the IDs are the same.
        Assert.assertEquals(reservationId, reservationAfterUpdate.getInt(RESERVE_RES_ID));
        Assert.assertEquals(uniqueId, reservationAfterUpdate.getString(RESERVE_OUTLOOK_UNIQUE_ID));
    }

}
