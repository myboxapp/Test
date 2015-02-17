package com.archibus.app.reservation.service;

import java.util.*;

import junit.framework.Assert;

import com.archibus.app.reservation.dao.datasource.*;
import com.archibus.app.reservation.domain.*;
import com.archibus.context.ContextStore;
import com.archibus.datasource.data.*;
import com.archibus.datasource.restriction.Restrictions;

/**
 * Test class for ApproveReservationService.
 * 
 * @author Yorik Gerlo
  * <p>
 * Suppress warning "PMD.TooManyMethods".
 * <p>
 * Justification: the JUnit tests for this class should be kept in one test class.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class ApproveReservationServiceTest extends RoomReservationServiceTestBase {
    
    /** Approval days field name. */
    private static final String APPROVE_DAYS = "approve_days";

    /** Qualified primary key field of the reserve table. */
    private static final String RESERVE_RES_ID = "reserve.res_id";
    
    /** Message used as comments for rejection. */
    private static final String REJECTION_MESSAGE = "Rejected for test.";
    
    /** Identifier of a resource that requires approval. */
    private static final String RESOURCE_ID1 = "ROOM DECORATION";
    
    /** Identifier of a resource that requires approval. */
    private static final String RESOURCE_ID2 = "TV1";
    
    /** Identifier of an other resource that requires approval. */
    private static final String RESOURCE_ID3 = "TV2";
    
    /** Identifier of a resource that requires no approval. */
    private static final String RESOURCE_ID4 = "LCD-PROJECTOR1";
    
    /** Qualified primary key field name for room allocations. */
    private static final String ROOM_PKEY = "reserve_rm.rmres_id";
    
    /** Qualified primary key field name for resource allocations. */
    private static final String RESOURCE_PKEY = "reserve_rs.rsres_id";
    
    /** The reservation approval service under test. */
    private ApproveReservationService approveReservationService;
    
    /** The room arrangement data source for changing approve_days and expire action. */
    private RoomArrangementDataSource roomArrangementDataSource;
    
    /** The resource reservation data source to test resource-only reservations. */
    private ResourceReservationDataSource resourceReservationDataSource;
    
    /** The resource data source to change approve_days and expire action. */
    private ResourceDataSource resourceDataSource;
    
    /**
     * Set up for testing the Approval Reservation Service.
     * 
     * @throws Exception when setup fails, e.g. because of bad time formatting
     *             <p>
     *             Suppress Warning "PMD.SignatureDeclareThrowsException"
     *             <p>
     *             Justification: the overridden method also throws it.
     */
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        
        // Use getBean because this service class exists twice in the Spring configuration files.
        // Using autowired doesn't work.
        this.approveReservationService =
                (ApproveReservationService) ContextStore.get().getBean("approveReservationService");
    }

    /**
     * Test approving a room reservation.
     */
    public void testApproveRoomReservation() {
        final DataRecord reservationRecord = this.createAndSaveRoomReservation(false);
        final Integer reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        final RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertNotNull(reservation);
        
        final List<Integer> roomReservationIds = new ArrayList<Integer>();
        roomReservationIds.add(reservation.getRoomAllocations().get(0).getId());
        
        this.approveReservationService.approveReservation(
            ApproveReservationService.RESERVATION_TYPE_ROOM,
            convertToDataSetList(this.roomAllocationDataSource, ROOM_PKEY, roomReservationIds));
        
        verifyConfirmedStatus(reservationId, true);
    }
    
    /**
     * Verify that the entire reservation has been confirmed.
     * 
     * @param reservationId id of the master reservation record
     * @param checkCalendar whether to verify the meeting on the Exchange calendar
     */
    private void verifyConfirmedStatus(final Integer reservationId, final boolean checkCalendar) {
        final RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertEquals(Constants.STATUS_CONFIRMED, reservation.getStatus());
        for (final RoomAllocation allocation : reservation.getRoomAllocations()) {
            Assert.assertEquals(Constants.STATUS_CONFIRMED, allocation.getStatus());
        }
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            Assert.assertEquals(Constants.STATUS_CONFIRMED, allocation.getStatus());
        }
        
        if (checkCalendar) {
            Assert.assertNotNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
                reservation.getUniqueId()));
        }
    }
    
    /**
     * Test rejecting a room reservation.
     */
    public void testRejectRoomReservation() {
        final DataRecord reservationRecord = this.createAndSaveRoomReservation(false);
        final Integer reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        final RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
            reservation.getUniqueId()));
        
        final List<Integer> roomReservationIds = new ArrayList<Integer>();
        roomReservationIds.add(reservation.getRoomAllocations().get(0).getId());
        
        this.approveReservationService.rejectReservation(
            ApproveReservationService.RESERVATION_TYPE_ROOM,
            convertToDataSetList(this.roomAllocationDataSource, ROOM_PKEY, roomReservationIds),
            REJECTION_MESSAGE);
        
        verifyRejectedStatus(reservationId, true);
    }
    
    /**
     * Verify that the entire reservation has been rejected.
     * 
     * @param reservationId identifier of the master reservation record
     * @param checkCalendar whether to check the meeting on the Exchange calendar
     */
    private void verifyRejectedStatus(final Integer reservationId, final boolean checkCalendar) {
        final RoomReservation reservation =
                this.roomReservationDataSource.get(reservationId);
        Assert.assertEquals(Constants.STATUS_REJECTED, reservation.getStatus());
        for (final RoomAllocation allocation : reservation.getRoomAllocations()) {
            Assert.assertEquals(Constants.STATUS_REJECTED, allocation.getStatus());
        }
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            Assert.assertEquals(Constants.STATUS_REJECTED, allocation.getStatus());
        }
        
        if (checkCalendar) {
            Assert.assertNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
                reservation.getUniqueId()));
        }
    }
    
    /**
     * Test approving a room reservation with resource reservations included.
     */
    public void testApproveRoomReservationWithResources() {
        final DataRecord reservationRecord = this.createAndSaveRoomReservation(false);
        final Integer reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        addResourceAllocation(reservation, RESOURCE_ID1);
        addResourceAllocation(reservation, RESOURCE_ID2);
        addResourceAllocation(reservation, RESOURCE_ID3);
        addResourceAllocation(reservation, RESOURCE_ID4);
        reservation = this.roomReservationDataSource.get(reservationId);
        
        final Map<String, ResourceAllocation> allocations =
                new HashMap<String, ResourceAllocation>();
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            allocations.put(allocation.getResourceId(), allocation);
        }
        
        final List<Integer> identifiers = new ArrayList<Integer>();
        
        // First confirm one resource.
        identifiers.add(allocations.get(RESOURCE_ID1).getId());
        this.approveReservationService.approveReservation(
            ApproveReservationService.RESERVATION_TYPE_RESOURCE,
            convertToDataSetList(this.resourceAllocationDataSource, RESOURCE_PKEY, identifiers));
        
        // Check that the reservation is still awaiting approval.
        reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertEquals(Constants.STATUS_AWAITING_APP, reservation.getStatus());
        
        // Confirm the room.
        identifiers.clear();
        identifiers.add(reservation.getRoomAllocations().get(0).getId());
        this.approveReservationService.approveReservation(
            ApproveReservationService.RESERVATION_TYPE_ROOM,
            convertToDataSetList(this.roomAllocationDataSource, ROOM_PKEY, identifiers));
        
        // Check that the reservation is still awaiting approval.
        reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertEquals(Constants.STATUS_AWAITING_APP, reservation.getStatus());
        
        // Confirm all resources.
        identifiers.clear();
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            identifiers.add(allocation.getId());
        }
        this.approveReservationService.approveReservation(
            ApproveReservationService.RESERVATION_TYPE_RESOURCE,
            convertToDataSetList(this.resourceAllocationDataSource, RESOURCE_PKEY, identifiers));
        
        verifyConfirmedStatus(reservationId, true);
    }
    
    /**
     * Test rejecting a room reservation with resource reservations included.
     */
    public void testRejectRoomReservationWithResources() {
        final DataRecord reservationRecord = this.createAndSaveRoomReservation(false);
        final Integer reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        addResourceAllocation(reservation, RESOURCE_ID1);
        addResourceAllocation(reservation, RESOURCE_ID2);
        addResourceAllocation(reservation, RESOURCE_ID3);
        addResourceAllocation(reservation, RESOURCE_ID4);
        
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
            reservation.getUniqueId()));
        
        // First approve/reject some of the resource allocations.
        reservation = this.roomReservationDataSource.get(reservationId);
        
        final Map<String, ResourceAllocation> allocations =
                new HashMap<String, ResourceAllocation>();
        for (final ResourceAllocation allocation : reservation.getResourceAllocations()) {
            allocations.put(allocation.getResourceId(), allocation);
        }
        
        final List<Integer> identifiers = new ArrayList<Integer>();
        
        // Confirm one resource.
        identifiers.add(allocations.get(RESOURCE_ID1).getId());
        this.approveReservationService.approveReservation(
            ApproveReservationService.RESERVATION_TYPE_RESOURCE,
            convertToDataSetList(this.resourceAllocationDataSource, RESOURCE_PKEY, identifiers));
        
        // Reject an other resource.
        identifiers.clear();
        identifiers.add(allocations.get(RESOURCE_ID2).getId());
        this.approveReservationService.rejectReservation(
            ApproveReservationService.RESERVATION_TYPE_RESOURCE,
            convertToDataSetList(this.resourceAllocationDataSource, RESOURCE_PKEY, identifiers),
            "test reject resource");
        
        Assert.assertEquals(Constants.STATUS_AWAITING_APP,
            this.roomReservationDataSource.get(reservationId).getStatus());
        
        identifiers.clear();
        identifiers.add(reservation.getRoomAllocations().get(0).getId());
        this.approveReservationService.rejectReservation(
            ApproveReservationService.RESERVATION_TYPE_ROOM,
            convertToDataSetList(this.roomAllocationDataSource, ROOM_PKEY, identifiers),
            REJECTION_MESSAGE);
        
        verifyRejectedStatus(reservationId, true);
    }
    
    /**
     * Test running the scheduled WFR for automated approve / reject / notify.
     */
    public void testCheckRoomApproval() {
        DataRecord reservationRecord = this.createAndSaveRoomReservation(false);
        Integer reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        RoomReservation reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
            reservation.getUniqueId()));
        
        this.roomArrangementDataSource.addRestriction(Restrictions.eq(
            this.roomArrangementDataSource.getMainTableName(), Constants.BL_ID_FIELD_NAME, BL_ID));
        this.roomArrangementDataSource.addRestriction(Restrictions.eq(
            this.roomArrangementDataSource.getMainTableName(), Constants.FL_ID_FIELD_NAME, FL_ID));
        this.roomArrangementDataSource.addRestriction(Restrictions.eq(
            this.roomArrangementDataSource.getMainTableName(), Constants.RM_ID_FIELD_NAME, RM_ID));
        this.roomArrangementDataSource.addRestriction(Restrictions.eq(
            this.roomArrangementDataSource.getMainTableName(), Constants.CONFIG_ID_FIELD_NAME,
            CONFIG_ID));
        this.roomArrangementDataSource.addRestriction(Restrictions.eq(
            this.roomArrangementDataSource.getMainTableName(),
            Constants.RM_ARRANGE_TYPE_ID_FIELD_NAME, ARRANGE_TYPE_ID));
        this.roomArrangementDataSource.addField(this.roomArrangementDataSource.getMainTableName(),
            ApproveReservationService.ACTION_APPROVAL_EXPIRED);
        final DataRecord arrangement = this.roomArrangementDataSource.getRecord();
        arrangement.setValue(this.roomArrangementDataSource.getMainTableName() + Constants.DOT
                + APPROVE_DAYS, DAYS_IN_WEEK * 2);
        arrangement.setValue(this.roomArrangementDataSource.getMainTableName() + Constants.DOT
                + ApproveReservationService.ACTION_APPROVAL_EXPIRED,
            ApproveReservationService.EXPIRE_APPROVE);
        this.roomArrangementDataSource.updateRecord(arrangement);
        
        this.approveReservationService.checkRoomAndResourcesApproval();
        verifyConfirmedStatus(reservationId, true);
        
        this.roomReservationDataSource.cancel(reservation);
        reservationRecord = this.createAndSaveRoomReservation(false);
        reservationId = (Integer) reservationRecord.getValue(RESERVE_RES_ID);
        reservation = this.roomReservationDataSource.get(reservationId);
        Assert.assertNotNull(this.appointmentHelper.bindToAppointment(reservation.getEmail(),
            reservation.getUniqueId()));
        
        arrangement.setValue(this.roomArrangementDataSource.getMainTableName() + Constants.DOT
                + ApproveReservationService.ACTION_APPROVAL_EXPIRED,
            ApproveReservationService.EXPIRE_REJECT);
        this.roomArrangementDataSource.updateRecord(arrangement);
        
        this.approveReservationService.checkRoomAndResourcesApproval();
        verifyRejectedStatus(reservationId, true);
    }
    
    /**
     * Test running the scheduled WFR for automated approve / reject / notify.
     */
    public void testCheckResourcesApproval() {
        DataRecord reservationRecord = this.resourceReservationDataSource.createNewRecord();
        this.createReservation(reservationRecord, false);
        DataRecord pkeyRecord =
                this.resourceReservationDataSource.saveRecord(reservationRecord);
        Integer reservationId = pkeyRecord.getInt(RESERVE_RES_ID);
        Assert.assertNotNull(reservationId);
        
        ResourceReservation reservation = this.resourceReservationDataSource.get(reservationId);
        addResourceAllocation(reservation, RESOURCE_ID1);

        this.resourceDataSource.addRestriction(Restrictions.eq(
            this.resourceDataSource.getMainTableName(), Constants.RESOURCE_ID_FIELD, RESOURCE_ID1));
        this.resourceDataSource.addField(this.resourceDataSource.getMainTableName(),
            "action_approval_expired");
        final DataRecord resource = this.resourceDataSource.getRecord();
        resource.setValue(
            this.resourceDataSource.getMainTableName() + Constants.DOT + APPROVE_DAYS,
            DAYS_IN_WEEK * 2);
        resource.setValue(this.resourceDataSource.getMainTableName() + Constants.DOT
                + ApproveReservationService.ACTION_APPROVAL_EXPIRED,
            ApproveReservationService.EXPIRE_APPROVE);
        this.resourceDataSource.updateRecord(resource);
        
        this.approveReservationService.checkRoomAndResourcesApproval();
        verifyConfirmedStatus(reservationId, false);
        
        this.resourceReservationDataSource.cancel(reservation);
        reservationRecord = this.resourceReservationDataSource.createNewRecord();
        this.createReservation(reservationRecord, false);
        pkeyRecord = this.resourceReservationDataSource.saveRecord(reservationRecord);
        reservationId = pkeyRecord.getInt(RESERVE_RES_ID);
        Assert.assertNotNull(reservationId);

        reservation = this.resourceReservationDataSource.get(reservationId);
        addResourceAllocation(reservation, RESOURCE_ID1);
        
        resource.setValue(this.resourceDataSource.getMainTableName() + Constants.DOT
                + ApproveReservationService.ACTION_APPROVAL_EXPIRED,
            ApproveReservationService.EXPIRE_REJECT);
        this.resourceDataSource.updateRecord(resource);
        
        this.approveReservationService.checkRoomAndResourcesApproval();
        verifyRejectedStatus(reservationId, false);
    }
    
    /**
     * Set the room arrangement data source.
     * 
     * @param roomArrangementDataSource the new room arrangement data source
     */
    public void setRoomArrangementDataSource(
            final RoomArrangementDataSource roomArrangementDataSource) {
        this.roomArrangementDataSource = roomArrangementDataSource;
    }
    
    /**
     * Set the resource reservation data source.
     * 
     * @param resourceReservationDataSource the new resource reservation data source
     */
    public void setResourceReservationDataSource(
            final ResourceReservationDataSource resourceReservationDataSource) {
        this.resourceReservationDataSource = resourceReservationDataSource;
    }
    
    /**
     * Set the resource data source.
     * 
     * @param resourceDataSource the new resource data source
     */
    public void setResourceDataSource(final ResourceDataSource resourceDataSource) {
        this.resourceDataSource = resourceDataSource;
    }
    
    /**
     * Add a resource allocation to the given reservation and save it.
     * 
     * @param reservation the reservation to add a resource allocation to
     * @param resourceId identifier of the resource to allocate
     */
    private void addResourceAllocation(final AbstractReservation reservation,
            final String resourceId) {
        final Resource resource = new Resource();
        resource.setResourceId(resourceId);
        
        final ResourceAllocation resourceAllocation =
                new ResourceAllocation(resource, reservation, 1);
        
        // the resource allocation should be in a room
        resourceAllocation.setBlId(BL_ID);
        resourceAllocation.setFlId(FL_ID);
        resourceAllocation.setRmId(RM_ID);
        
        reservation.addResourceAllocation(resourceAllocation);
        if (reservation instanceof RoomReservation) {
            this.roomReservationDataSource.save((RoomReservation) reservation);
        } else {
            this.resourceReservationDataSource.save((ResourceReservation) reservation);
        }
    }
    
    /**
     * Convert a list of identifiers to a data set list.
     * 
     * @param dataSource the data source to use
     * @param pkFieldName fully qualified name of the primary key field
     * @param identifiers list of identifiers to convert
     * @return the corresponding data set list
     */
    private DataSetList convertToDataSetList(final AbstractAllocationDataSource<?> dataSource,
            final String pkFieldName, final List<Integer> identifiers) {
        final DataSetList list = new DataSetList();
        for (final Integer identifier : identifiers) {
            final DataRecord record = dataSource.createNewRecord();
            record.setValue(pkFieldName, identifier);
            list.addRecord(record);
        }
        return list;
    }
    
}
