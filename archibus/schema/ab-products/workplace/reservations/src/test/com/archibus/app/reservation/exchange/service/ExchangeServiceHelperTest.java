package com.archibus.app.reservation.exchange.service;

import java.net.URISyntaxException;
import java.util.EnumSet;

import junit.framework.Assert;
import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.ConfiguredDataSourceTestBase;

/**
 * Test class for Exchange Service helper.
 * 
 * @author Yorik Gerlo
 */
public class ExchangeServiceHelperTest extends ConfiguredDataSourceTestBase {
    
    /** An email address that resides on the connected Exchange. */
    private static final String EXCHANGE_EMAIL = "yorik.gerlo@procos1.onmicrosoft.com";
    
    /** An email address that doesn't reside on the connected Exchange. */
    private static final String OTHER_EMAIL = "ai@tgd.com";
    
    /** The Service Helper under test. */
    private ExchangeServiceHelper serviceHelper;
    
    /**
     * Test method for
     * {@link com.archibus.app.reservation.exchange.service.ExchangeServiceHelper#getService(java.lang.String)}
     * .
     * @throws URISyntaxException when the Exchange URL is invalid
     */
    public void testGetServiceString() throws URISyntaxException {
        ExchangeService service = this.serviceHelper.initializeService(EXCHANGE_EMAIL);
        service.setTraceFlags(EnumSet.of(TraceFlags.EwsRequest, TraceFlags.EwsRequestHttpHeaders,
            TraceFlags.EwsResponse, TraceFlags.EwsResponseHttpHeaders));
        service.setTraceEnabled(true);
        try {
            Folder.bind(service, WellKnownFolderName.Calendar, PropertySet.IdOnly);
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
        
        service = this.serviceHelper.getService(OTHER_EMAIL);
        service.setTraceFlags(EnumSet.of(TraceFlags.EwsRequest, TraceFlags.EwsRequestHttpHeaders,
            TraceFlags.EwsResponse, TraceFlags.EwsResponseHttpHeaders));
        service.setTraceEnabled(true);
        try {
            Folder.bind(service, WellKnownFolderName.Calendar, PropertySet.IdOnly);
            Assert.fail("Should not be able to bind to a folder of a user that doesn't exist.");
        } catch (final ServiceResponseException exception) {
            Assert.assertEquals(ServiceError.ErrorNonExistentMailbox, exception.getErrorCode());
            // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API method
            // throws a checked Exception.
        } catch (final Exception exception) {
            // CHECKSTYLE:ON
            Assert.fail(exception.toString());
        }
    }
    
    /**
     * Set the service helper.
     * 
     * @param serviceHelper the new service helper
     */
    public void setServiceHelper(final ExchangeServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }

}
