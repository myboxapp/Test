package com.archibus.app.reservation.service.actions;

import junit.framework.*;

/**
 * All tests in the service actions package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other actions tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor specifying a name for the actions test.
     * 
     * @param name the name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get test suite for the service action package.
     * 
     * @return suite suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(SaveRecurringReservationOccurrenceActionTest.class);
        suite.addTestSuite(VerifyRecurrencePatternOccurrenceActionTest.class);
        return suite;
    }
}
