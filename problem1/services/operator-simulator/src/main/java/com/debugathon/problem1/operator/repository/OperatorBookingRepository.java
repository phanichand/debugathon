package com.debugathon.problem1.operator.repository;

import com.debugathon.problem1.operator.domain.OperatorBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorBookingRepository extends JpaRepository<OperatorBooking, Long> {
    Optional<OperatorBooking> findByIdempotencyKey(String idempotencyKey);
    Optional<OperatorBooking> findByOperatorBookingReference(String operatorBookingReference);
}
