package com.dianastore.repository;

import com.dianastore.entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByPaypalOrderId(String paypalOrderId);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);

}

