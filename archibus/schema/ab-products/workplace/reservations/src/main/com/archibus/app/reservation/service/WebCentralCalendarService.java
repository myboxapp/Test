package com.archibus.app.reservation.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONObject;

import com.archibus.app.common.organization.domain.Employee;
import com.archibus.app.reservation.dao.IVisitorDataSource;
import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.domain.AttendeeResponseStatus.ResponseStatus;
import com.archibus.app.reservation.service.helpers.WebCentralCalendarServiceHelper;
import com.archibus.app.reservation.util.*;
import com.archibus.context.ContextStore;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.eventhandler.reservations.ReservationsCommonHandler;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.model.view.datasource.ClauseDef.Operation;
import com.archibus.model.view.datasource.ParsedRestrictionDef;
import com.archibus.utility.ExceptionBase;
import com.archibus.utility.StringUtil;

/**
 * The Class WebCentralCalendarService.
 */
public class WebCentralCalendarService extends RoomReservationServiceBase implements ICalendarService {  
    
    /** The Constant SPACE. */
    private static final String SPACE = " ";

    /** Invitation type for new invitations. */
    private static final String TYPE_NEW = "new";

    /** Invitation type for cancel invitations. */
    private static final String TYPE_CANCEL = "cancel";

    /** Invitation type for invitation updates. */
    private static final String TYPE_UPDATE = "update";

    /** Percent symbol for LIKE restrictions. */
    private static final String PERCENT = "%";

    /** Name of the table containing reservations. */
    private static final String RESERVE_TABLE = "reserve";

    /** The employee service. */
    private IEmployeeService employeeService;

    /** The visitors data source. */
    private IVisitorDataSource visitorDataSource; 
    
    /** {@inheritDoc} */
    public void checkServiceAvailable() throws ExceptionBase {        
        // always available, don't throw an exception
    }

    /** {@inheritDoc} */
    public List<ICalendarEvent> findAttendeeAvailability(final Date startDate, final Date endDate,
            final TimeZone timeZone, final String email) throws ExceptionBase {

        final List<ICalendarEvent> events = new ArrayList<ICalendarEvent>();
        if (!StringUtil.isNullOrEmpty(email)) {
            this.roomReservationDataSource.clearRestrictions();
            this.roomReservationDataSource.addRestriction(Restrictions.or(
                Restrictions.like(RESERVE_TABLE, "attendees", PERCENT + email + PERCENT),
                Restrictions.eq(RESERVE_TABLE, "email", email)));

            final ParsedRestrictionDef restriction = new ParsedRestrictionDef();
            if (endDate == null) {
                restriction.addClause(RESERVE_TABLE, Constants.DATE_START_FIELD_NAME, startDate,
                        Operation.EQUALS);
            } else {
                restriction.addClause(RESERVE_TABLE, Constants.DATE_START_FIELD_NAME, startDate,
                        Operation.GTE);
                restriction.addClause(RESERVE_TABLE, Constants.DATE_END_FIELD_NAME, endDate,
                        Operation.LTE);
            }

            restriction.addClause(
                    RESERVE_TABLE,
                    Constants.STATUS,
                    Arrays.asList(new String[] { Constants.STATUS_AWAITING_APP,
                            Constants.STATUS_CONFIRMED }), Operation.IN);

            final List<RoomReservation> reservations =
                    this.roomReservationDataSource.find(restriction);

            for (final RoomReservation reservation : reservations) {

                final ICalendarEvent calendarEvent = new CalendarEvent();
                // take the reservation id as reference id
                calendarEvent.setEventId(Integer.toString(reservation.getReserveId()));
                // confidential
                calendarEvent.setSubject(reservation.getReservationName());
                calendarEvent.setStartDate(reservation.getStartDate());
                calendarEvent.setEndDate(reservation.getEndDate());
                calendarEvent.setStartTime(reservation.getStartTime());
                calendarEvent.setEndTime(reservation.getEndTime());

                final List<RoomAllocation> roomAllocations =
                        this.roomAllocationDataSource.find(reservation);
                if (roomAllocations.isEmpty()) {
                    calendarEvent.setLocation("");
                } else {
                    calendarEvent.setLocation(roomAllocations.get(0).getLocation());
                }

                calendarEvent.setSubject(reservation.getReservationName());

                if (Constants.TYPE_RECURRING.equalsIgnoreCase(reservation.getReservationType())) {
                    calendarEvent.setRecurrent(true);
                } else {
                    calendarEvent.setRecurrent(false);
                }

                events.add(calendarEvent);
            }
        }

        return events;
    }

