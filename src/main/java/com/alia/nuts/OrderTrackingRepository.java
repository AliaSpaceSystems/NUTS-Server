package com.alia.nuts;

import com.alia.nuts.db.OrderTracking;
import com.alia.nuts.db.TrackingData;
import org.hibernate.mapping.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Integer> {
    //@Query("SELECT u FROM OrderTracking u WHERE u.uuid_user = :uuid_user and u.uuid_session = :uuid_session and not u.status = 'CANCELLED' ORDER BY u.id DESC")
    @Query(value = "SELECT * FROM order_tracking WHERE uuid_user = :uuid_user and uuid_session = :uuid_session and  status != 'CANCELLED' ORDER BY order_id DESC LIMIT 1", nativeQuery = true)
    Optional<OrderTracking> findOrderByUserAndSessionParams(
            @Param("uuid_user") String uuid_user,
            @Param("uuid_session") String uuid_session);

    @Query(value = "SELECT u FROM OrderTracking u WHERE u.uuid_user = :uuid_user and u.uuid_session = :uuid_session and not u.status = 'CANCELLED' ORDER BY u.id DESC")
    //@Query(value = "SELECT * FROM order_tracking WHERE uuid_user = :uuid_user and uuid_session = :uuid_session and status != 'CANCELLED' ORDER BY order_id DESC LIMIT 1", nativeQuery = true)
    Optional<TrackingData> findTrackingByUserAndSessionParams(
            @Param("uuid_user") String uuid_user,
            @Param("uuid_session") String uuid_session);


}
