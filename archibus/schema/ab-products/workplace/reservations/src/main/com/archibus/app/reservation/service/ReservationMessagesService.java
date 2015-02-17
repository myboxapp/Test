package com.archibus.app.reservation.service;

import java.util.*;

import com.archibus.app.common.notification.dao.INotificationMessageDao;
import com.archibus.app.common.notification.domain.Notification;
import com.archibus.app.common.notification.message.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.app.reservation.util.ReservationsContextHelper;
import com.archibus.context.*;

/**
 * Service class that can build messages related to reservations.
 * 
 * Managed by Spring.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class ReservationMessagesService {
    
    /** Activity ID for reservations application. */
    private static final String ACTIVITY_ID = "AbWorkplaceReservations";
    
    /** Referenced_by value for the messages used by the Exchange listener service. */
    private static final String REFERENCED_BY_LISTENER = "LISTENER_WFR";
    
    /** Referenced_by value for the messages used by the Exchange calendar service. */
    private static final String REFERENCED_BY_CALENDAR = "CALENDAR_SERVICE";
    
    /** Subject for a cancellation success message. */
    private static final String EXCHANGE_CANCEL_SUCCESS_SUBJECT =
            "EXCHANGE_CANCEL_SUCCESS_NOTIFY_SUBJECT";
    
    /** Body for a cancellation success message. */
    private static final String EXCHANGE_CANCEL_SUCCESS_BODY =
            "EXCHANGE_CANCEL_SUCCESS_NOTIFY_BODY";
    
    /** Subject for a cancellation failure message. */
    private static final String EXCHANGE_CANCEL_FAILED_SUBJECT =
            "EXCHANGE_CANCEL_FAILED_NOTIFY_SUBJECT";
    
    /** Body for a cancellation failure message. */
    private static final String EXCHANGE_CANCEL_FAILED_BODY = "EXCHANGE_CANCEL_FAILED_NOTIFY_BODY";
    
    /** Subject for an update failure message. */
    private static final String EXCHANGE_UPDATE_CONFLICT_SUBJECT =
            "EXCHANGE_UPDATE_CONFLICT_NOTIFY_SUBJECT";
    
    /** Body for an update failure message. */
    private static final String EXCHANGE_UPDATE_CONFLICT_BODY1 =
            "EXCHANGE_UPDATE_CONFLICT_NOTIFY_BODY1";
    
    /** Body for an update failure message. */
    private static final String EXCHANGE_UPDATE_CONFLICT_BODY2 =
            "EXCHANGE_UPDATE_CONFLICT_NOTIFY_BODY2";
    
    /** Body for an update failure message. */
    private static final String EXCHANGE_UPDATE_CONFLICT_BODY3 =
            "EXCHANGE_UPDATE_CONFLICT_NOTIFY_BODY3";
    
    /** Subject for a cancellation message from WebCentral. */
    private static final String CANCEL_NOTIFY_SUBJECT = "CANCEL_NOTIFY_SUBJECT";
    
    /** Body for a cancellation message from WebCentral. */
    private static final String CANCEL_NOTIFY_BODY = "CANCEL_NOTIFY_BODY";

    /** The notification messages data source. */
    private INotificationMessageDao notificationMessageDao;
    
    /**
     * Create a cancellation confirmation message.
     * 
     * @param reservation the room reservation to create the message for
     * @return the message
     */
    public ReservationMessage createCancelledConfirmation(final RoomReservation reservation) {
        ReservationsContextHelper.checkProjectContext();
        final Notification notification = new Notification();
        notification.setSubject(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_CANCEL_SUCCESS_SUBJECT));
        notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_CANCEL_SUCCESS_BODY));
        
        final NotificationDataModel dataModel =
                createDataModelWithRoom(reservation, reservation.getRoomAllocations().get(0)
                    .getRoomArrangement());
        
        return formatMessage(reservation.getEmail(), notification, dataModel);
    }
    
    /**
     * Create a cancellation failure message.
     * 
     * @param reservation the room reservation to create the message for
     * @return the message
     */
    public ReservationMessage createCancelledFailure(final RoomReservation reservation) {
        ReservationsContextHelper.checkProjectContext();
        final Notification notification = new Notification();
        notification.setSubject(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_CANCEL_FAILED_SUBJECT));
        notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_CANCEL_FAILED_BODY));
        
        final NotificationDataModel dataModel =
                createDataModelWithRoom(reservation, reservation.getRoomAllocations().get(0)
                    .getRoomArrangement());
        
        return formatMessage(reservation.getEmail(), notification, dataModel);
    }
    
    /**
     * Create an update failure message.
     * 
     * @param reservation the room reservation to create the message for
     * @param conflictingReservable the reservable in the reservation that causes the failure
     * @param detailMessage indicates the reason for the failure
     * @return the message
     */
    public ReservationMessage createUpdateFailure(final RoomReservation reservation,
            final IReservable conflictingReservable, final String detailMessage) {
        ReservationsContextHelper.checkProjectContext();
        final Notification notification = new Notification();
        notification.setSubject(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_UPDATE_CONFLICT_SUBJECT));
        notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_LISTENER, EXCHANGE_UPDATE_CONFLICT_BODY1));
        
        NotificationDataModel dataModel = null;
        if (conflictingReservable instanceof RoomArrangement) {
            dataModel =
                    createDataModelWithRoom(reservation, (RoomArrangement) conflictingReservable);
        } else if (conflictingReservable instanceof Resource) {
            dataModel = createDataModelWithResource(reservation, (Resource) conflictingReservable);
        } else {
            dataModel = createDataModel(reservation);
        }
        
        // First create a message containing the first part of the body.
        final ReservationMessage message =
                formatMessage(reservation.getEmail(), notification, dataModel);
        
        // Add the second part of the body depending on what is causing the conflict.
        if (conflictingReservable instanceof RoomArrangement) {
            notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
                REFERENCED_BY_LISTENER, EXCHANGE_UPDATE_CONFLICT_BODY2));
            final ReservationMessage body2 =
                    formatMessage(reservation.getEmail(), notification, dataModel);
            message.setBody(message.getBody() + body2.getBody());
        } else if (conflictingReservable instanceof Resource) {
            notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
                REFERENCED_BY_LISTENER, EXCHANGE_UPDATE_CONFLICT_BODY3));
            final ReservationMessage body3 =
                    formatMessage(reservation.getEmail(), notification, dataModel);
            message.setBody(message.getBody() + body3.getBody());
        }
        
        return message;
    }
    
    /**
     * Create a notification message to inform the meeting organizer that his reservation was
     * cancelled.
     * 
     * @param reservation the cancelled reservation
     * @param comments a message from the user cancelling the reservation
     * @return the message to send to the organizer
     */
    public ReservationMessage createCancelledNotification(final RoomReservation reservation,
            final String comments) {
        ReservationsContextHelper.checkProjectContext();
        final Notification notification = new Notification();
        notification.setSubject(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_CALENDAR, CANCEL_NOTIFY_SUBJECT));
        notification.setBody(this.notificationMessageDao.getByPrimaryKey(ACTIVITY_ID,
            REFERENCED_BY_CALENDAR, CANCEL_NOTIFY_BODY));
        
        final NotificationDataModel dataModel =
                createDataModelWithRoom(reservation, reservation.getRoomAllocations().get(0)
                    .getRoomArrangement());
        
        // Include details of the current user in the data model.
        addUserDetailsToModel(dataModel, comments);
        
        return formatMessage(reservation.getEmail(), notification, dataModel);
    }

    /**
     * Set the notification messages data source.
     * 
     * @param notificationMessageDao the notification messages data source to set
     */
    public void setNotificationMessageDao(final INotificationMessageDao notificationMessageDao) {
        this.notificationMessageDao = notificationMessageDao;
    }
    
    /**
     * Format the notification message.
     * 
     * @param email destination email address
     * @param notification the notification to format
     * @param dataModel the data model to use for the formatting
     * @return the formatted message
     */
    private ReservationMessage formatMessage(final String email, final Notification notification,
            final NotificationDataModel dataModel) {
        final NotificationMessageFormatter formatter = new NotificationMessageFormatter();
        formatter.format(email, notification, dataModel);
        return new ReservationMessage(formatter.getFormattedSubject(), formatter.getFormattedBody());
    }
    
    /**
     * Create a data model for the given data.
     * 
     * @param reservation the reservation to include in the data model
     * @return the data model
     */
    private NotificationDataModel createDataModel(final RoomReservation reservation) {
        final NotificationDataModel dataModel = new NotificationDataModel();
        dataModel.setDataModel(new HashMap<String, Object>());
        
        final Map<String, Object> reserveDataModel = new HashMap<String, Object>();
        reserveDataModel.put("res_id", reservation.getReserveId());
        reserveDataModel.put("date_start", reservation.getStartDate());
        reserveDataModel.put("time_start", reservation.getStartTime());
        reserveDataModel.put("time_end", reservation.getEndTime());
        reserveDataModel.put("reservation_name", reservation.getReservationName());
        dataModel.getDataModel().put("reserve", reserveDataModel);
        
        return dataModel;
    }
    
    /**
     * Create a data model for the given data.
     * 
     * @param reservation the reservation to include in the data model
     * @param roomArrangement the room arrangement to include in the data model
     * @return the data model
     */
    private NotificationDataModel createDataModelWithRoom(final RoomReservation reservation,
            final RoomArrangement roomArrangement) {
        final NotificationDataModel dataModel = createDataModel(reservation);
        
        final Map<String, Object> rmArrangeDataModel = new HashMap<String, Object>();
        rmArrangeDataModel.put("bl_id", roomArrangement.getBlId());
        rmArrangeDataModel.put("fl_id", roomArrangement.getFlId());
        rmArrangeDataModel.put("rm_id", roomArrangement.getRmId());
        rmArrangeDataModel.put("config_id", roomArrangement.getConfigId());
        rmArrangeDataModel.put("arrange_type_id", roomArrangement.getArrangeTypeId());
        dataModel.getDataModel().put("rm_arrange", rmArrangeDataModel);
        
        return dataModel;
    }
    
    /**
     * Create a data model for the given data.
     * 
     * @param reservation the reservation to include in the data model
     * @param resource the resource to include in the data model
     * @return the data model
     */
    private NotificationDataModel createDataModelWithResource(final RoomReservation reservation,
            final Resource resource) {
        final NotificationDataModel dataModel = createDataModel(reservation);
        
        final Map<String, Object> resourceDataModel = new HashMap<String, Object>();
        resourceDataModel.put("resource_id", resource.getResourceId());
        resourceDataModel.put("resource_name", resource.getResourceName());
        resourceDataModel.put("resource_std", resource.getResourceStandard());
        dataModel.getDataModel().put("resources", resourceDataModel);
        
        // Also include the count of the resource allocation.
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            if (allocation.getResourceId().equals(resource.getResourceId())) {
                final Map<String, Object> reserveRsDataModel = new HashMap<String, Object>();
                reserveRsDataModel.put("quantity", allocation.getQuantity());
                dataModel.getDataModel().put("reserve_rs", reserveRsDataModel);
                break;
            }
        }
        
        return dataModel;
    }
    
    /**
     * Add user details of the current user to the data model. Also include the user's comments.
     * 
     * @param dataModel the datamodel to add user details to.
     * @param comments message from the user to include
     */
    private void addUserDetailsToModel(final NotificationDataModel dataModel, final String comments) {
        final User user = ContextStore.get().getUser();
        final Map<String, Object> userDataModel = new HashMap<String, Object>();
        userDataModel.put("name", user.getName());
        userDataModel.put("comments", comments);
        
        dataModel.getDataModel().put("user", userDataModel);
    }
}
