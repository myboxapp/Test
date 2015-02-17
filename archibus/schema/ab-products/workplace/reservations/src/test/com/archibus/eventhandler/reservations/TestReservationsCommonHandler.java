package com.archibus.eventhandler.reservations;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import com.archibus.fixture.EventHandlerFixture;
import com.archibus.utility.ExceptionBase;

/**
 * Tests common reservations event handlers.
 */
public class TestReservationsCommonHandler extends TestCase {

    private EventHandlerFixture fixture = null;

    static final String ACTIVITY_ID = "AbWorkplaceReservations";
    static final String EVENT_HANDLER_CLASS = "com.archibus.eventhandler.reservations.ReservationsCommonHandler";

    // ----------------------- required TestCase methods -------------------------------------------
    /**
     * JUnit test initialization method.
     */
    public void setUp() throws Exception {
        this.fixture = new EventHandlerFixture(this, "ab-ex-echo.axvw");
        this.fixture.setUp();
    }

    /**
     * JUnit clean-up method.
     */
    public void tearDown() {
        this.fixture.tearDown();
    }

    //-------------------------------------------------------------------------------------------------
    //Tests testNotifyRequestedBy WFR.
    //
/*	public void testNotifyRequestedBy() {

		//Begin database transaction
        Object transactionContext = fixture.beginTransaction();
        
        try {

        	//Add test input values
            Map inputs = new HashMap();
            inputs.put("res_id", "65"); //It must be obligatory and it must be comprehensive with DB data
            //Prepare response map
            Map response = new HashMap();

        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "notifyRequestedBy", inputs, response, transactionContext);

            //Check response parameter values
            if (response.containsKey("message")) {
            	assertTrue("message is not null", response.get("message")!=null);
            }
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
    //Tests testNotifyRequestedFor WFR.
    //	
	public void testNotifyRequestedFor() {

		//Begin database transaction
        Object transactionContext = fixture.beginTransaction();
        
        try {
        	//Add test input values
            Map inputs = new HashMap();
            inputs.put("res_id", "65"); //It must be obligatory and it must be comprehensive with DB data
            //Prepare response map
            Map response = new HashMap();

        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "notifyRequestedFor", inputs, response, transactionContext);

            //Check response parameter values
            if (response.containsKey("message")) {
            	assertTrue("message is not null", response.get("message")!=null);
            }
        }catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
    //Tests testApproveReservation and testRejectReservation WFRs.
	//-------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------
	// Approve a room reservation: Waited result: the reserve and the room 'Confirmed' 
	//
	public void testApproveReservation_rm_1() throws ExceptionBase{
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();
		
		try {
			int xrmres_id = 30; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","room");
			inputs.put("res_pk_list","<record reserve_rm.rmres_id=\""+xrmres_id+"\"><keys reserve_rm.rmres_id=\""+xrmres_id+"\"/></record>");
			
			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);
			
			if (response.containsKey("message")) {
				assertTrue("int this test exist a error emnsaje: ["+response.get("message")+"]", response.get("message")!=null);
			}
		} catch (Throwable e) {
			fail(" Global Exception "+e);
		} finally {
		    //Whatever happened, rollback all database changes
		    fixture.rollbackTransaction(transactionContext);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	// Approve a room and resource reservations: Waited result: the reserve and the room 'Confirmed' and the resource 'Aw. App.' 
	//
	public void testApproveReservation_rm_2() throws ExceptionBase {

		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();
        
		try {
			int xrmres_id = 31; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","room");
			inputs.put("res_pk_list","<record reserve_rm.rmres_id=\""+xrmres_id+"\"><keys reserve_rm.rmres_id=\""+xrmres_id+"\"/></record>");

			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);

			if (response.containsKey("message")) {
				assertTrue("message is not null", response.get("message")!=null);
			}
		} catch (Throwable e) {
			fail(" Global Exception "+e);
		} finally {
			//Whatever happened, rollback all database changes
			fixture.rollbackTransaction(transactionContext);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Approve a room and resource reservations: Waited result: the reserve, the room and the resource 'Confirmed' 
	//
	public void testApproveReservation_rm_3() throws ExceptionBase {
		//Begin database transaction
        Object transactionContext = fixture.beginTransaction();
        
        try {
        	int xrmres_id = 32; //It must be obligatory and it must be comprehensive with DB data
        	//Add test input values
            Map inputs = new HashMap();
            //Prepare response map
            Map response = new HashMap();

            inputs.put("res_type","room");
            inputs.put("res_pk_list","<record reserve_rm.rmres_id=\""+xrmres_id+"\"><keys reserve_rm.rmres_id=\""+xrmres_id+"\"/></record>");

        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")) {
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}
	
	//-------------------------------------------------------------------------------------------------
	// Approve a resource reservation: Waited result: the reserve and the resource 'Confirmed' 
	//
	public void testApproveReservation_rs_1() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

		try {
			int xrsres_id = 9; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");

			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);

			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
		
	}

	//-------------------------------------------------------------------------------------------------
	// Approve a resource reservation: Waited result: the resource 'Confirmed' and the reserve 'Aw. App.' 
	//
	public void testApproveReservation_rs_2() throws ExceptionBase {
		
		//Begin database transaction
        Object transactionContext = fixture.beginTransaction();
        
        try {
			int xrsres_id = 12; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");

			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);

			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
	// Approve a room and resource reservations: Waited result: the resource 'Confirmed' and the reserve and the room 'Aw. App.' 
	//
	public void testApproveReservation_rs_3() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 14; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
                    
			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
		
	}

	//-------------------------------------------------------------------------------------------------
	// Approve a room and resource reservations: Waited result: the resource and the reserve 'Confirmed' and the room 'Aw. App.' 
	//
	public void testApproveReservation_rs_4() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 15; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();
			
			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");

        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
	// Approve a room and resource reservations: Waited result: the resource and the room 'Confirmed' and the reserve 'Aw. App.' 
	//
	public void testApproveReservation_rs_5() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 16; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

            inputs.put("res_type","resource");
            inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
        	
        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "approveReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
		
	}

	//-------------------------------------------------------------------------------------------------
	// Reject a room and resource reservations: Waited result: the reserve, the resource and the room 'Rejected' 
	//
	public void testRejectReservation_rm_1() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrmres_id = 38; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","room");
			inputs.put("res_pk_list","<record reserve_rm.rmres_id=\""+xrmres_id+"\"><keys reserve_rm.rmres_id=\""+xrmres_id+"\"/></record>");

			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "rejectReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
	// Reject resource reservation: Waited result: the reserve and the resource 'Rejected' 
	//
	public void testRejectReservation_rs_1() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 20; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();
			
			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
        	
			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "rejectReservation", inputs, response, transactionContext);

			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
	// Reject resource reservation: Waited result: the resource 'Rejected', the reserve 'Aw. App.' 
	//
	public void testRejectReservation_rs_2() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 21; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();
			
			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
			
			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "rejectReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}
	
	//-------------------------------------------------------------------------------------------------
	// Reject room and resource reservations: Waited result: the resource 'Rejected', the reserve and the room 'Aw. App.' 
	//
	public void testRejectReservation_rs_3() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 21; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();
			
			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
			
			// execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "rejectReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}

	//-------------------------------------------------------------------------------------------------
	// Reject resource reservation: Waited result: the resource and the reserve 'Rejected' 
	//
	public void testRejectReservation_rs_4() throws ExceptionBase {
		//Begin database transaction
		Object transactionContext = fixture.beginTransaction();

        try {
			int xrsres_id = 24; //It must be obligatory and it must be comprehensive with DB data
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_type","resource");
			inputs.put("res_pk_list","<record reserve_rs.rsres_id=\""+xrsres_id+"\"><keys reserve_rs.rsres_id=\""+xrsres_id+"\"/></record>");
			
			// execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "rejectReservation", inputs, response, transactionContext);
            
			if (response.containsKey("message")){
				assertTrue("message is not null", response.get("message")!=null);
			}
        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
        	fixture.rollbackTransaction(transactionContext);
        }
	}

    //-------------------------------------------------------------------------------------------------
	//Tests testCloseReservations WFR.
    //
	public void testCloseReservations()  throws ExceptionBase{
		//Begin database transaction
        Object transactionContext = fixture.beginTransaction();

        try {
        	//Add test input values
            Map inputs = new HashMap();
            //Prepare response map
            Map response = new HashMap();

        	//Execute WFR event handler
            this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "closeReservations", inputs, response, transactionContext);

            if ( response.containsKey("message") ){
            	assertTrue("message is not null", response.get("message")!=null);
            }
            else {
            	assertTrue(true);
            }

        } catch (Throwable e) {
        	fail(" Global Exception "+e);
        } finally {
            //Whatever happened, rollback all database changes
            fixture.rollbackTransaction(transactionContext);
        }
	}
*/	
	//-------------------------------------------------------------------------------------------------
	//Tests testSendEmailInvitations WFR.
	//
	public void testSendEmailInvitations() throws ExceptionBase {
		//Begin database transaction
		 Object transactionContext = fixture.beginTransaction();

		try {
			//Add test input values
			Map inputs = new HashMap();
			//Prepare response map
			Map response = new HashMap();

			inputs.put("res_id","92"); //It must be obligatory and it must be comprehensive with DB data
			inputs.put("res_parent","0"); //It must be obligatory and it must be comprehensive with DB data
			inputs.put("Reservation", "{\"phone\":\"227-2508\",\"comments\":\"\",\"recur_val1\":[null,null,null,null,\"4\",null,null],\"attendees\":\"rdiaz@ingenia.es\",\"res_type\":\"recurring\",\"group_size\":\"6\",\"date_start\":[\"2007-03-09\",\"2007-03-15\",\"2007-03-22\"],\"recurring_rule\":\"<options type='week'><ndays value='' /><weekly mon='false' tue='false' wed='false' thu='true' fri='false' sat='false' sun='false' /><monthly 1st='false' 2nd='false' 3rd='false' 4th='false' last='false' mon='false' tue='false' wed='false' thu='false' fri='false' sat='false' sun='false' /></options>\",\"reservation_name\":\"Recurring Res. for: HQ-19-110 2007-03-08 09:00:00\",\"time_end\":\"10:00:00\",\"rm_arrange_type_id\":\"CONFERENCE\",\"user_requested_for\":\"AFM\",\"all_resource_stds\":[{\"type\":\"CATERING-COLD\",\"name\":\"Cold food catering\"},{\"type\":\"PROJECTOR-FIXED\",\"name\":\"Projector - Fixed\"},{\"type\":\"PROJECTOR-LCD\",\"name\":\"Projector - LCD - portable\"},{\"type\":\"TV - 50 INCH\",\"name\":\"TV - 50 Inch large screen\"},{\"type\":\"CATERING-COLD 2\",\"name\":\"Cold food catering Class A\"},{\"type\":\"CHAIRS\",\"name\":\"Chairs\"},{\"type\":\"COFFEE\",\"name\":\"Coffee Can\"},{\"type\":\"IT-SUPPORT\",\"name\":\"IT Person\"},{\"type\":\"NETWORK-CABLE\",\"name\":\"Network cable for PC\"},{\"type\":\"SOFT DRINKS\",\"name\":\"Soft Drinks No Alcohol\"},{\"type\":\"SPECIAL DECORAT.\",\"name\":\"Special Event Decorations\"},{\"type\":\"TABLES\",\"name\":\"Tables\"}],\"recur_val2\":[null],\"resource_stds\":[],\"dv_id\":\"ELECTRONIC SYS.\",\"dp_id\":\"ENGINEERING\",\"res_id\":\"\",\"time_start\":\"09:30:00\",\"user_created_by\":\"AFM\",\"status\":\"\",\"require_reply\":false,\"user_requested_by\":\"AFM\",\"cost_res\":\"\",\"recur_type\":\"week\",\"ext_guest\":\"0\",\"ctry_id\":\"USA\",\"date_end\":\"2007-03-22\",\"site_id\":\"MARKET\",\"email\":\"afm@tgd.com\",\"rm_id\":\"110\",\"bl_id\":\"HQ\",\"fl_id\":\"19\"}");
			inputs.put("RoomConflicts", "[]");
			inputs.put("invitation_type","update");
			inputs.put("email_invitations", "lmmartin@ingenia.es"); //It must be obligatory and it must be comprehensive with DB data
			inputs.put("date_cancel", "");
			inputs.put("time_cancel", "");
			inputs.put("original_date", "2007-03-09");
			inputs.put("require_reply", new Boolean(false)); //It must be obligatory and it must be comprehensive with DB data

			//Execute WFR event handler
			this.fixture.runEventHandlerMethod(ACTIVITY_ID, EVENT_HANDLER_CLASS, "sendEmailInvitations", inputs, response, transactionContext);

			if ( response.containsKey("message") ){
				assertTrue("message is not null", response.get("message")!=null);
			}
			else {
				assertTrue(true);
			}
		} catch (Throwable e) {
			fail(" Global Exception "+e);
		} finally {
			//Whatever happened, rollback all database changes
			fixture.rollbackTransaction(transactionContext);
		}
	}
}
