package com.archibus.app.reservation.service;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.common.organization.domain.Employee;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.recurrence.*;
import com.archibus.utility.LocalDateTimeUtil;

/**
 * Base class fore reservation services.
 * 
 * @author bv
 * @since 20.1
 * 
 *        <p>
 *        Suppress warning "PMD.TestClassWithoutTestCases".
 *        <p>
 *        Justification: this is a base class for other tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class ReservationServiceTestBase extends AbstractReservationServiceTestBase {
    
    /** Semicolon is used as a list separator. */
    public static final String SEMICOLON = ";";
    
    /** test. */
    protected static final String TEST = "test";
    
    /** Dummy unique ID for testing. */
    protected static final String UNIQUE_ID = "12345678";
    
    /** Dummy reservation name. */
    protected static final String RESERVATION_NAME = "test name";
    
    /** confirmed. */
    protected static final String CONFIRMED = "Confirmed";
    
    /** regular. */
    protected static final String TYPE_REGULAR = "regular";
    
    /** email. */
    protected static final String AFM_EMAIL = "afm@tgd.com";
    
    /** The site id. */
    protected static final String SITE_ID = "MARKET";
    
    /** Number of days in a week. */
    protected static final int DAYS_IN_WEEK = 7;
    
    /** The reservation service. */
    protected IReservationService reservationService;
    
    /** The reservation service. */
    protected CancelReservationService cancelReservationService;    
    
    /** The employee service. */
    protected IEmployeeService employeeService;
    
    /** The space service. */
    protected ISpaceService spaceService;
    
    /** The time period. */
    protected TimePeriod timePeriod;
    
    /**
     * Set up time for a test case.
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
        this.timePeriod = new TimePeriod(startDate, endDate, startTime, endTime);
    }
    
    /**
     * 
     * Create Room Reservation.
     * 
     * @return Room Reservation.
     */
    protected RoomReservation createRoomReservation() {
        final String timeZone = LocalDateTimeUtil.getLocationTimeZone(null, null, null, BL_ID);
        
        this.timePeriod.setTimeZone(timeZone);
        
        final RoomReservation roomReservation =
                new RoomReservation(timePeriod, BL_ID, FL_ID, RM_ID, CONFIG_ID, ARRANGE_TYPE_ID);
        
        final Employee creator = this.employeeService.findEmployee(AFM_EMAIL);
        roomReservation.setCreator(creator);
        
        roomReservation.setReservationName(TEST);
        roomReservation.setReservationType(TYPE_REGULAR);
        roomReservation
            .setAttendees("jason@mailinator.com;martin@mailinator.com;linda@mailinator.com");
        
        return roomReservation;
    }
    
    /**
     * Make the given reservation reservation recurring, without saving it to the database. Use
     * dummy reservation id's based on the reservation id of the given reservation.
     * 
     * @param reservation the reservation to make recurring
     */
    protected void addRecurrence(final RoomReservation reservation) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(reservation.getStartDate());
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) {
            calendar.add(Calendar.DATE, 1);
        }
        reservation.setStartDate(new java.sql.Date(calendar.getTimeInMillis()));
        reservation.setEndDate(reservation.getStartDate());
        calendar.add(Calendar.MONTH, 1);
        final Date recurrenceEndDate = calendar.getTime();
        final List<com.archibus.app.reservation.domain.recurrence.DayOfTheWeek> dayOfTheWeek =
                new ArrayList<com.archibus.app.reservation.domain.recurrence.DayOfTheWeek>();
        dayOfTheWeek.add(com.archibus.app.reservation.domain.recurrence.DayOfTheWeek.Tuesday);
        final WeeklyPattern recurrence =
                new WeeklyPattern(reservation.getStartDate(), recurrenceEndDate, 1, dayOfTheWeek);
        
        // Add reservations for each occurrence to the main reservation object.
        final List<RoomReservation> createdReservations = new ArrayList<RoomReservation>();
        createdReservations.add(reservation);
        
        calendar.setTime(reservation.getStartDate());
        calendar.add(Calendar.DATE, DAYS_IN_WEEK);
        int reservationId = reservation.getReserveId() + 1;
        while (!calendar.getTime().after(recurrenceEndDate)) {
            final RoomReservation reservationOccurrence =
                    new RoomReservation(reservation.getTimePeriod(), reservation
                        .getRoomAllocations().get(0).getRoomArrangement());
            reservationOccurrence.setReserveId(reservationId);
            reservationOccurrence.setStartDate(new java.sql.Date(calendar.getTimeInMillis()));
            reservationOccurrence.setEndDate(reservationOccurrence.getStartDate());
            createdReservations.add(reservationOccurrence);
            calendar.add(Calendar.DATE, DAYS_IN_WEEK);
            ++reservationId;
        }
        reservation.setCreatedReservations(createdReservations);
        reservation.setRecurrence(recurrence);
    }
    
    /**
     * Add daily recurrence to the reservation object, without saving it to the database. Use
     * dummy reservation id's based on the reservation id of the given reservation.
     * 
     * @param reservation the reservation to make recurring
     * @param count number of occurrences to add to the first one
     */
    protected void addDailyRecurrence(final RoomReservation reservation, final int count) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(reservation.getStartDate());
        calendar.add(Calendar.DATE, count);
        final Date recurrenceEndDate = calendar.getTime();
        
        final DailyPattern recurrence = new DailyPattern(reservation.getStartDate(), 1, count);

        // Add reservations for each occurrence to the main reservation object.
        final List<RoomReservation> createdReservations = new ArrayList<RoomReservation>();
        createdReservations.add(reservation);
        
        calendar.setTime(reservation.getStartDate());
        calendar.add(Calendar.DATE, 1);        
        
        int reservationId = reservation.getReserveId() + 1;
        
        calendar.setTime(reservation.getStartDate());
        calendar.add(Calendar.DATE, 1);
        while (!calendar.getTime().after(recurrenceEndDate)) {
            final RoomReservation reservationOccurrence =
                    new RoomReservation(reservation.getTimePeriod(), reservation
                        .getRoomAllocations().get(0).getRoomArrangement());
            reservationOccurrence.setReserveId(reservationId);
            reservationOccurrence.setStartDate(calendar.getTime());
            reservationOccurrence.setEndDate(reservationOccurrence.getStartDate());
            createdReservations.add(reservationOccurrence);
            calendar.add(Calendar.DATE, 1);
            ++reservationId;
        }
        reservation.setCreatedReservations(createdReservations);
        reservation.setRecurrence(recurrence);
        Assert.assertEquals(count + 1, createdReservations.size());
    }
    
    /**
     * Add and remove attendees in the reservation.
     * 
     * @param reservation the reservation to edit
     */
    public void changeAttendees(final RoomReservation reservation) {
        // Add & remove attendees.
        final List<String> emails = new ArrayList<String>();
        for (final String email : reservation.getAttendees().split(SEMICOLON)) {
            emails.add(email);
        }
        emails.remove(0);
        final StringBuffer attendees = new StringBuffer("martin.johnson@mailinator.com");
        for (final String email : emails) {
            attendees.append(';');
            attendees.append(email);
        }
        reservation.setAttendees(attendees.toString());
    }
    
    /**
     * Set the reservation service for this test.
     * 
     * @param reservationService the new reservation service
     */
    public final void setReservationService(final IReservationService reservationService) {
        this.reservationService = reservationService;
    }
    
    /**
     * Set the employee service for this test.
     * 
     * @param employeeService the new employee service
     */
    public final void setEmployeeService(final IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    /**
     * Set the space service for this test.
     * 
     * @param spaceService the new space service
     */
    public final void setSpaceService(final ISpaceService spaceService) {
        this.spaceService = spaceService;
    }

    
    /**
     * Sets the cancel reservation service.
     *
     * @param cancelReservationService the new cancel reservation service
     */
    public void setCancelReservationService(
            final CancelReservationService cancelReservationService) {
        this.cancelReservationService = cancelReservationService;
    }
    
    
    
}
