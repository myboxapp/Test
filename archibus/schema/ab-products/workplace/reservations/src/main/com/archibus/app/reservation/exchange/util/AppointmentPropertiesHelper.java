package com.archibus.app.reservation.exchange.util;

import java.text.*;
import java.util.*;

import microsoft.exchange.webservices.data.*;

import org.apache.log4j.Logger;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.utility.ExceptionBase;

/**
 * Provides access to the user properties in an Exchange appointment that are used for managing
 * reservations.
 * 
 * @author Yorik Gerlo
 * 
 */
public class AppointmentPropertiesHelper {
    
    /**
     * Name of the Appointment user property that contains the identifier of the reservation linked
     * to the appointment.
     */
    private static final String RESERVATION_ID_PROPERTYNAME = "ReservationID-Archibus";
    
    /**
     * Name of the Appointment user property that indicates the identifiers of reservations linked
     * to a recurring appointment, stored in the recurrence master appointment. Each occurrence is
     * linked to its reservation via the occurrence's original date.
     */
    private static final String RECURRING_RESERVATION_IDS_PROPERTYNAME =
            "RecurringReservationIDs-Archibus";
    
    /** Date format used in the user properties. */
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    
    /**
     * Separator used in the recurring reservation IDs property, between the original date and the
     * reservation ID of each occurrence.
     */
    private static final String DATE_SEPARATOR = "|";
    
    /**
     * Regular expression pattern format of the separator used in the recurring reservation IDs
     * property, between the original date and the reservation ID of each occurrence.
     */
    private static final String DATE_SEPARATOR_PATTERN = "\\|";
    
    /** MAPI Property ID of the ICal UI Property. */
    private static final int UID_PROPERTY = 0x03;
    
    /** The date formatter used for all Date <-> String conversions. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING,
        Locale.ENGLISH);
    
    /**
     * Extended set of properties to retrieve via EWS when binding to an Appointment, including the
     * relevant user properties also used by the Outlook Plugin.
     */
    private final PropertySet extendedPropertySet;
    
    /** Definition of the reservation ID user property. */
    private final ExtendedPropertyDefinition reservationIdProperty;
    
    /** Definition of the recurring reservation IDs user property. */
    private final ExtendedPropertyDefinition recurringReservationIdsProperty;
    
    /** Definition of the ical UID property. */
    private final ExtendedPropertyDefinition icalUidProperty;
    
    /** The logger. */
    private final Logger logger = Logger.getLogger(this.getClass());
    
