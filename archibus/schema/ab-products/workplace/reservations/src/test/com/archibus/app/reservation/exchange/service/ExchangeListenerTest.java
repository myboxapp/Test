package com.archibus.app.reservation.exchange.service;

import java.util.*;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.service.RoomReservationServiceTestBase;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.utility.LocalDateTimeUtil;

/**
 * Test class for ExchangeListener.
 * 
 * @author Yorik Gerlo
 */
public class ExchangeListenerTest extends RoomReservationServiceTestBase {
    
    /** Users table. */
    private static final String AFM_USERS = "afm_users";

    /** The constant 30. */
    private static final int THIRTY = 30;
    
    /** Message included when cancelling an appointment. */
    private static final String CANCEL_MESSAGE = "Cancel for listener test.";
    
    /** Table + field name of the unique id field. */
    private static final String RESERVE_OUTLOOK_UNIQUE_ID = "reserve.outlook_unique_id";
    
    /** Constant: slack time to allow the email server to process messages. */
    private static final int SLEEP_MILLIS = 5000;
    
    /** The user name that corresponds with the email address of the meeting organizer. */
    private String usernameForEmail;
    
    /** The Exchange Listener under test. */
    private ExchangeListener exchangeListener;
    
    /** Calendar service to modify the appointments directly. */
    private ExchangeCalendarService calendarService;
    
