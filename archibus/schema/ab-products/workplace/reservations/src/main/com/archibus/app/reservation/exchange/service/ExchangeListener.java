package com.archibus.app.reservation.exchange.service;

import microsoft.exchange.webservices.data.*;

import org.apache.log4j.Logger;

import com.archibus.app.reservation.domain.CalendarException;
import com.archibus.utility.ExceptionBase;

/**
 * Exchange Listener service that receives events from Exchange when enabled.
 * 
 * Managed via Spring.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public class ExchangeListener {
    
    /** This helper provides the connection with Exchange. */
    private ExchangeServiceHelper serviceHelper;
    
    /** The item handler that process items received from Exchange. */
    private ItemHandler itemHandler;
    
    /** Indicates whether the listener should be enabled after initialization. */
    private boolean enableListener;
    
    /**
     * The signalling object used to wake up the WFR service when an event is received from
     * Exchange.
     */
    private final Object signal = new Object();
    
    /**
     * This value is set to true when an event is received from Exchange. The WFR service can check
     * this value to verify whether it missed a signal before waiting for the next signal.
     */
    private boolean wasSignalled;
    
    /** Indicates whether the listener should stop. */
    private boolean stopRequested;
    
    /** The logger. */
    private final Logger logger = Logger.getLogger(this.getClass());
    
    /**
     * First handle all pending events, then start the Exchange streaming notification listener.
     */
    public void run() {
        if (this.enableListener && !this.stopRequested) {
            
            logger.info("Starting Exchange listener");
            
            final ExchangeService exchangeService =
                    this.serviceHelper.initializeService(this.serviceHelper.getResourceAccount());
            
            // First handle all messages currently in the inbox.
            processInbox(exchangeService);
            
            // When the inbox is empty, start a streaming subscription.
            new StreamingNotificationHandler(this, exchangeService);
            
            while (!this.stopRequested) {
                // Read the inbox again to handle intermediate arrivals.
                try {
                    processInbox(exchangeService);
                } catch (ExceptionBase exception) {
                    this.logger.warn("Processing inbox items failed. Waiting for next signal.",
                        exception);
                }
                
                // Now wait for a signal from the notification handler before checking again.
                waitForSignal();
            }
        }
    }
    
    /**
     * Start a new subscription.
     */
    void reStartListener() {
        if (this.enableListener && !this.stopRequested) {
            final ExchangeService exchangeService =
                    this.serviceHelper.initializeService(this.serviceHelper.getResourceAccount());
            new StreamingNotificationHandler(this, exchangeService);
            signalEventReceived();
        }
    }
    
    /**
     * Request to stop the listener.
     */
    public void requestStop() {
        synchronized (this.signal) {
            this.stopRequested = true;
            this.signal.notifyAll();
        }
    }
    
    /**
     * Signal that an event was received from Exchange.
     */
    void signalEventReceived() {
        synchronized (this.signal) {
            this.wasSignalled = true;
            this.signal.notify();
        }
    }
    
    /**
     * Wait until a signal is received or until the current thread is interrupted.
     */
    private void waitForSignal() {
        try {
            synchronized (this.signal) {
                while (!(this.wasSignalled || this.stopRequested)) {
                    this.signal.wait();
                }
                this.wasSignalled = false;
            }
        } catch (InterruptedException exception) {
            // We can safely ignore this exception and pretend we were woken up
            // as usual. The interruption can also occur because the Job is
            // being terminated. In that case no attempt will be made to process
            // any events.
            this.logger.debug("waitForSignal was interrupted", exception);
        }
    }
    
    /**
     * Set whether the listener should be enabled.
     * 
     * @param enableListener the enableListener to set
     */
    public void setEnableListener(final boolean enableListener) {
        this.enableListener = enableListener;
    }
    
    /**
     * Set the new item handler.
     * 
     * @param itemHandler the new item handler
     */
    public void setItemHandler(final ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }
    
    /**
     * Set the new Exchange service helper.
     * 
     * @param serviceHelper the serviceHelper to set
     */
    public void setServiceHelper(final ExchangeServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }
    
    /**
     * Process all items in the inbox. Those items should be meeting invitations or cancellations;
     * other types of items are ignored.
     * 
     * @param exchangeService the service connected to Exchange
     */
    void processInbox(final ExchangeService exchangeService) {
        Integer offset = 0;
        try {
            do {
                final ItemView itemView = new ItemView(512, offset);
                itemView.getOrderBy().add(EmailMessageSchema.DateTimeReceived,
                    SortDirection.Ascending);
                final FindItemsResults<Item> results =
                        exchangeService.findItems(WellKnownFolderName.Inbox, itemView);
                logger.debug("Processing " + results.getTotalCount() + " items. Next offset is "
                        + results.getNextPageOffset());
                for (final Item item : results.getItems()) {
                    // If an error occurs handling an individual item, then the Job should ignore
                    // this item and continue.
                    try {
                        // The itemHandler is wrapped in a proxy for transaction management via
                        // Spring.
                        this.itemHandler.handleItem(item);
                    } catch (ExceptionBase exception) {
                        // Rollback occurs in the interceptors of ItemHanderlImpl.
                        this.logger.warn("Error handling Exchange Item.", exception);
                    }
                }
                offset = results.getNextPageOffset();
            } while (offset != null);
        } catch (final ExceptionBase exception) {
            throw exception;
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
            // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            throw new CalendarException("Error processing inbox items.", exception,
                ExchangeListener.class);
        }
    }
    
}
