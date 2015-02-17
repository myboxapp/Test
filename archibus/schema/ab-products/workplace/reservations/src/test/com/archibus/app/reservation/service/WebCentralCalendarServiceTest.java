package com.archibus.app.reservation.service;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.domain.*;
import com.archibus.utility.ExceptionBase;

/**
 * Test class for WebCentralCalendarService.
 */
public class WebCentralCalendarServiceTest extends ReservationServiceTestBase {
    
    /** First email address used for testing. */
    private static final String EMAIL1 = "afm@tgd.com";
    
    /** The Calendar service instance being tested. */
    private WebCentralCalendarService webCentralCalendarService;
    
    /**
     * Set the WebCentral calendar service.
     * 
     * @param webCentralCalendarService the new WebCentral calendar service
     */
    public void setWebCentralCalendarService(
            final WebCentralCalendarService webCentralCalendarService) {
        this.webCentralCalendarService = webCentralCalendarService;
    }
    
    /**
     * Test getting attendee availability.
     */
    public void testAttendeeAvailibility() {
        final TimeZone timeZone = TimeZone.getDefault();
        
        final List<ICalendarEvent> events =
                this.webCentralCalendarService.findAttendeeAvailability(this.startDate,
                    this.endDate, timeZone, EMAIL1);
        
        Assert.assertNotNull(events);
    }
    
    /**
     * Test sending an invitation for a new appointment.
     */
    public void testCreateAppointment() {
        final RoomReservation roomReservation = createRoomReservation();
        final String uniqueId = this.webCentralCalendarService.createAppointment(roomReservation);
        
        Assert.assertEquals("", uniqueId);
    }
    
    /**
     * Test sending an update invitation.
     */
    public void testUpdateAppointment() {
        final RoomReservation roomReservation = createRoomReservation();
        try {
            this.webCentralCalendarService.updateAppointment(roomReservation);
        } catch (final ExceptionBase exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test sending a cancellation.
     */
    public void testCancelAppointment() {
        final RoomReservation roomReservation = createRoomReservation();
        try {
            this.webCentralCalendarService.cancelAppointment(roomReservation,
                "reservation cancelled");
        } catch (final ExceptionBase exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Test getting attendee response status.
     */
    public void testGetAttendeesResponseStatus() {
        final RoomReservation reservation = createRoomReservation();
        final List<AttendeeResponseStatus> responses =
                this.webCentralCalendarService.getAttendeesResponseStatus(reservation);
        Assert.assertEquals(reservation.getAttendees().split(";").length, responses.size());
        for (final AttendeeResponseStatus response : responses) {
            Assert.assertEquals(AttendeeResponseStatus.ResponseStatus.Unknown,
                response.getResponseStatus());
        }
    }
    
}
