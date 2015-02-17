package com.archibus.eventhandler.reservations;

import java.util.*;

import junit.framework.TestCase;

import com.archibus.fixture.EventHandlerFixture;

public class TestReservationsRoomHandler extends TestCase {

    /**
     * Helper object providing test-related resource and methods.
     */
    private EventHandlerFixture fixture = null;

    /**
     * Workflow rule IDs.
     */
    static final String ACTIVITY_ID = "AbWorkplaceReservations";

    static final String EVENT_HANDLER_CLASS = "com.archibus.eventhandler.reservations.ReservationsRoomHandler";

    protected void setUp() throws Exception {
        this.fixture = new EventHandlerFixture(this, "ab-ex-echo.axvw");
        this.fixture.setUp();
    }

    protected void tearDown() throws Exception {
        this.fixture.tearDown();
    }

    // -------------------------------------------------------------------------------------------------
    // Test testCreateWorkRequest wfr
    //
    public void testCreateWorkRequest() {
        // Begin database transaction
        Object transactionContext = fixture.beginTransaction();

        try {
            // Add test input values
            Map inputs = new HashMap();
            // Prepare response map
            Map response = new HashMap();

            // Either parent_id or res_id will be empty
            inputs.put("res_id", "86"); // It must be obligatory and it must be comprehensive with
                                        // DB data
            inputs.put("res_parent", "0"); // It must be obligatory and it must be comprehensive
                                            // with DB data

            // Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS,
                                               "createWorkRequest", inputs, response,
                                               transactionContext);

            // Check response parameter values
            if (response.containsKey("message")) {
                assertTrue("message is not null", response.get("message") != null);
            }
        } catch (Throwable e) {
            fail(" Global Exception " + e);
        } finally {
            // Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
    }

}
