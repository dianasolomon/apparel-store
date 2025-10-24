package com.dianastore.controllers;

import com.dianastore.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {

    @Autowired private PayPalOrderService orderService;
    @Autowired private PayPalAuthorizationService authorizationService;
    @Autowired private PayPalCaptureService captureService;
    @Autowired private PayPalRefundService refundService;
    // 1️⃣ Create Order
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam String amount) {
        return ResponseEntity.ok(orderService.createOrder(amount));
    }

    // 2️⃣ Authorize Order
    @PostMapping("/authorize/{orderId}")
    public ResponseEntity<?> authorizePayment(@PathVariable String orderId) {
        return ResponseEntity.ok(authorizationService.authorizePayment(orderId));
    }

    // 3️⃣ Capture Authorized Payment
    @PostMapping("/capture-by-order/{orderId}")
    public ResponseEntity<?> captureByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(captureService.capturePayment(orderId));
    }


    // 4️⃣ Refund
    @PostMapping("/refund-by-order/{orderId}")
    public ResponseEntity<?> refundByOrder(@PathVariable String orderId, @RequestParam(required = false) String amount) {
        return ResponseEntity.ok(refundService.refundPayment(orderId, amount));
    }
    // ✅ Handle success redirect from PayPal
    @GetMapping("/success")
    public ResponseEntity<?> onSuccess(@RequestParam("token") String orderId) {
        try {
            // Authorize the payment using the orderId (PayPal token)
            Map<String, Object> result = authorizationService.authorizePayment(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Payment authorization failed: " + e.getMessage());
        }
    }

    // ❌ Handle cancel redirect
    @GetMapping("/cancel")
    public ResponseEntity<?> onCancel() {
        return ResponseEntity.ok(Map.of("message", "Payment was cancelled by the user."));
    }
}
