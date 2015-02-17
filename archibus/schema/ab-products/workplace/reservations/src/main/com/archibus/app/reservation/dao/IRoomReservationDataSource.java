package com.archibus.app.reservation.dao;

import java.util.List;

import com.archibus.app.reservation.domain.*;
import com.archibus.datasource.data.DataRecord;

/**
 * The Interface IRoomReservationDataSource.
 */
public interface IRoomReservationDataSource extends IReservationDataSource<RoomReservation> {
    
    /**
     * Get room reservations by unique id.
     * 
     * @param uniqueId the unique id
     * @param timeZoneId the time zone id
     * @return list of room reservations
     */
    List<RoomReservation> getByUniqueId(final String uniqueId, final String timeZoneId);
    
    /**
     * Clear the unique ID coming from Exchange.
     * 
     * The reservation is de-coupled from the appointment in MS Exchange.
     * 
     * @param reservation reservation object
     * @return reservation
     * 
     * @throws ReservationException reservation exception is thrown when the reservation cannot be
     *             found
     */
    RoomReservation clearUniqueId(final RoomReservation reservation) throws ReservationException;
    
    /**
     * Convert data records to RoomReservation domain object.
     * 
     * @param reservationRecord the reservation record
     * @param roomAllocationRecord the room allocation record
     * @param resourceAllocationRecords the resource allocation records
     * @return room reservation object
     */
    RoomReservation convertRecordToObject(final DataRecord reservationRecord,
            final DataRecord roomAllocationRecord, final List<DataRecord> resourceAllocationRecords);
    
}
