package com.cts.repository;

import com.cts.entity.Ride;
import com.cts.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByUserId(Long userId);
    List<Ride> findByDriverId(Long driverId);
    List<Ride> findByStatus(RideStatus status);
    List<Ride> findByUserIdAndStatus(Long userId, RideStatus status);
    List<Ride> findByDriverIdAndStatus(Long driverId, RideStatus status);
}