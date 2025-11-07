package com.dianastore.controllers;

import com.dianastore.entities.Order;
import com.dianastore.entities.OrderStatus;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.OrderRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import com.dianastore.services.OrderService;
import com.dianastore.services.PayPalAuthorizationService;
import com.dianastore.services.PayPalCaptureService;
import com.dianastore.services.PayPalRefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;
@RestController
@RequestMapping("/{market}/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PayPalCaptureService payPalCaptureService;
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private PayPalRefundService payPalRefundService;
    @Autowired
    private PayPalAuthorizationService payPalAuthorizationService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        PaymentTransaction txn = order.getPaymentTransaction();
        if (txn == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No PaymentTransaction linked to this order"));
        }
        Map<String, Object> result = null;
        switch (status.toUpperCase()) {
            case "SHIPPED":
                // ✅ Capture payment before shipping
                if ("AUTHORIZED".equalsIgnoreCase(txn.getStatus())) {
                    result = payPalCaptureService.capturePayment(txn.getPaypalOrderId());
                    order.setStatus(OrderStatus.SHIPPED);
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Cannot capture payment. Invalid transaction state.",
                            "status", txn.getStatus()
                    ));
                }
                break;

            case "CANCELLED":
                if ("AUTHORIZED".equalsIgnoreCase(txn.getStatus())) {
                    result = payPalAuthorizationService.voidAuthorization(txn.getPaypalOrderId());
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Cannot cancel. Transaction already captured or refunded.",
                            "status", txn.getStatus()
                    ));
                }

                break;

            case "REFUNDED":
                // ✅ If captured, refund payment
                if ("CAPTURED".equalsIgnoreCase(txn.getStatus())) {
                    result = payPalRefundService.refundPayment(
                            txn.getPaypalOrderId(),
                            String.valueOf(txn.getTotalAmount())
                    );
                    order.setStatus(OrderStatus.REFUNDED);
                }
                else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Cannot refund before capture.",
                            "status", txn.getStatus()
                    ));
                }
            break;


            default:
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status: " + status));
        }

        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "Order status updated", "status", order.getStatus()));
    }


}
