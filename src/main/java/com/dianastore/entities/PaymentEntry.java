package com.dianastore.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paypalOrderId;     // PayPal’s own order ID
    private String paypalTransactionId;      // PayPal authorization ID
    private String eventType;         // AUTHORIZED, CAPTURED, REFUNDED
    private String status;            // COMPLETED, FAILED, etc.
    private Double amount;
    private String currency;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    @JsonBackReference
    private PaymentTransaction paymentTransaction;
}
