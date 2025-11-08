package com.dianastore.controllers;

import com.dianastore.services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/{market}/carts")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/{id}/payment")
    public ResponseEntity<?> makePayment(@PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cart ID is required"));
            }
            String approvalLink = paymentService.createPaymentForCart(id);
            return ResponseEntity.ok(Map.of("approvalLink", approvalLink));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment initiation failed"));
        }
    }

    @GetMapping("/{cartId}/payment/success")
    public ResponseEntity<?> handlePayPalSuccess(
            @PathVariable String market,
            @PathVariable Long cartId,
            @RequestParam("token") String paypalOrderId) {
        try {
            return ResponseEntity.ok(paymentService.handlePaymentSuccess(cartId, paypalOrderId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Payment authorization failed",
                    "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/{cartId}/payment/cancel")
    public ResponseEntity<?> onPaymentCancel(
            @PathVariable String market,
            @PathVariable Long cartId) {

        return ResponseEntity.ok(Map.of(
                "message", "Payment was cancelled by the user",
                "cartId", cartId
        ));
    }
}
