package com.archibus.app.reservation.exchange.util;

import java.util.*;

import microsoft.exchange.webservices.data.*;

import org.springframework.security.util.StringUtils;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.utility.StringUtil;

/**
 * Utility class. Provides methods to check equivalence between an Exchange appointment and the
 * corresponding reservation.
 * <p>
 * 
 * Used by ItemHandler to verify whether incoming requests have relevant changes.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class AppointmentEquivalenceChecker {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private AppointmentEquivalenceChecker() {
    }
    
    /**
     * Compare the appointment with the reservation regarding date, time, duration, subject and
     * attendees. Update the reservation object if any of the properties is different and
     * updateReservation is true. If we are not updating the reservation object, also check location
     * equivalence.
     * 
     * @param reservation the reservation linked to the appointment
     * @param appointment the appointment on the organizer's calendar
     * @param updateReservation true to update the reservation, false to not change anything
     * @return true if equal, false if different
     */
    public static boolean isEquivalent(final IReservation reservation,
            final Appointment appointment, final boolean updateReservation) {
        try {
            final boolean dateTimeEqual =
                    compareDateTime(reservation, appointment, updateReservation);
            
            boolean subjectEqual = true;
            if (!appointment.getSubject().equals(reservation.getReservationName())) {
                subjectEqual = false;
                if (updateReservation) {
                    reservation.setReservationName(appointment.getSubject());
                }
            }
            
            final boolean attendeesEqual =
                    compareAttendees(reservation, appointment, updateReservation);
            
            final boolean bodyEqual = compareBody(reservation, appointment, updateReservation);
            /*
             * Since we never change the location when updating appointment -> reservation,
             * only compare the location if we are not updating the reservation.
             */
            final boolean bodyAndLocationEqual =
                    bodyEqual
                            && (updateReservation || isLocationEquivalent(reservation, appointment));
            
            return dateTimeEqual && subjectEqual && attendeesEqual && bodyAndLocationEqual;
        } catch (final ServiceLocalException exception) {
            // @translatable
            throw new CalendarException("Error accessing appointment properties.", exception,
                AppointmentEquivalenceChecker.class);
        }
    }
    
    /**
     * Compare the location line of the appointment with the location in the reservation.
     * 
     * @param reservation the reservation object
     * @param appointment the appointment object
     * @return true if equivalent, false if different
     * @throws ServiceLocalException when the appointment location property could not be read
     */
    private static boolean isLocationEquivalent(final IReservation reservation,
            final Appointment appointment) throws ServiceLocalException {
        boolean equivalent = true;
        if (reservation instanceof RoomReservation) {
            final RoomReservation roomReservation = (RoomReservation) reservation;
            final List<RoomAllocation> allocations = roomReservation.getRoomAllocations();
            
            equivalent = appointment.getLocation().equals(allocations.get(0).getLocation());
        }
        return equivalent;
    }
    
    /**
     * Compare the start and end date/time of the appointment to the reservation. Update the
     * reservation object if differences are found and updateReservation is true.
     * 
     * @param reservation the reservation object
     * @param appointment the Exchange appointment
     * @param updateReservation true to update the reservation object, false to only compare
     * @return true if equivalent, false if different
     * @throws ServiceLocalException when an error occurs accessing the appointment properties
     */
    private static boolean compareDateTime(final IReservation reservation,
            final Appointment appointment, final boolean updateReservation)
            throws ServiceLocalException {
        boolean dateTimeEqual = true;
        
        // Get the UTC time period. Do not convert again if already in UTC.
        TimePeriod timePeriod = reservation.getTimePeriod();
        if (!Constants.TIMEZONE_UTC.equals(reservation.getTimeZone())) {
            timePeriod = reservation.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC);
        }

        if (!timePeriod.getStartDateTime().equals(appointment.getStart())) {
            dateTimeEqual = false;
            if (updateReservation) {
                reservation.setStartDateTime(appointment.getStart());
            }
        }
        if (!timePeriod.getEndDateTime().equals(appointment.getEnd())) {
            dateTimeEqual = false;
            if (updateReservation) {
                reservation.setEndDateTime(appointment.getEnd());
            }
        }
        return dateTimeEqual;
    }
    
    /**
     * Compare the attendees set in the appointment and reservation. Update the reservation object
     * if differences are found and updateReservation is true.
     * 
     * @param reservation the reservation
     * @param appointment the appointment
     * @param updateReservation true to update the reservation if differences are found
     * @return true if equal, false if different
     * @throws ServiceLocalException when an error occurs in the EWS library
     */
    private static boolean compareAttendees(final IReservation reservation,
            final Appointment appointment, final boolean updateReservation)
            throws ServiceLocalException {
        // check the attendees
        final SortedSet<String> appointmentAttendees = new TreeSet<String>();
        for (final Attendee attendee : appointment.getRequiredAttendees()) {
            appointmentAttendees.add(attendee.getAddress());
        }
        for (final Attendee attendee : appointment.getOptionalAttendees()) {
            appointmentAttendees.add(attendee.getAddress());
        }

        final boolean attendeesEqual =
                compareToReservationAttendees(reservation, appointmentAttendees);
        
        if (!attendeesEqual && updateReservation) {
            // Set the updated list of attendees in the reservation.
            final StringBuffer buffer = new StringBuffer();
            for (final String attendee : appointmentAttendees) {
                buffer.append(attendee);
                buffer.append(';');
            }
            reservation.setAttendees(buffer.substring(0, buffer.length() - 1));
        }
        return attendeesEqual;
    }
    
    /**
     * Compare the attendees in the given reservation to the given set of email addresses. Ignores
     * the presence of the organizer email in the list of exchange attendees if it's not already in
     * the list of reservation attendees.
     * 
     * @param reservation the reservation for which to compare attendees
     * @param appointmentAttendees the list of attendees
     * @return true if equivalent, false if different
     */
    private static boolean compareToReservationAttendees(final IReservation reservation,
            final SortedSet<String> appointmentAttendees) {
        
        final SortedSet<String> reservationAttendees = new TreeSet<String>();
        if (reservation.getAttendees() != null) {
            final String[] attendees = reservation.getAttendees().split(";");
            for (final String attendee : attendees) {
                if (StringUtil.notNullOrEmpty(attendee)) {
                    reservationAttendees.add(attendee);
                }
            }
        }
        if (!reservationAttendees.contains(reservation.getEmail())) {
            /*
             * Don't count the organizer as an attendee when checking equivalence. He's listed as an
             * attendee in the meeting if he doesn't have a mailbox on the connected Exchange
             * server. In WebCentral the organizer shouldn't be in the list of attendees.
             */
            appointmentAttendees.remove(reservation.getEmail());
        }
        
        return appointmentAttendees.size() == reservationAttendees.size()
                && appointmentAttendees.containsAll(reservationAttendees)
                && reservationAttendees.containsAll(appointmentAttendees);
    }
    
    /**
     * Compare the body text of the appointment with the reservation comments. If it's different and
     * updateReservation is true, update the reservation comments.
     * 
     * @param reservation the reservation
     * @param appointment the appointment
     * @param updateReservation true to update the reservation if changes are found
     * @return true if equal, false if different
     * @throws ServiceLocalException when the body property cannot be read
     */
    private static boolean compareBody(final IReservation reservation,
            final Appointment appointment, final boolean updateReservation)
            throws ServiceLocalException {
        boolean equal = true;
        final String body = extractPlainTextBody(appointment);
        final String comments = StringUtils.notNull(reservation.getComments());
        if (!comments.equals(body)) {
            if (updateReservation) {
                reservation.setComments(body);
            }
            equal = false;
        }
        return equal;
    }
    
    /**
     * Extract the plain text body from the appointment.
     * 
     * @param appointment the appointment to get the body for
     * @return plain text body content
     * @throws ServiceLocalException when the body cannot be read
     */
    private static String extractPlainTextBody(final Appointment appointment)
            throws ServiceLocalException {
        final MessageBody body = appointment.getBody();
        String bodyText = body.toString();
        if (BodyType.HTML.equals(body.getBodyType())) {
            // remove all html tags and convert non-blank spaces to normal spaces
            bodyText = bodyText.replaceAll("\\<[^>]*>", "");
            bodyText = bodyText.replaceAll("&nbsp;", " ");
        }
        return bodyText.trim();
    }
    
}