    /** {@inheritDoc} */
    public String createAppointment(final IReservation reservation) throws ExceptionBase {
        // send emails to attendees, only recurring if recurrence property is set
        sendEmailInvitations(reservation, null, TYPE_NEW, reservation.getRecurrence() != null, null);
        // return empty string for appointment identifier
        return "";
    }

    /** {@inheritDoc} */
    public void updateAppointment(final IReservation reservation) throws ExceptionBase {
        // send emails to attendees.
        sendEmailInvitations(reservation, null, TYPE_UPDATE, true, null);
    }

    /** {@inheritDoc} */
    public void cancelAppointment(final IReservation reservation, final String message)
            throws ExceptionBase {
        // send emails to attendees
        sendEmailInvitations(reservation, null, TYPE_CANCEL, true, message);
    }

    /** {@inheritDoc} */
    public void cancelAppointmentOccurrence(final IReservation reservation, final String message)
            throws ExceptionBase {
        // send emails to attendees
        sendEmailInvitations(reservation, null, TYPE_CANCEL, false, message);
    }

    /** {@inheritDoc} */
    public void updateAppointmentOccurrence(final IReservation reservation, final IReservation originalReservation) 
            throws ExceptionBase {
        // send emails to attendees
        sendEmailInvitations(reservation, originalReservation, TYPE_UPDATE, false, null);
    }
 
    /**
     * Set the employee service.
     * 
     * @param employeeService the new employee service
     */
    public void setEmployeeService(final IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }
 
    /**
     * Set the visitor data source.
     * 
     * @param visitorDataSource the new visitor data source
     */
    public void setVisitorDataSource(final IVisitorDataSource visitorDataSource) {
        this.visitorDataSource = visitorDataSource;
    }

    /**
     * Send the e-mail notifications.
     *
     * @param reservation the reservation to send an invite for
     * @param originalReservation the original reservation
     * @param invitationType the type of invite to send
     * @param allRecurrences true to send for all occurrences, false for only the given occurrence
     * @param message the message to include in the notification
     */
    private void sendEmailInvitations(final IReservation reservation, final IReservation originalReservation, 
            final String invitationType, final boolean allRecurrences, final String message) {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();

        if (EmailNotificationHelper.notificationsEnabled()) {
            // the reservation id
            context.addResponseParameter(Constants.RES_ID, reservation.getReserveId().toString());
            // set the invitation type
            context.addResponseParameter("invitation_type", invitationType);

            if (invitationType.equals(TYPE_CANCEL) && !allRecurrences) {
                // when canceling one occurrence, specify which date to cancel
                context.addResponseParameter("date_cancel",
                        WebCentralCalendarServiceHelper.getDateFormatted(reservation.getStartDate()));
            }

            // email_invitations: set empty string if null
            String attendees = reservation.getAttendees();
            if (attendees == null) {
                attendees = "";
            }
            context.addResponseParameter("email_invitations", attendees);
            // require reply always
            context.addResponseParameter("require_reply", true);
            // canceling message
            
            if (StringUtil.notNullOrEmpty(message)) {
                context.addResponseParameter("cancel_message", message); 
            } 

            // when update
            WebCentralCalendarServiceHelper.addResponseParametersUpdate(reservation, originalReservation, context); 
 
            addReservationResponseParameters(reservation, allRecurrences, context);
            // do we allow exceptions for cancelled reservations?
            context.addResponseParameter("RoomConflicts", "[]");

            // Check whether the result message is already set and remember it.
            final String resultMessage = WebCentralCalendarServiceHelper.checkResultMessage(context);
           
            // when editing different occurrences of a recurrent reservation, send as separate update invitations
 
            // call using the ReservationsCommonHandler
            // all parameters are in context
            final ReservationsCommonHandler commonHandler = new ReservationsCommonHandler();        
            commonHandler.sendEmailInvitations(context);        

            WebCentralCalendarServiceHelper.setResultMessage(context, resultMessage);
        }
    }

