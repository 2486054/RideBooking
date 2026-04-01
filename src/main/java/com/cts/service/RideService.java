package com.cts.service;

import com.cts.dto.*;
import com.cts.entity.Ride;
import com.cts.entity.RideStatus;
import com.cts.exception.InvalidRideStateException;
import com.cts.exception.RideNotFoundException;
import com.cts.exception.UnauthorizedAccessException;
import com.cts.repository.RideRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final FareCalculationService fareService;

    public RideService(RideRepository rideRepository, FareCalculationService fareService) {
        this.rideRepository = rideRepository;
        this.fareService = fareService;
    }

    // --- Book a new ride (CUSTOMER) ---
    @Transactional
    public RideResponse bookRide(BookRideRequest request, Long userId) {
        Ride ride = new Ride();
        ride.setUserId(userId);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setFare(fareService.calculateFare(request.getPickupLocation(), request.getDropoffLocation()));
        ride.setStatus(RideStatus.REQUESTED);

        Ride saved = rideRepository.save(ride);
        return mapToResponse(saved);
    }

    // --- Assign a driver to a ride (ADMIN) ---
    @Transactional
    public RideResponse assignDriver(Long rideId, AssignDriverRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException("Driver can only be assigned to rides in REQUESTED status");
        }

        ride.setDriverId(request.getDriverId());
        ride.setStatus(RideStatus.ACCEPTED);
        Ride updated = rideRepository.save(ride);
        return mapToResponse(updated);
    }

    // --- Accept a ride (DRIVER) ---
    @Transactional
    public RideResponse acceptRide(Long rideId, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException("Only REQUESTED rides can be accepted");
        }

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        Ride updated = rideRepository.save(ride);
        return mapToResponse(updated);
    }

    // --- Update ride status (DRIVER) ---
    @Transactional
    public RideResponse updateRideStatus(Long rideId, UpdateRideStatusRequest request, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getDriverId().equals(driverId)) {
            throw new UnauthorizedAccessException("You are not assigned to this ride");
        }

        validateStatusTransition(ride.getStatus(), request.getStatus());
        ride.setStatus(request.getStatus());
        Ride updated = rideRepository.save(ride);
        return mapToResponse(updated);
    }

    // --- Cancel a ride (CUSTOMER) ---
    @Transactional
    public RideResponse cancelRide(Long rideId, Long userId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only cancel your own rides");
        }

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Cannot cancel a ride that is already " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        Ride updated = rideRepository.save(ride);
        return mapToResponse(updated);
    }

    // --- Get ride by ID ---
    public RideResponse getRideById(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with id: " + rideId));
        return mapToResponse(ride);
    }

    // --- Get rides by user ---
    public List<RideResponse> getRidesByUser(Long userId) {
        return rideRepository.findByUserId(userId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Get rides by driver ---
    public List<RideResponse> getRidesByDriver(Long driverId) {
        return rideRepository.findByDriverId(driverId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Get all available (REQUESTED) rides for drivers ---
    public List<RideResponse> getAvailableRides() {
        return rideRepository.findByStatus(RideStatus.REQUESTED)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- Validate status transitions ---
    private void validateStatusTransition(RideStatus current, RideStatus next) {
        boolean valid = switch (current) {
            case REQUESTED -> next == RideStatus.ACCEPTED || next == RideStatus.CANCELLED;
            case ACCEPTED -> next == RideStatus.IN_PROGRESS || next == RideStatus.CANCELLED;
            case IN_PROGRESS -> next == RideStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidRideStateException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    // --- Map Entity to Response DTO ---
    private RideResponse mapToResponse(Ride ride) {
        return new RideResponse(
                ride.getRideId(),
                ride.getUserId(),
                ride.getDriverId(),
                ride.getPickupLocation(),
                ride.getDropoffLocation(),
                ride.getFare(),
                ride.getStatus(),
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }
}
