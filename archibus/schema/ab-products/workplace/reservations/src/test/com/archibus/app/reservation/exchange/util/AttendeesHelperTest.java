package com.archibus.app.reservation.exchange.util;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.Appointment;

import com.archibus.app.reservation.ConfiguredDataSourceTestBase;
import com.archibus.app.reservation.domain.RoomReservation;
import com.archibus.app.reservation.exchange.service.ExchangeServiceHelper;

/**
 * Test class for AttendeesHelper.
 *
 * @author Yorik Gerlo
 * @since 21.2
 */
public class AttendeesHelperTest extends ConfiguredDataSourceTestBase {

    /** The servive helper used in this test. */
    private ExchangeServiceHelper serviceHelper;
    
    /**
     * Test setting attendees in an appointment starting from a reservation.
     */
    public void testSetAttendees() {
        try {
            final RoomReservation reservation = new RoomReservation();
            final Appointment appointment =
                    new Appointment(
                        this.serviceHelper.initializeService(this.serviceHelper
                        .getOrganizerAccount()));
            
            // Test with null attendees.
            AttendeesHelper.setAttendees(reservation, appointment,
                this.serviceHelper.getResourceAccount(), this.serviceHelper.getOrganizerAccount());
            
            // Test with empty attendees.
            reservation.setAttendees("");
            AttendeesHelper.setAttendees(reservation, appointment,
                this.serviceHelper.getResourceAccount(), this.serviceHelper.getOrganizerAccount());
            
            // Test with empty attendee entries.
            reservation.setAttendees(";tim@mailinator.com;;john@mailinator.com;;;");
            AttendeesHelper.setAttendees(reservation, appointment,
                this.serviceHelper.getResourceAccount(), this.serviceHelper.getOrganizerAccount());
            Assert.assertEquals(2, appointment.getRequiredAttendees().getCount());

            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Set the Exchange service helper used in this test.
     * 
     * @param serviceHelper the new service helper
     */
    public void setServiceHelper(final ExchangeServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }

}