    /**
     * Create an instance of the Appointment Properties Helper.
     */
    public AppointmentPropertiesHelper() {
        try {
            this.reservationIdProperty =
                    new ExtendedPropertyDefinition(DefaultExtendedPropertySet.PublicStrings,
                        RESERVATION_ID_PROPERTYNAME, MapiPropertyType.Integer);
            this.recurringReservationIdsProperty =
                    new ExtendedPropertyDefinition(DefaultExtendedPropertySet.PublicStrings,
                        RECURRING_RESERVATION_IDS_PROPERTYNAME, MapiPropertyType.StringArray);
            this.icalUidProperty =
                    new ExtendedPropertyDefinition(DefaultExtendedPropertySet.Meeting,
                        UID_PROPERTY, MapiPropertyType.Binary);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error setting up appointment user properties.", exception,
                AppointmentPropertiesHelper.class);
        }
        this.extendedPropertySet =
                new PropertySet(PropertySet.FirstClassProperties.getBasePropertySet(),
                    this.reservationIdProperty, this.recurringReservationIdsProperty);
    }
    
    /**
     * Get the set of properties that must be used for binding to appointments via EWS. This set
     * includes the relevant user properties.
     * 
     * @return the extendedPropertySet
     */
    public PropertySet getExtendedPropertySet() {
        return this.extendedPropertySet;
    }
    
    /**
     * Get the reservation ID stored in the given appointment's user property. Do not perform a
     * lookup for recurring appointments.
     * 
     * @param appointment the appointment to get the reservation date from
     * @return the reservation ID stored in the user property, or null if the property doesn't exist
     * @throws ServiceLocalException when an EWS error occurs
     */
    public Integer getReservationIdFromUserProperty(final Appointment appointment)
            throws ServiceLocalException {
        final Object value = findPropertyValue(appointment, this.reservationIdProperty);
        Integer reservationId = null;
        if (value instanceof Integer) {
            reservationId = (Integer) value;
        }
        return reservationId;
    }
    
    /**
     * Get the reservation ID from the appointment's user properties. For recurring appointments,
     * look in the master appointment if required.
     * 
     * @param appointment the appointment to get the reservation date from
     * @return the reservation ID stored in the appointment, or null if the property doesn't exist
     */
    public Integer getReservationId(final Appointment appointment) {
        try {
            final AppointmentType appointmentType = appointment.getAppointmentType();
            Integer reservationId = null;
            if (AppointmentType.RecurringMaster.equals(appointmentType)) {
                // A recurrence master does not have a reservation id property.
                // Get the reservation ID from the first date in the recurrence
                // tracking state.
                reservationId =
                        getRecurringReservationIds(appointment).get(
                            TimePeriod.clearTime(appointment.getStart()));
            } else if (AppointmentType.Occurrence.equals(appointmentType)) {
                // Bind to the master and check there.
                final Appointment master =
                        Appointment.bindToRecurringMaster(appointment.getService(),
                            appointment.getId(), getExtendedPropertySet());
                reservationId =
                        getRecurringReservationIds(master).get(
                            TimePeriod.clearTime(appointment.getStart()));
            } else {
                // Check the property in the given appointment.
                reservationId = getReservationIdFromUserProperty(appointment);
                
                // If not found and the appointment is an Exception, check in
                // the master.
                if (reservationId == null && AppointmentType.Exception.equals(appointmentType)) {
                    final Appointment master =
                            Appointment.bindToRecurringMaster(appointment.getService(),
                                appointment.getId(), getExtendedPropertySet());
                    // Use the ICalRecurrenceId, that is the original date of the occurrence.
                    reservationId =
                            getRecurringReservationIds(master).get(
                                TimePeriod.clearTime(appointment.getICalRecurrenceId()));
                }
            }
            return reservationId;
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException("Error reading reservation ID properties in appointment.", exception,
                AppointmentPropertiesHelper.class);
        }
    }
    
    /**
     * Set the reservation ID in the appointment's user properties.
     * 
     * @param appointment the appointment to modify
     * @param reservationId the reservation id to set
     */
    public void setReservationId(final Appointment appointment, final Integer reservationId) {
        try {
            appointment.setExtendedProperty(this.reservationIdProperty, reservationId);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException("Error setting reservation ID in appointment.", exception,
                AppointmentPropertiesHelper.class);
        }
    }
    
    /**
     * Remove the reservation ID user property.
     * 
     * @param appointment the appointment to remove the reservation id from
     */
    public void removeReservationId(final Appointment appointment) {
        try {
            appointment.removeExtendedProperty(this.reservationIdProperty);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException("Error removing reservation ID from appointment.", exception,
                AppointmentPropertiesHelper.class);
        }
    }
    
    /**
     * Get the recurrence reservation IDs from the appointment's user property.
     * 
     * @param appointment the appointment to get the dates from
     * @return a map with keys representing the occurrences' original date and values indicating
     *         their reservation id
     */
    public Map<Date, Integer> getRecurringReservationIds(final Appointment appointment) {
        Map<Date, Integer> reservationIds = null;
        final Object value = findPropertyValue(appointment, this.recurringReservationIdsProperty);
        if (value instanceof Object[]) {
            reservationIds = new HashMap<Date, Integer>();
            
            for (final Object pair : (Object[]) value) {
                final String[] splitPair = pair.toString().split(DATE_SEPARATOR_PATTERN);
                if (splitPair.length == 2) {
                    try {
                        final Date originalDate = this.dateFormat.parse(splitPair[0]);
                        final Integer reservationId = Integer.valueOf(splitPair[1]);
                        reservationIds.put(originalDate, reservationId);
                    } catch (final ParseException exception) {
                        // ignore this pair
                        this.logger.warn("Invalid date '" + splitPair[0]
                                + "' in recurring reservation ids.", exception);
                    }
                } else {
                    this.logger.warn("No date separator '" + DATE_SEPARATOR + "' in '" + pair
                            + "'.");
                }
            }
        }
        
        return reservationIds;
    }
    
    /**
     * Remove the recurring reservation IDs user property.
     * 
     * @param appointment the appointment to remove the recurring reservation ids from
     */
    public void removeRecurringReservationIds(final Appointment appointment) {
        try {
            appointment.removeExtendedProperty(this.recurringReservationIdsProperty);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException(
                "Error removing recurring reservation IDs from appointment.", exception,
                AppointmentPropertiesHelper.class);
        }
    }
    
    /**
     * Set the recurring reservation ids user property in the appointment.
     * 
     * @param appointment the recurring appointment to set the exception dates for
     * @param reservation master reservation containing the reservations created according to the
     *            recurrence pattern (in building time)
     */
    public void setRecurringReservationIds(final Appointment appointment,
            final RoomReservation reservation) {
        final Map<Date, Integer> reservationIds = getReservationIds(reservation);
        final ArrayList<String> serializedReservationIds =
                new ArrayList<String>(reservationIds.size());
        for (final Map.Entry<Date, Integer> entry : reservationIds.entrySet()) {
            final String originalDate = this.dateFormat.format(entry.getKey());
            final String reservationId = String.valueOf(entry.getValue());
            serializedReservationIds.add(originalDate + DATE_SEPARATOR + reservationId);
        }
        
        try {
            appointment.setExtendedProperty(this.recurringReservationIdsProperty,
                serializedReservationIds.toArray(new String[serializedReservationIds.size()]));
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            // @translatable
            throw new CalendarException("Error setting recurring reservation IDs in appointment.", exception,
                AppointmentPropertiesHelper.class);
        }
    }
    
    /**
     * Get the reservation IDs to be stored in the appointment property.
     * 
     * @param roomReservation the main reservation that contains the reservation IDs
     * @return map of reservation IDs by their UTC start date/time
     */
    private Map<Date, Integer> getReservationIds(final RoomReservation roomReservation) {
        final Map<Date, Integer> reservationIds = new HashMap<Date, Integer>();
        
        final List<RoomReservation> createdReservations = roomReservation.getCreatedReservations();
        
        if (createdReservations == null) {
            // only add the ID of the main reservation
            reservationIds.put(roomReservation.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC)
                .getStartDate(), roomReservation.getReserveId());
        } else {
            // add the IDs of all created reservations
            for (final RoomReservation createdReservation : createdReservations) {
                reservationIds.put(
                    createdReservation.getTimePeriodInTimeZone(Constants.TIMEZONE_UTC)
                        .getStartDate(), createdReservation.getReserveId());
            }
        }
        return reservationIds;
    }
    
    /**
     * Get the property definition for the ICal UID property.
     * 
     * @return the property definition of the ICal UID property.
     */
    public ExtendedPropertyDefinition getIcalUidPropertyDefinition() {
        return this.icalUidProperty;
    }
    
    /**
     * Find the value of the user property with the given name in the appointment's user properties.
     * 
     * @param appointment the appointment
     * @param propertyDef definition of the user property to get the value for
     * @return the value, or null if it doesn't exist
     */
    private static Object findPropertyValue(final Appointment appointment,
            final ExtendedPropertyDefinition propertyDef) {
        Object propertyValue = null;
        try {
            for (final ExtendedProperty property : appointment.getExtendedProperties()) {
                if (propertyDef.equals(property.getPropertyDefinition())) {
                    propertyValue = property.getValue();
                    break;
                }
            }
        } catch (final ServiceLocalException exception) {
            // @translatable
            throw new CalendarException("Error reading extended appointment properties", exception,
                AppointmentPropertiesHelper.class);
        }
        return propertyValue;
    }
    
}
