package com.dianastore.controllers;

import com.dianastore.entities.Cart;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.PaymentTransactionRepository;
import com.dianastore.services.CartService;
import com.dianastore.services.PayPalAuthorizationService;
import com.dianastore.services.PayPalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/{market}/carts")
public class PaymentController {
    @Autowired
    private CartService cartService;

    @Autowired
    private PayPalOrderService payPalOrderService;

    @Autowired
    private PayPalAuthorizationService payPalAuthorizationService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @PostMapping("/{id}/payment")
    public ResponseEntity<String> makePayment(@PathVariable Long id) {
        Cart cart = cartService.getCart(id)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Call PayPal API
        Map<String, Object> response = payPalOrderService.createOrder(String.format("%.2f", cart.getTotalAmount()),cart);


        // Extract approval link
        String approvalLink = null;
        Object linksObj = response.get("links");
        if (linksObj instanceof List) {
            List<Map<String, Object>> links = (List<Map<String, Object>>) linksObj;
            for (Map<String, Object> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    approvalLink = (String) link.get("href");
                    break;
                }
            }
        }

        if (approvalLink == null) {
            return ResponseEntity.badRequest().body("No approval link found in PayPal response");
        }

        return ResponseEntity.ok(approvalLink);
    }

    @GetMapping("/{cartId}/payment/success")
    public ResponseEntity<?> handlePayPalSuccess(
            @PathVariable String market,
            @PathVariable Long cartId,
            @RequestParam("token") String paypalOrderId) {
        try {
            Map<String, Object> result = payPalAuthorizationService.authorizePayment(paypalOrderId);

            PaymentTransaction transaction = paymentTransactionRepository
                    .findByPaypalOrderId(paypalOrderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            transaction.setStatus("AUTHORIZED");
            paymentTransactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment successful",
                    "cartId", cartId,
                    "paypalOrderId", paypalOrderId,
                    "transactionStatus", transaction.getStatus()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Payment authorization failed", "details", e.getMessage())
            );
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
