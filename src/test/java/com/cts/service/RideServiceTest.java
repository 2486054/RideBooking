package com.cts.service;

import com.cts.dto.*;
import com.cts.entity.Ride;
import com.cts.entity.RideStatus;
import com.cts.entity.Role;
import com.cts.exception.InvalidRideStateException;
import com.cts.exception.RideNotFoundException;
import com.cts.exception.UnauthorizedAccessException;
import com.cts.feign.AuthServiceClient;
import com.cts.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private FareCalculationService fareService;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private RideService rideService;

    private static final String TOKEN = "Bearer test-token";
    private static final String CUSTOMER_EMAIL = "john@gmail.com";
    private static final String DRIVER_EMAIL = "ravi@gmail.com";

    private UserResponse customerUser;
    private UserResponse driverUser;
    private Ride sampleRide;

    @BeforeEach
    void setUp() {
        customerUser = new UserResponse(1L, "John", CUSTOMER_EMAIL, "9876543210", Role.CUSTOMER);
        driverUser = new UserResponse(2L, "Ravi", DRIVER_EMAIL, "9876543211", Role.DRIVER);

        sampleRide = Ride.builder()
                .rideId(1L)
                .userId(1L)
                .pickupLocation("MG Road, Chennai")
                .dropoffLocation("T Nagar, Chennai")
                .fare(new BigDecimal("170.00"))
                .status(RideStatus.REQUESTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =============================================
    // Book Ride Tests
    // =============================================
    @Nested
    @DisplayName("Book Ride Tests")
    class BookRideTests {

        @Test
        @DisplayName("Should book a ride successfully")
        void shouldBookRideSuccessfully() {
            BookRideRequest request = new BookRideRequest("MG Road", "T Nagar");

            when(authServiceClient.getUserByEmail(CUSTOMER_EMAIL, TOKEN)).thenReturn(customerUser);
            when(fareService.calculateFare(anyString(), anyString())).thenReturn(new BigDecimal("170.00"));
            when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide);

            RideResponse response = rideService.bookRide(request, CUSTOMER_EMAIL, TOKEN);

            assertThat(response).isNotNull();
            assertThat(response.getRideId()).isEqualTo(1L);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
            assertThat(response.getFare()).isEqualTo(new BigDecimal("170.00"));

            verify(authServiceClient).getUserByEmail(CUSTOMER_EMAIL, TOKEN);
            verify(fareService).calculateFare(anyString(), anyString());
            verify(rideRepository).save(any(Ride.class));
        }
    }

    // =============================================
    // Accept Ride Tests
    // =============================================
    @Nested
    @DisplayName("Accept Ride Tests")
    class AcceptRideTests {

        @Test
        @DisplayName("Should accept a ride successfully")
        void shouldAcceptRideSuccessfully() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(DRIVER_EMAIL, TOKEN)).thenReturn(driverUser);
            when(rideRepository.countActiveRidesByDriver(2L)).thenReturn(0L);

            Ride acceptedRide = Ride.builder()
                    .rideId(1L).userId(1L).driverId(2L)
                    .pickupLocation("MG Road").dropoffLocation("T Nagar")
                    .fare(new BigDecimal("170.00")).status(RideStatus.ACCEPTED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(rideRepository.save(any(Ride.class))).thenReturn(acceptedRide);

            RideResponse response = rideService.acceptRide(1L, DRIVER_EMAIL, TOKEN);

            assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
            assertThat(response.getDriverId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should throw exception when ride is not in REQUESTED status")
        void shouldThrowWhenRideNotRequested() {
            sampleRide.setStatus(RideStatus.ACCEPTED);
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));

            assertThatThrownBy(() -> rideService.acceptRide(1L, DRIVER_EMAIL, TOKEN))
                    .isInstanceOf(InvalidRideStateException.class)
                    .hasMessageContaining("Only REQUESTED rides can be accepted");
        }

        @Test
        @DisplayName("Should throw exception when driver already has active ride")
        void shouldThrowWhenDriverHasActiveRide() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(DRIVER_EMAIL, TOKEN)).thenReturn(driverUser);
            when(rideRepository.countActiveRidesByDriver(2L)).thenReturn(1L);

            assertThatThrownBy(() -> rideService.acceptRide(1L, DRIVER_EMAIL, TOKEN))
                    .isInstanceOf(InvalidRideStateException.class)
                    .hasMessageContaining("already have an active ride");
        }
    }

    // =============================================
    // Update Ride Status Tests
    // =============================================
    @Nested
    @DisplayName("Update Ride Status Tests")
    class UpdateRideStatusTests {

        @Test
        @DisplayName("Should update status from ACCEPTED to IN_PROGRESS")
        void shouldUpdateToInProgress() {
            sampleRide.setStatus(RideStatus.ACCEPTED);
            sampleRide.setDriverId(2L);

            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(DRIVER_EMAIL, TOKEN)).thenReturn(driverUser);

            Ride updatedRide = Ride.builder()
                    .rideId(1L).userId(1L).driverId(2L)
                    .pickupLocation("MG Road").dropoffLocation("T Nagar")
                    .fare(new BigDecimal("170.00")).status(RideStatus.IN_PROGRESS)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(rideRepository.save(any(Ride.class))).thenReturn(updatedRide);

            UpdateRideStatusRequest request = new UpdateRideStatusRequest(RideStatus.IN_PROGRESS);
            RideResponse response = rideService.updateRideStatus(1L, request, DRIVER_EMAIL, TOKEN);

            assertThat(response.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void shouldThrowForInvalidTransition() {
            sampleRide.setStatus(RideStatus.REQUESTED);
            sampleRide.setDriverId(2L);

            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(DRIVER_EMAIL, TOKEN)).thenReturn(driverUser);

            UpdateRideStatusRequest request = new UpdateRideStatusRequest(RideStatus.COMPLETED);

            assertThatThrownBy(() -> rideService.updateRideStatus(1L, request, DRIVER_EMAIL, TOKEN))
                    .isInstanceOf(InvalidRideStateException.class)
                    .hasMessageContaining("Cannot transition from REQUESTED to COMPLETED");
        }

        @Test
        @DisplayName("Should throw exception when driver is not assigned to ride")
        void shouldThrowWhenDriverNotAssigned() {
            sampleRide.setStatus(RideStatus.ACCEPTED);
            sampleRide.setDriverId(99L); // different driver

            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(DRIVER_EMAIL, TOKEN)).thenReturn(driverUser);

            UpdateRideStatusRequest request = new UpdateRideStatusRequest(RideStatus.IN_PROGRESS);

            assertThatThrownBy(() -> rideService.updateRideStatus(1L, request, DRIVER_EMAIL, TOKEN))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("not assigned to this ride");
        }
    }

    // =============================================
    // Cancel Ride Tests
    // =============================================
    @Nested
    @DisplayName("Cancel Ride Tests")
    class CancelRideTests {

        @Test
        @DisplayName("Should cancel a ride successfully")
        void shouldCancelRideSuccessfully() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(CUSTOMER_EMAIL, TOKEN)).thenReturn(customerUser);

            Ride cancelledRide = Ride.builder()
                    .rideId(1L).userId(1L)
                    .pickupLocation("MG Road").dropoffLocation("T Nagar")
                    .fare(new BigDecimal("170.00")).status(RideStatus.CANCELLED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(rideRepository.save(any(Ride.class))).thenReturn(cancelledRide);

            RideResponse response = rideService.cancelRide(1L, CUSTOMER_EMAIL, TOKEN);

            assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw exception when cancelling another user's ride")
        void shouldThrowWhenCancellingOthersRide() {
            sampleRide.setUserId(99L); // different user

            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(CUSTOMER_EMAIL, TOKEN)).thenReturn(customerUser);

            assertThatThrownBy(() -> rideService.cancelRide(1L, CUSTOMER_EMAIL, TOKEN))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("only cancel your own rides");
        }

        @Test
        @DisplayName("Should throw exception when cancelling completed ride")
        void shouldThrowWhenCancellingCompletedRide() {
            sampleRide.setStatus(RideStatus.COMPLETED);

            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserByEmail(CUSTOMER_EMAIL, TOKEN)).thenReturn(customerUser);

            assertThatThrownBy(() -> rideService.cancelRide(1L, CUSTOMER_EMAIL, TOKEN))
                    .isInstanceOf(InvalidRideStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }
    }

    // =============================================
    // Get Ride Tests
    // =============================================
    @Nested
    @DisplayName("Get Ride Tests")
    class GetRideTests {

        @Test
        @DisplayName("Should get ride by ID")
        void shouldGetRideById() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));

            RideResponse response = rideService.getRideById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getRideId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void shouldThrowWhenRideNotFound() {
            when(rideRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rideService.getRideById(999L))
                    .isInstanceOf(RideNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 999");
        }

        @Test
        @DisplayName("Should get rides by user")
        void shouldGetRidesByUser() {
            when(rideRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(sampleRide));

            List<RideResponse> rides = rideService.getRidesByUser(1L);

            assertThat(rides).hasSize(1);
            assertThat(rides.get(0).getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should get available rides")
        void shouldGetAvailableRides() {
            when(rideRepository.findByStatus(RideStatus.REQUESTED))
                    .thenReturn(List.of(sampleRide));

            List<RideResponse> rides = rideService.getAvailableRides();

            assertThat(rides).hasSize(1);
            assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.REQUESTED);
        }
    }

    // =============================================
    // Assign Driver Tests
    // =============================================
    @Nested
    @DisplayName("Assign Driver Tests")
    class AssignDriverTests {

        @Test
        @DisplayName("Should throw when assigning non-driver user")
        void shouldThrowWhenAssigningNonDriver() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(sampleRide));
            when(authServiceClient.getUserById(1L, TOKEN)).thenReturn(customerUser); // CUSTOMER role

            AssignDriverRequest request = new AssignDriverRequest(1L);

            assertThatThrownBy(() -> rideService.assignDriver(1L, request, TOKEN))
                    .isInstanceOf(InvalidRideStateException.class)
                    .hasMessageContaining("is not a DRIVER");
        }
    }
}