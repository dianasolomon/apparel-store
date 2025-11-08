package com.dianastore.services;

import com.dianastore.entities.Cart;
import com.dianastore.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private CartService cartService;

    @Autowired
    private PayPalOrderService payPalOrderService;

    @Autowired
    private PayPalAuthorizationService payPalAuthorizationService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public String createPaymentForCart(Long cartId) {
        Cart cart = cartService.getCart(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Map<String, Object> response = payPalOrderService.createOrder(
                String.format("%.2f", cart.getTotalAmount()), cart);

        // Extract approval link
        List<Map<String, Object>> links = (List<Map<String, Object>>) response.get("links");
        return links.stream()
                .filter(link -> "approve".equals(link.get("rel")))
                .map(link -> (String) link.get("href"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No approval link found in PayPal response"));
    }

    public Map<String, Object> handlePaymentSuccess(Long cartId, String paypalOrderId) {
        payPalAuthorizationService.authorizePayment(paypalOrderId);

        var transaction = paymentTransactionRepository.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setStatus("AUTHORIZED");
        paymentTransactionRepository.save(transaction);

        return Map.of(
                "message", "Payment successful",
                "cartId", cartId,
                "paypalOrderId", paypalOrderId,
                "transactionStatus", transaction.getStatus()
        );
    }
}

