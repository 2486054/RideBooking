package com.cts.repository;

import com.cts.entity.Ride;
import com.cts.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ride> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    List<Ride> findByStatus(RideStatus status);

    @Query("SELECT r FROM Ride r WHERE r.userId = :userId AND r.status = :status")
    List<Ride> findByUserIdAndStatus(@Param("userId") Long userId,
                                     @Param("status") RideStatus status);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.driverId = :driverId AND r.status IN ('ACCEPTED', 'IN_PROGRESS')")
    long countActiveRidesByDriver(@Param("driverId") Long driverId);
}