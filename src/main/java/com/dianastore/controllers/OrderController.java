package com.dianastore.controllers;

import com.dianastore.dto.UpdateOrderStatusRequest;
import com.dianastore.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/{market}/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,@RequestBody UpdateOrderStatusRequest request) {

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }
        return orderService.updateOrderStatus(orderId, request.getStatus());
    }


}
