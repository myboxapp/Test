package com.archibus.app.reservation;

/**
 * Test data constants.
 * 
 * @author bv
 * @since 20.1
 * 
 */
public final class TestData {
    
    /** User that is used to create reservations. */
    public static final String USER_ID = "AFM";
    
    /** Building identifier for the reservation. */
    public static final String BL_ID = "HQ";
    
    /** Floor identifier for the reservation. */
    public static final String FL_ID = "19";
    
    /** Room identifier for the reservation. */
    public static final String RM_ID = "110";
    
    /** Arrangement type identifier for the reservation. */
    public static final String ARRANGE_TYPE_ID = "THEATER";
    
    /** Configuration identifier for the reservation. */
    public static final String CONFIG_ID = "A1";
    
    /** unique key. */
    public static final String UNIQUE_ID = "0003FFABCDDE00040A";
    
    /** days in advance. */
    public static final int DAYS_IN_ADVANCE = 7;
    
    /**
     * 
     * Private default constructor: utility class is non-instantiable.
     * 
     * @throws InstantiationException InstantiationException
     */
    private TestData() throws InstantiationException {
        throw new InstantiationException("Never instantiate " + this.getClass().getName()
                + "; use static methods!");
        
    }
    
}