    /** The location time zone. */
    private String locationTimeZone;

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
    public void onSetUp() throws Exception {
        super.onSetUp();
        final DataSource userDs =
                DataSourceFactory.createDataSourceForFields(AFM_USERS, new String[] {
                        "user_name", Constants.EMAIL_FIELD_NAME });
        userDs.addRestriction(Restrictions.eq(AFM_USERS, Constants.EMAIL_FIELD_NAME, this.email));
        final DataRecord record = userDs.getRecord();
        this.usernameForEmail = record.getString("afm_users.user_name");
        this.locationTimeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, null, BL_ID);
    }
    
    /**
     * Test processing the update and cancellation of a single appointment.
     */
    public void testProcessSingleAppointment() {
        final ExchangeService resourceExchangeService =
                this.serviceHelper.initializeService(this.serviceHelper
                    .getResourceAccount());
        
        final RoomReservation reservation = createAndProcessReservation(resourceExchangeService);
        changeReservationViaExchange(reservation, resourceExchangeService);
        checkAgainstDatabase(reservation);
        
        // Allow time for the accept message to reach the organizer's mailbox.
        sleep();
        
        // Cancel the meeting via ExchangeCalendarService.
        final RoomReservation roomReservation = new RoomReservation();
        roomReservation.setEmail(reservation.getEmail());
        roomReservation.setUniqueId(reservation.getUniqueId());
        roomReservation.setRoomAllocations(reservation.getRoomAllocations());
        this.calendarService.cancelAppointment(roomReservation, CANCEL_MESSAGE);
        
        // Allow for some slack time.
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        
        // Check that the reservation was cancelled by the creator.
        final RoomReservation cancelledReservation =
                this.roomReservationDataSource.get(reservation.getReserveId());
        Assert.assertEquals(Constants.STATUS_CANCELLED, cancelledReservation.getStatus());
        Assert.assertEquals(this.usernameForEmail, cancelledReservation.getLastModifiedBy());
    }
    
    /**
     * Test the scenario where the Exchange listener declines an update because the reservation
     * could not be updated.
     */
    public void testUpdateViaExchangeFailed() {
        final ExchangeService resourceExchangeService =
                this.serviceHelper.initializeService(this.serviceHelper
                    .getResourceAccount());
        final RoomReservation reservation = createAndProcessReservation(resourceExchangeService);
        changeReservationViaExchange(reservation, resourceExchangeService);
        // Check that the reservation was updated.
        checkAgainstDatabase(reservation);
        
        // Create a second reservation.
        final RoomReservation secondReservation =
                createAndProcessReservation(resourceExchangeService);
        final RoomReservation unmodifiedReservation =
                this.roomReservationDataSource.getActiveReservation(
                    secondReservation.getReserveId(), null);
        changeReservationViaExchange(secondReservation, resourceExchangeService);
        // Check that the reservation was not updated and the resource account has declined.
        // Allow time for the decline message to reach the organizer's mailbox.
        sleep();
        sleep();
        
        final Appointment appointment =
                this.appointmentHelper.bindToAppointment(secondReservation.getEmail(),
                    secondReservation.getUniqueId());
        try {
            Assert.assertEquals(MeetingResponseType.Decline, appointment.getResources()
                .getPropertyAtIndex(0).getResponseType());
        } catch (final ServiceLocalException exception) {
            Assert.fail(exception.toString());
        }
        checkAgainstDatabase(unmodifiedReservation);
        
        // Cancel the meeting that could not be updated via ExchangeCalendarService.
        final RoomReservation roomReservation = new RoomReservation();
        roomReservation.setEmail(secondReservation.getEmail());
        roomReservation.setUniqueId(secondReservation.getUniqueId());
        roomReservation.setRoomAllocations(secondReservation.getRoomAllocations());
        this.calendarService.cancelAppointment(roomReservation, CANCEL_MESSAGE);
        
        // Allow for some slack time.
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        
        // Check that the reservation was cancelled by the creator.
        final RoomReservation cancelledReservation =
                this.roomReservationDataSource.get(secondReservation.getReserveId());
        Assert.assertEquals(Constants.STATUS_CANCELLED, cancelledReservation.getStatus());
        Assert.assertEquals(this.usernameForEmail, cancelledReservation.getLastModifiedBy());
    }
    
    /**
     * Create a regular reservation and process it with the Exchange Listener.
     * 
     * @param resourceExchangeService the ExchangeService to use for processing Exchange events
     * @return the created reservation object
     */
    private RoomReservation createAndProcessReservation(
            final ExchangeService resourceExchangeService) {
        // Create a meeting with reservation via the RoomReservationService.
        final DataRecord record = createAndSaveRoomReservation(false);
        final String uniqueId = record.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        final Integer reservationId = record.getInt("reserve.res_id");
        // Check that the meeting is present in the database.
        final RoomReservation reservation =
                this.roomReservationDataSource.getActiveReservation(reservationId,
                    Constants.TIMEZONE_UTC);
        Assert.assertNotNull(reservation);
        // Allow for some slack time.
        sleep();
        
        this.exchangeListener.processInbox(resourceExchangeService);
        
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(
            this.serviceHelper.getResourceAccount(), uniqueId));
        
        return reservation;
    }
    
    /**
     * Change the given reservation via Exchange.
     * 
     * @param reservation the reservation to modify
     * @param resourceExchangeService the service to use for processing Exchange events
     */
    private void changeReservationViaExchange(final RoomReservation reservation,
            final ExchangeService resourceExchangeService) {
        // Change the meeting via Exchange.
        reservation.setReservationName("test update via Exchange");
        reservation.setComments("Updated appointment body.");
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(reservation.getStartDateTime());
        calendar.add(Calendar.DATE, 1);
        reservation.setStartDate(calendar.getTime());
        reservation.setEndDate(reservation.getStartDate());
        reservation.convertToTimeZone(locationTimeZone);
        this.calendarService.updateAppointment(reservation);
        
        // Allow for some slack time.
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
    }
    
    /**
     * Check the given reservation object against the same reservation in the database.
     * 
     * @param reservation the reservation to compare with the stored copy
     */
    private void checkAgainstDatabase(final RoomReservation reservation) {
        final RoomReservation updatedReservation =
                this.roomReservationDataSource.getActiveReservation(reservation.getReserveId(),
                    Constants.TIMEZONE_UTC);
        reservation.convertToTimeZone(Constants.TIMEZONE_UTC);
        Assert.assertEquals(reservation.getReservationName(),
            updatedReservation.getReservationName());
        Assert.assertEquals(reservation.getStartDateTime(), updatedReservation.getStartDateTime());
        Assert.assertEquals(reservation.getEndDateTime(), updatedReservation.getEndDateTime());
        Assert.assertEquals(reservation.getComments(), updatedReservation.getComments());
    }
    
    /**
     * Test processing the update and cancellation of an entire recurring appointment series.
     */
    public void testProcessRecurringAppointment() {
        final List<RoomReservation> createdReservations = setupRecurringMeeting();
        
        final ExchangeService resourceExchangeService =
                this.serviceHelper.initializeService(this.serviceHelper
                    .getResourceAccount());
        
        // Update the subject and attendees.
        final RoomReservation firstReservation = createdReservations.get(0);
        firstReservation.setReservationName("Update series subject via Exchange");
        firstReservation
            .setAttendees("jason.matthews@mailinator.com;joan.mitch@mailinator.com;kim.sim@mailinator.com");
        firstReservation.setTimeZone(LocalDateTimeUtil.getLocationTimeZone(null, null, null,
            BL_ID));
        this.calendarService.updateAppointment(firstReservation);
        
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        // Check that all occurrences have the modified name and attendees.
        for (final RoomReservation reservation : createdReservations) {
            final RoomReservation updatedReservation =
                    this.roomReservationDataSource.get(reservation.getReserveId());
            Assert.assertEquals(firstReservation.getReservationName(),
                updatedReservation.getReservationName());
            Assert.assertEquals(firstReservation.getAttendees(), updatedReservation.getAttendees());
        }
        
        // Allow some additional time for the accept to reach the organizer.
        sleep();
        // Cancel the meeting via ExchangeCalendarService.
        this.calendarService.cancelAppointment(createdReservations.get(0), CANCEL_MESSAGE);
        
        // Allow for some slack time.
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        
        // Check that all reservations were cancelled by the creator.
        for (final RoomReservation reservation : createdReservations) {
            final RoomReservation cancelledReservation =
                    this.roomReservationDataSource.get(reservation.getReserveId());
            Assert.assertEquals(Constants.STATUS_CANCELLED, cancelledReservation.getStatus());
            Assert.assertEquals(this.usernameForEmail, cancelledReservation.getLastModifiedBy());
        }
    }
    
    /**
     * Test processing the cancellation of a number of occurrences in a recurring appointment
     * series.
     */
    public void testCancelAppointmentOccurrence() {
        final List<RoomReservation> createdReservations = setupRecurringMeeting();
        final ExchangeService resourceExchangeService =
                this.serviceHelper.initializeService(this.serviceHelper
                    .getResourceAccount());
        
        // Cancel three occurrences via ExchangeCalendarService.
        final List<RoomReservation> reservationsToCancel = new ArrayList<RoomReservation>();
        reservationsToCancel.add(createdReservations.remove(0));
        reservationsToCancel.add(createdReservations.remove(1));
        reservationsToCancel.add(createdReservations.remove(1));
        this.calendarService.cancelAppointmentOccurrence(reservationsToCancel.get(1),
            CANCEL_MESSAGE);
        this.calendarService.cancelAppointmentOccurrence(reservationsToCancel.get(0),
            CANCEL_MESSAGE);
        this.calendarService.cancelAppointmentOccurrence(reservationsToCancel.get(2),
            CANCEL_MESSAGE);
        
        // Allow for some slack time.
        sleep();
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        
        // Check that the three occurrences were cancelled by the creator.
        for (final RoomReservation reservation : reservationsToCancel) {
            final RoomReservation storedReservation =
                    this.roomReservationDataSource.get(reservation.getReserveId());
            Assert.assertEquals(Constants.STATUS_CANCELLED, storedReservation.getStatus());
            Assert.assertEquals(this.usernameForEmail, storedReservation.getLastModifiedBy());
        }
        
        // Check that the other reservations were not cancelled.
        for (final RoomReservation reservation : createdReservations) {
            final RoomReservation storedReservation =
                    this.roomReservationDataSource.get(reservation.getReserveId());
            Assert.assertTrue(Constants.STATUS_AWAITING_APP.equals(storedReservation.getStatus())
                    || Constants.STATUS_CONFIRMED.equals(storedReservation.getStatus()));
            Assert.assertNull(storedReservation.getLastModifiedBy());
        }
        
    }
    
    /**
     * Test processing the update and cancellation of a number of occurrences in a recurring
     * appointment series.
     */
    public void testProcessAppointmentOccurrence() {
        final List<RoomReservation> createdReservations = setupRecurringMeeting();
        final ExchangeService resourceExchangeService =
                this.serviceHelper.initializeService(this.serviceHelper
                    .getResourceAccount());
        // Update the first occurrence via Exchange.
        RoomReservation reservationToUpdate = createdReservations.get(0);
        final RoomReservation originalReservation = new RoomReservation();
        reservationToUpdate.copyTo(originalReservation, true);
        originalReservation.setRoomAllocations(reservationToUpdate.getRoomAllocations());
        reservationToUpdate.setReservationName("First updated occurrence");
        reservationToUpdate.setAttendees("tom.winter@mailinator.com");
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(reservationToUpdate.getStartDateTime());
        calendar.add(Calendar.DATE, -1);
        calendar.add(Calendar.HOUR, 1);
        reservationToUpdate.setStartDateTime(calendar.getTime());
        reservationToUpdate.setEndDate(reservationToUpdate.getStartDate());
        reservationToUpdate.setTimeZone(LocalDateTimeUtil.getLocationTimeZone(null, null, null,
            BL_ID));
        this.calendarService
            .updateAppointmentOccurrence(reservationToUpdate, originalReservation);
        
        // Cancel the second occurrence via Exchange.
        this.calendarService
            .cancelAppointmentOccurrence(createdReservations.get(1), CANCEL_MESSAGE);
        createdReservations.get(1).setStatus(Constants.STATUS_CANCELLED);
        
        // Update the second last occurrence via Exchange.
        reservationToUpdate = createdReservations.get(createdReservations.size() - 2);
        reservationToUpdate.copyTo(originalReservation, true);
        originalReservation.setRoomAllocations(reservationToUpdate.getRoomAllocations());
        reservationToUpdate.setReservationName("Second last updated occurrence");
        reservationToUpdate.setAttendees("joan.mitch@mailinator.com;tom.winters@mailinator.com");
        calendar.setTime(reservationToUpdate.getStartDateTime());
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MINUTE, THIRTY);
        reservationToUpdate.setStartDateTime(calendar.getTime());
        reservationToUpdate.setEndDate(reservationToUpdate.getStartDate());
        reservationToUpdate.setTimeZone(LocalDateTimeUtil.getLocationTimeZone(null, null, null,
            BL_ID));
        this.calendarService
            .updateAppointmentOccurrence(reservationToUpdate, originalReservation);
        
        // Allow for some slack time.
        sleep();
        this.exchangeListener.processInbox(resourceExchangeService);
        
        // Check that all reservations are now in the database as expected (i.e. matching the
        // objects changed locally).
        for (final RoomReservation reservation : createdReservations) {
            RoomReservation storedReservation =
                    this.roomReservationDataSource.getActiveReservation(reservation.getReserveId(),
                        Constants.TIMEZONE_UTC);
            if (Constants.STATUS_CANCELLED.equals(reservation.getStatus())) {
                Assert.assertNull(storedReservation);
                storedReservation = this.roomReservationDataSource.get(reservation.getReserveId());
            } else {
                // This one has not been converted to UTC yet. Do it before comparing date/time.
                reservation.convertToTimeZone(Constants.TIMEZONE_UTC);
                Assert.assertEquals(reservation.getStartDateTime(),
                    storedReservation.getStartDateTime());
                Assert.assertEquals(reservation.getEndDateTime(),
                    storedReservation.getEndDateTime());
            }
            Assert.assertEquals(reservation.getStatus(), storedReservation.getStatus());
            Assert.assertEquals(reservation.getReservationName(),
                storedReservation.getReservationName());
            Assert.assertEquals(reservation.getAttendees(), storedReservation.getAttendees());
        }
    }
    
    /**
     * Set the Exchange listener service.
     * 
     * @param exchangeListener the exchangeListener to set
     */
    public void setExchangeListener(final ExchangeListener exchangeListener) {
        this.exchangeListener = exchangeListener;
    }
    
    /**
     * Set the Calendar Service to test.
     * 
     * @param calendarService the calendar service
     */
    public void setCalendarService(final ExchangeCalendarService calendarService) {
        this.calendarService = calendarService;
    }
    
    /**
     * Specific Spring configuration for this test to avoid proxies.
     * {@inheritDoc}
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "context\\core\\core-infrastructure.xml", "appContext-test.xml",
                "exchange-listener.xml" };
    }
    
    /**
     * Set up a recurring meeting in the database and on the calendar.
     * 
     * @return the list of created reservations (in building time)
     */
    private List<RoomReservation> setupRecurringMeeting() {
        // Create a meeting with reservation via the RoomReservationService.
        final DataRecord record = createAndSaveRoomReservation(true);
        final String uniqueId = record.getString(RESERVE_OUTLOOK_UNIQUE_ID);
        
        final List<RoomReservation> createdReservations =
                this.roomReservationDataSource.getByUniqueId(uniqueId, null);
        Assert.assertTrue(createdReservations.size() > 2);
        // Allow for some slack time.
        sleep();
        sleep();
        
        this.exchangeListener.processInbox(this.serviceHelper
            .initializeService(this.serviceHelper.getResourceAccount()));
        
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(
            this.serviceHelper.getResourceAccount(), uniqueId));
        return createdReservations;
    }
    
    /**
     * Sleep for a short while to give the listener some time to respond.
     */
    private void sleep() {
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (final InterruptedException exception) {
            this.logger.warn("Sleep was interrupted.");
        }
    }
    
}
