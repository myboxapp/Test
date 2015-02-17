package com.archibus.app.reservation.util;

import java.text.MessageFormat;

import org.springframework.security.userdetails.UsernameNotFoundException;

import com.archibus.app.reservation.dao.datasource.Constants;
import com.archibus.context.*;
import com.archibus.datasource.*;
import com.archibus.datasource.data.DataRecord;
import com.archibus.datasource.restriction.Restrictions;
import com.archibus.eventhandler.EventHandlerBase;
import com.archibus.jobmanager.EventHandlerContext;
import com.archibus.security.UserAccount;
import com.archibus.utility.StringUtil;

/**
 * Utility class. Provides methods to set up the Web Central context for processing Exchange events.
 * <p>
 * Used by the Exchange Listener to set the correct user in the context.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class ReservationsContextHelper {
    
    /** Message parameter name in the event handler context. */
    public static final String RESULT_MESSAGE_PARAMETER = "message";

    /** User name field name. */
    private static final String USER_NAME = "user_name";
    
    /** Table containing user account info. */
    private static final String TABLE_AFM_USERS = "afm_users";
    
    /** Project parameter name. */
    private static final String PROJECT_PARAMETER = "project";
    
    /** Default OK message to write in the message parameter. */
    private static final String RESULT_MESSAGE_OK = "OK";

    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private ReservationsContextHelper() {
    }
    
    /**
     * Set the user account based on the user's email address.
     * 
     * @param email the email address
     */
    public static void setUserFromEmail(final String email) {
        final DataSource userDs =
                DataSourceFactory.createDataSourceForFields(TABLE_AFM_USERS, new String[] {
                        USER_NAME, Constants.EMAIL_FIELD_NAME });
        final Context context = ContextStore.get();
        
        userDs.addRestriction(Restrictions.eq(TABLE_AFM_USERS, Constants.EMAIL_FIELD_NAME, email));
        final DataRecord record = userDs.getRecord();
        
        if (record == null) {
            throw new UsernameNotFoundException("No user found with email {0}", email);
        } else {
            final String username = record.getString(TABLE_AFM_USERS + "." + USER_NAME);
            final UserAccount.Immutable userAccount =
                    context.getProject().loadUserAccount(username, context.getSession().getId(),
                        false);
            context.setUser(userAccount.getUser());
        }
    }
    
    /**
     * Check project context.
     */
    public static void checkProjectContext() {
        final EventHandlerContext eventHandlerContext = ContextStore.get().getEventHandlerContext();
        if (!eventHandlerContext.parameterExistsNotEmpty(PROJECT_PARAMETER)) {
            ContextStore.get().getEventHandlerContext()
                .addInputParameter(PROJECT_PARAMETER, ContextStore.get().getProject());
        }
    }
    
    /**
     * Check whether the EventHandlerContext has a message. If it doesn't, then add the default OK
     * message.
     */
    public static void ensureResultMessageIsSet() {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        if (!context.parameterExists(RESULT_MESSAGE_PARAMETER)
                || StringUtil.isNullOrEmpty(context.getParameter(RESULT_MESSAGE_PARAMETER))) {
            context.addResponseParameter(RESULT_MESSAGE_PARAMETER, RESULT_MESSAGE_OK);
        }
    }
    
    /**
     * Append an error message in the event handler context.
     * 
     * @param error the error message to append
     */
    public static void appendResultError(final String error) {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        String message = context.getString(RESULT_MESSAGE_PARAMETER, "");
        // Don't append if the same error was already in the context.
        if (!message.equals(error)) {
            if (StringUtil.notNullOrEmpty(message)) {
                message += "\n";
            }
            message += error;
        }
        
        context.addResponseParameter(RESULT_MESSAGE_PARAMETER, message);
    }
    
    /**
     * Localize the given string that was marked translatable in the given class.
     * 
     * @param message the string to localize
     * @param clazz the class where the string is defined and marked translatable
     * @return the localized string
     */
    public static String localizeString(final String message, final Class<?> clazz) {
        return EventHandlerBase.localizeString(ContextStore.get().getEventHandlerContext(),
            message, clazz.getName());
    }
    
    /**
     * Localize the given string that was marked translatable in the given class.
     * 
     * @param message the string to localize
     * @param clazz the class where the message was defined and marked translatable
     * @param args additional arguments used for formatting the localized message
     * @return the localized string
     */
    public static String localizeString(final String message, final Class<?> clazz,
            final Object... args) {
        return MessageFormat.format(EventHandlerBase.localizeString(ContextStore.get()
            .getEventHandlerContext(), message, clazz.getName()), args);
    }

}
