package com.debugathon.problem1.orchestrator.repository;

import com.debugathon.problem1.orchestrator.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
}
