package com.debugathon.problem1.payment.repository;

import com.debugathon.problem1.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}
