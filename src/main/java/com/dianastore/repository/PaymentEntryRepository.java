package com.dianastore.repository;

import com.dianastore.entities.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Long> {

    // Optional helper method to find a payment entry by PayPal order ID
    Optional<PaymentEntry> findByOrderId(String orderId);
}
