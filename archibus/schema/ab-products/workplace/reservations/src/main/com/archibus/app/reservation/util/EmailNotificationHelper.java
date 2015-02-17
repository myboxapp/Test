package com.archibus.app.reservation.util;

import java.util.*;

import com.archibus.context.ContextStore;
import com.archibus.eventhandler.reservations.ReservationsCommonHandler;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.utility.StringUtil;

/**
 * Utility class. Provides methods to send email notifications.
 * <p>
 * Used by Reservation Application.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class EmailNotificationHelper {
    
    /** Parent reservation id field name. */
    private static final String RES_PARENT = "res_parent";
    
    /** Event handler result parameter name. */
    private static final String MESSAGE = "message";
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private EmailNotificationHelper() {
    }
    
    /**
     * Send notifications for the single reservation with the given identifier.
     * 
     * @param reservationId reservation identifier
     */
    public static void sendNotifications(final Integer reservationId) {
        sendNotifications(reservationId, null, null);
    }
    
    /**
     * Send notifications to the requested by and requested for of the reservation.
     * 
     * @param reservationId the reservation id to notify for
     * @param parentId the parent reservation id to notify for a recurring series
     * @param cancelMessage the message
     */
    public static void sendNotifications(final Integer reservationId, final Integer parentId,
            final String cancelMessage) {
        // Use the ReservationsCommonHandler to send the notifications.
        
        if (notificationsEnabled()) {
            final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
            
            if (StringUtil.notNullOrEmpty(cancelMessage)) {
                context.addResponseParameter("cancel_message", cancelMessage);
            }
            
            context.addResponseParameter("res_id", Integer.toString(reservationId));
            if (parentId == null) {
                context.removeResponseParameter(RES_PARENT);
            } else {
                context.addResponseParameter(RES_PARENT, Integer.toString(parentId));
            }
            final ReservationsCommonHandler handler = new ReservationsCommonHandler();
            
            final List<String> errorMessages = new ArrayList<String>();
            
            if (context.parameterExists(MESSAGE)) {
                errorMessages.add(String.valueOf(context.getParameter(MESSAGE)));
            }
            
            handler.notifyRequestedBy(context);
            if (context.parameterExists(MESSAGE)) {
                errorMessages.add(String.valueOf(context.getParameter(MESSAGE)));
            }
            
            handler.notifyRequestedFor(context);
            if (context.parameterExists(MESSAGE)) {
                errorMessages.add(String.valueOf(context.getParameter(MESSAGE)));
            }
            
            setFullErrorMessage(context, errorMessages);
        }
    }
    
    /**
     * Set the full error message in the context, concatenating the errors in the list but removing
     * sequential duplicates.
     * 
     * @param context context to put the error message
     * @param errorMessages the list of error messages to concatenate
     */
    private static void setFullErrorMessage(final EventHandlerContext context,
            final List<String> errorMessages) {
        String fullErrorMessage = "";
        for (int i = errorMessages.size() - 1; i > 0; --i) {
            final String errorMessage = errorMessages.get(i);
            // Skip duplicate and empty error messages.
            if (StringUtil.notNullOrEmpty(errorMessage) && !errorMessage.equals(errorMessages.get(i - 1))) {
                fullErrorMessage = '\n' + errorMessage + fullErrorMessage;
            }
        }
        
        if (!errorMessages.isEmpty()) {
            fullErrorMessage = errorMessages.get(0) + fullErrorMessage;
        }
        
        if (StringUtil.notNullOrEmpty(fullErrorMessage)) {
            context.addResponseParameter(MESSAGE, fullErrorMessage);
        }
    }
    
    /**
     * Check whether sending email notifications is enabled for the Reservations Application.
     * 
     * @return true if enabled, false otherwise
     */
    public static boolean notificationsEnabled() {
        final String sendEmailNotifications =
                com.archibus.service.Configuration.getActivityParameterString(
                    "AbWorkplaceReservations", "SendEmailNotifications");
        
        return sendEmailNotifications != null && "YES".equals(sendEmailNotifications.toUpperCase());
    }
    
}
