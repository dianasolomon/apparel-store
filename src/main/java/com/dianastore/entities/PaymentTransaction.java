package com.dianastore.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private String internalOrderId;
    @Column(unique = true)
    private String paypalOrderId;

    private String transactionId;
    private String paymentMethod;
    private String currency;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    // ✅ One transaction can have many payment entries
    @OneToMany(mappedBy = "paymentTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<PaymentEntry> paymentEntries = new ArrayList<>();

    @OneToOne(mappedBy = "paymentTransaction")
    @JsonBackReference // ✅ stop recursion back to Cart
    private Cart cart;



    @OneToMany(mappedBy = "paymentTransaction", cascade = CascadeType.ALL)
    @JsonManagedReference
    @Builder.Default
    private List<Order> orders = new ArrayList<>();



}
