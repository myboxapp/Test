package com.archibus.app.reservation.util;

import junit.framework.Assert;

import com.archibus.context.ContextStore;
import com.archibus.datasource.DataSourceTestBase;
import com.archibus.jobmanager.EventHandlerContext;

/**
 * Test for ReservationsContextHelper.
 */
public class ReservationsContextHelperTest extends DataSourceTestBase {
    
    /** The separator between error messages. */
    private static final String ERROR_SEPARATOR = "\n";

    /** String value used for testing. */
    private static final String TEST1 = "test 1";
    
    /** String value used for testing. */
    private static final String TEST2 = "test 2";
    
    /**
     * Test appending an error message to the result message.
     */
    public void testAppendResultError() {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();

        ReservationsContextHelper.appendResultError(TEST1);
        
        Assert.assertEquals(TEST1,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
        
        ReservationsContextHelper.appendResultError(TEST1);
        
        Assert.assertEquals(TEST1,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
        
        ReservationsContextHelper.appendResultError(TEST2);
        
        Assert.assertEquals(TEST1 + ERROR_SEPARATOR + TEST2,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
        
        ReservationsContextHelper.appendResultError(TEST1);
        
        Assert.assertEquals(TEST1 + ERROR_SEPARATOR + TEST2 + ERROR_SEPARATOR + TEST1,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
    }
    
    /**
     * Test ensuring the result message is set.
     */
    public void testEnsureResultMessageIsSet() {
        final EventHandlerContext context = ContextStore.get().getEventHandlerContext();
        
        ReservationsContextHelper.ensureResultMessageIsSet();
        
        Assert.assertEquals("OK",
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
        
        context.removeResponseParameter(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER);
        
        Assert.assertEquals(TEST2,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER, TEST2));

        ReservationsContextHelper.appendResultError(TEST1);
        ReservationsContextHelper.ensureResultMessageIsSet();
        
        Assert.assertEquals(TEST1,
            context.getString(ReservationsContextHelper.RESULT_MESSAGE_PARAMETER));
    }

}
