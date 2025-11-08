package com.dianastore.entities;

public enum OrderStatus {
    PENDING,
    AUTHORIZED,// just created
    PAID,          // payment captured
    SHIPPED,       // items shipped
    DELIVERED,     // order completed
    CANCELLED,
    REFUNDED
}
