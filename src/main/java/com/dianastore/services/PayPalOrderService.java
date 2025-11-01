package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import com.dianastore.entities.Cart;
import com.dianastore.entities.PaymentEntry;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.CartRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PayPalOrderService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepo;

    @Autowired
    private CartRepository cartRepository;

    public Map<String, Object> createOrder(String amount, Cart cart) {
        String url = config.getBaseUrl() + "/v2/checkout/orders";

        Map<String, Object> payload = Map.of(
                "intent", "AUTHORIZE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of("currency_code", "USD", "value", amount)
                )),
                "application_context", Map.of(
                        "return_url", "http://localhost:8080/" + cart.getMarket() + "/carts/" + cart.getId() + "/payment/success",
                        "cancel_url", "http://localhost:8080/" + cart.getMarket() + "/carts/" + cart.getId() + "/payment/cancel"
                )
        );


        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        // ✅ Call PayPal API to create order
        Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();

        // ✅ Extract PayPal order ID
        String paypalOrderId = (String) response.get("id");

        PaymentTransaction txn = PaymentTransaction.builder()
                .internalOrderId("INT-" + System.currentTimeMillis())
                .paypalOrderId(paypalOrderId)
                .transactionId("TXN-" + System.currentTimeMillis())
                .paymentMethod("PAYPAL")
                .currency("USD")
                .totalAmount(Double.valueOf(amount))
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .cart(cart)
                .build();


        // ✅ Create initial PaymentEntry (child)
        PaymentEntry entry = PaymentEntry.builder()
                .paypalOrderId(paypalOrderId)
                .eventType("CREATED")
                .status("CREATED")
                .amount(Double.parseDouble(amount))
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .paymentTransaction(txn) // link to parent
                .build();

        // ✅ Link child to parent
        txn.getPaymentEntries().add(entry);
        cart.setPaymentTransaction(txn);
        paymentTransactionRepo.save(txn);
        cartRepository.save(cart);
        return response;
    }
}
