package com.debugathon.problem1.orchestrator.repository;

import com.debugathon.problem1.orchestrator.domain.BookingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAttemptRepository extends JpaRepository<BookingAttempt, Long> {
}
