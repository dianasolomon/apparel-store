package com.dianastore.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shopperId;
    private String type; // "SHIPPING" or "BILLING"
    private String street;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
