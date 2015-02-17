package com.archibus.app.reservation.domain;

import junit.framework.Assert;

import com.archibus.datasource.DataSourceTestBase;

/**
 * Test for ReservationException.
 */
public class ReservationExceptionTest extends DataSourceTestBase {
    
    /** Message pattern used for testing. */
    private static final String EXCEPTION_MESSAGE_PATTERN = "Test [{0}] [{1}]";
    
    /** Message pattern used for testing. */
    private static final String EXCEPTION_MESSAGE = "Test [hello] [world]";
    
    /** Additional arguments for the exception constructor. */
    private static final Object[] ADDITIONAL_ARGUMENTS = { "hello", "world" };
    
    /**
     * Test creating a reservation exception without additional parameters.
     */
    public void testReservationException1() {
        try {
            throw new ReservationException(EXCEPTION_MESSAGE_PATTERN,
                ReservationExceptionTest.class);
        } catch (final ReservationException exception) {
            Assert.assertEquals(EXCEPTION_MESSAGE_PATTERN, exception.getPattern());
        }
    }
    
    /**
     * Test creating a reservation exception with additional parameters.
     */
    public void testReservationException2() {
        try {
            throw new ReservationException(EXCEPTION_MESSAGE_PATTERN,
                ReservationExceptionTest.class, ADDITIONAL_ARGUMENTS);
        } catch (final ReservationException exception) {
            Assert.assertEquals(EXCEPTION_MESSAGE, exception.getPattern());
        }
        try {
            throw new ReservationException(EXCEPTION_MESSAGE_PATTERN,
                ReservationExceptionTest.class, ADDITIONAL_ARGUMENTS[0], ADDITIONAL_ARGUMENTS[1]);
        } catch (final ReservationException exception) {
            Assert.assertEquals(EXCEPTION_MESSAGE, exception.getPattern());
        }
    }
    
}
