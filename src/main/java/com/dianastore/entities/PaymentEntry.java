package com.dianastore.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, unique = true)
    private String orderId;   // This links to the Order table (one order = one payment entry)

    private String paymentMethod;  // PayPal, Razorpay, etc.
    private String status;         // The current payment status (e.g., COMPLETED, PENDING)

    @OneToMany(mappedBy = "paymentEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentTransaction> transactions = new ArrayList<>();

}
