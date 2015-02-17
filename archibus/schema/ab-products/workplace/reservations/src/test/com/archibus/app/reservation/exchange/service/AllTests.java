package com.archibus.app.reservation.exchange.service;

import junit.framework.*;

/**
 * All tests in the exchange service package.
 * <p>
 * Suppress warning "PMD.TestClassWithoutTestCases".
 * <p>
 * Justification: this is a suite that groups other actions tests.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class AllTests extends TestCase {
    
    /**
     * Constructor specifying a name for the exchange service test.
     * 
     * @param name the name
     */
    public AllTests(final String name) {
        super(name);
    }
    
    /**
     * Get test suite for the exchange service package.
     * 
     * @return suite suite
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(ExchangeCalendarServiceTest.class);
        suite.addTestSuite(ExchangeServiceHelperTest.class);
        
        // this test takes very long to execute...
        suite.addTestSuite(ExchangeListenerTest.class);
        return suite;
    }
}
