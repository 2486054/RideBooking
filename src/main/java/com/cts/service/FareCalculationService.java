
package com.cts.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculationService {

    private static final BigDecimal BASE_FARE = new BigDecimal("50.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("12.00");


    public BigDecimal calculateFare(String pickup, String dropoff) {
        // Simulate distance based on string hash difference (placeholder logic)
        int simulatedKm = Math.abs(pickup.hashCode() - dropoff.hashCode()) % 30 + 3;
        BigDecimal distance = new BigDecimal(simulatedKm);
        return BASE_FARE.add(PER_KM_RATE.multiply(distance)).setScale(2, RoundingMode.HALF_UP);
    }
}
