package com.archibus.app.reservation.exchange.service;

import java.util.*;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.service.ReservationServiceTestBase;
import com.archibus.utility.ExceptionBase;

/**
 * Utility class. Provides methods to verify Exchange Calendar state.
 * <p>
 * Used by ExchangeCalendarServiceTest to verify test results.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class ExchangeCalendarVerifier {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private ExchangeCalendarVerifier() {
        
    }
    
    /**
     * Check equivalence between the local reservation and the corresponding appointment stored in
     * Exchange.
     * 
     * @param reservation the reservation to check
     * @param appointmentHelper the Exchange Appointment helper
     * @param serviceHelper the Exchange Service helper
     */
    public static void checkExchangeEquivalence(final RoomReservation reservation,
            final AppointmentHelper appointmentHelper, final ExchangeServiceHelper serviceHelper) {
        try {
            final Appointment appointment =
                    appointmentHelper.bindToAppointment(reservation.getEmail(),
                        reservation.getUniqueId());
            Assert.assertEquals(reservation.getUniqueId(), appointment.getICalUid());
            Assert.assertEquals(reservation.getReservationName(), appointment.getSubject());
            Assert.assertEquals(reservation.getRoomAllocations().get(0).getLocation(),
                appointment.getLocation());
            Assert.assertTrue(appointment.getBody().toString().contains(reservation.getComments()));
            final TimePeriod timePeriodUtc =
                    reservation.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC);
            Assert.assertEquals(timePeriodUtc.getStartDateTime(), appointment.getStart());
            Assert.assertEquals(timePeriodUtc.getEndDateTime(), appointment.getEnd());
            Assert.assertEquals(reservation.getEmail(), appointment.getOrganizer().getAddress());
            
            // Check the list of attendees.
            final List<String> emails = new ArrayList<String>();
            for (final String email : reservation.getAttendees().split(
                ReservationServiceTestBase.SEMICOLON)) {
                emails.add(email);
            }
            Assert.assertEquals(emails.size(), appointment.getRequiredAttendees().getCount());
            for (final Attendee attendee : appointment.getRequiredAttendees()) {
                Assert.assertTrue(emails.contains(attendee.getAddress()));
            }
            
            Assert.assertEquals(serviceHelper.getResourceAccount(), appointment.getResources()
                .getPropertyAtIndex(0).getAddress());
            
            // Check the reservation ID property and recurrence equivalence.
            Assert.assertEquals(reservation.getRecurrence() != null,
                appointment.getRecurrence() != null);
            if (reservation.getRecurrence() == null) {
                Assert.assertEquals(reservation.getReserveId(), appointmentHelper
                    .getAppointmentPropertiesHelper().getReservationId(appointment));
                Assert.assertNull(appointmentHelper.getAppointmentPropertiesHelper()
                    .getRecurringReservationIds(appointment));
            } else {
                Assert.assertEquals(reservation.getRecurrence().getEndDate(),
                    TimePeriod.clearTime(appointment.getRecurrence().getEndDate()));
                Assert.assertEquals(reservation.getRecurrence().getStartDate(),
                    TimePeriod.clearTime(appointment.getRecurrence().getStartDate()));
                Assert.assertNull(appointmentHelper.getAppointmentPropertiesHelper()
                    .getReservationIdFromUserProperty(appointment));
                Assert.assertEquals(reservation.getReserveId(), appointmentHelper
                    .getAppointmentPropertiesHelper().getReservationId(appointment));
                final Map<Date, Integer> reservationIds =
                        appointmentHelper.getAppointmentPropertiesHelper()
                            .getRecurringReservationIds(appointment);
                Assert.assertNotNull(reservationIds);
                Assert.assertEquals(reservation.getCreatedReservations().size(),
                    reservationIds.size());
                for (final RoomReservation createdReservation : reservation
                    .getCreatedReservations()) {
                    Assert.assertEquals(
                        createdReservation.getReserveId(),
                        reservationIds.get(createdReservation.getTimePeriodInTimeZone(
                            Constants.TIMEZONE_UTC).getStartDate()));
                }
            }
        } catch (final ServiceLocalException exception) {
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Verify the properties of a modified occurrence.
     * 
     * @param appointmentHelper the appointment helper
     * @param calendarEvent the calendar event corresponding with the occurrence to check
     * @param numberOfChanges number of changed occurrences in the series
     * @param occurrenceIndex index of the occurrence to check, corresponding with the calendarEvent
     * @param originalSubject original subject of the series
     */
    public static void verifyModifiedOccurrence(final AppointmentHelper appointmentHelper,
            final RoomReservation calendarEvent, final int numberOfChanges,
            final int occurrenceIndex, final String originalSubject) {
        try {
            // Bind to the master to check the number of changed occurrences.
            Appointment appointment =
                    appointmentHelper.bindToAppointment(calendarEvent.getEmail(),
                        calendarEvent.getUniqueId());
            Assert.assertEquals(AppointmentType.RecurringMaster, appointment.getAppointmentType());
            Assert.assertEquals(numberOfChanges, appointment.getModifiedOccurrences().getCount());
            Assert.assertEquals(originalSubject, appointment.getSubject());
            final TimePeriod timePeriodUtc =
                    calendarEvent.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC);
            Assert.assertEquals(timePeriodUtc.getStartDateTime(), appointment
                .getModifiedOccurrences().getPropertyAtIndex(numberOfChanges - 1).getStart());
            // Now bind to the occurrence to verify the actual changes.
            appointment =
                    Appointment.bindToOccurrence(appointment.getService(), appointment.getId(),
                        occurrenceIndex);
            Assert.assertEquals(timePeriodUtc.getStartDateTime(), appointment.getStart());
            Assert.assertEquals(calendarEvent.getReservationName(), appointment.getSubject());
            Assert.assertEquals(calendarEvent.getRoomAllocations().get(0).getLocation(),
                appointment.getLocation());
        } catch (final ExceptionBase exception) {
            Assert.fail(exception.toStringForLogging());
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Verify that binding to an occurrence fails.
     * @param appointment the master appointment in the series
     * @param index index of the occurrence to try to bind to
     */
    public static void verifyOccurrenceBindFails(final Appointment appointment, final int index) {
        try {
            try {
                Appointment.bindToOccurrence(appointment.getService(), appointment.getId(), index);
                Assert.fail("Should not be able to bind to a cancelled occurrence.");
            } catch (final ServiceResponseException e) {
                // OK if this specific error code is given, otherwise rethrow.
                if (!ServiceError.ErrorCalendarOccurrenceIsDeletedFromRecurrence.equals(e
                    .getResponse().getErrorCode())) {
                    throw e;
                }
            }
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Get the last modified time of the appointment linked to the given reservation object.
     * 
     * @param appointmentHelper the appointment helper to connect to Exchange
     * @param reservation the reservation linking to the appointment to check
     * @return the last modified date
     */
    public static Date getLastModifiedTime(final AppointmentHelper appointmentHelper,
            final RoomReservation reservation) {
        try {
            final Appointment appointment =
                    appointmentHelper.bindToAppointment(reservation.getEmail(),
                        reservation.getUniqueId());
            return appointment.getLastModifiedTime();
        } catch (ServiceLocalException exception) {
            throw new ExceptionBase("Error reading appointment property.", exception);
        }
    }
}
