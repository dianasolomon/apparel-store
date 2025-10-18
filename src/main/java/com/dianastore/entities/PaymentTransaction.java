package com.dianastore.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;  // PayPal/Razorpay transaction ID
    private String eventType;      // CREATED, AUTHORIZED, CAPTURED, REFUNDED
    private String status;         // SUCCESS, FAILED, PENDING
    private Double amount;         // Transaction amount
    private String currency;       // Currency code, e.g., USD, INR

    // Link back to parent PaymentEntry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_entry_id", nullable = false)
    private PaymentEntry paymentEntry;
}