    /**
     * Set the Reservation JSON object, reservation id and parent reservation id in the context for
     * sending the invitations.
     * 
     * @param reservation the reservation domain object
     * @param allRecurrences whether to create invitations for all occurrences
     * @param context the event handler context
     */
    private void addReservationResponseParameters(final IReservation reservation,
            final boolean allRecurrences, final EventHandlerContext context) {
        final JSONObject json = new JSONObject();
        Date endDate = null;
        // KB 3040087: for a recurring invitation, the endDate in the Reservation json object
        // must indicate the end date of the series.
        if (allRecurrences && reservation.getRecurrence() != null) {
            endDate = reservation.getRecurrence().getEndDate();
        } else {
            endDate = reservation.getEndDate();
        }
        json.put(Constants.DATE_END_FIELD_NAME,
            WebCentralCalendarServiceHelper.getDateFormatted(endDate));

        json.put("time_end", WebCentralCalendarServiceHelper.getTimeFormatted(reservation.getEndTime()));

        //bv when editing or canceling the full list of reservations
        if (allRecurrences && StringUtil.notNullOrEmpty(reservation.getRecurringRule())) {
              WebCentralCalendarSettings.prepareRecurringInvitations(reservation, context, json);
        } else {
            //bv when editing or canceling an occurrence, point to the correct date 
            WebCentralCalendarSettings.prepareSingleInvitation(reservation, context, json);
        }

        // add reservation in context
        context.addResponseParameter("Reservation", json.toString());
    }
    
    /**
     * {@inheritDoc}
     */
    public List<AttendeeResponseStatus> getAttendeesResponseStatus(final IReservation reservation)
            throws ExceptionBase {
        final List<AttendeeResponseStatus> responses = new ArrayList<AttendeeResponseStatus>();
        if (reservation != null) { 
            final String attendeesValue = reservation.getAttendees();
            if (StringUtil.notNullOrEmpty(attendeesValue)) {
                final String[] emails = attendeesValue.split(";");
                for (final String email : emails) {
                    final AttendeeResponseStatus responseStatus = toResponseStatus(email);
                    responses.add(responseStatus);
                }
            }

        }
        return responses;
    }

    /**
     * Get the response status object for the attendee with the given email.
     * 
     * @param email the email address
     * @return the attendee response status
     */
    private AttendeeResponseStatus toResponseStatus(final String email) {
        final AttendeeResponseStatus responseStatus = new AttendeeResponseStatus();
        responseStatus.setEmail(email);
        // the response status is always unknown
        responseStatus.setResponseStatus(ResponseStatus.Unknown);

        // lookup name in employee and visitors tables
        final Employee employee = this.employeeService.findEmployee(email);
        if (employee == null) {
            final Visitor visitor = this.visitorDataSource.findByEmail(email);
            if (visitor != null) {
                responseStatus.setName(visitor.getFirstName() + SPACE + visitor.getLastName());
            }
        } else {
            if (StringUtil.isNullOrEmpty(employee.getFirstName())
                    && StringUtil.isNullOrEmpty(employee.getLastName())) {
                responseStatus.setName(employee.getId());
            } else {
                responseStatus.setName(employee.getFirstName() + SPACE + employee.getLastName());
            }

        }
        return responseStatus;
    }
    
}
