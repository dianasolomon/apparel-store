package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import com.dianastore.entities.PaymentEntry;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.PaymentEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private PaymentEntryRepository paymentEntryRepo;

    public Map<String, Object> createOrder(String amount) {
        String url = config.getBaseUrl() + "/v2/checkout/orders";

        Map<String, Object> payload = Map.of(
                "intent", "AUTHORIZE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of("currency_code", "USD", "value", amount)
                )),
                "application_context", Map.of(
                        "return_url", "http://localhost:8080/api/paypal/success",
                        "cancel_url", "http://localhost:8080/api/paypal/cancel"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        // ✅ Call PayPal API to create order
        Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();

        // ✅ Extract PayPal order ID
        String orderId = (String) response.get("id");

        // ✅ Create PaymentEntry (parent)
        PaymentEntry paymentEntry = PaymentEntry.builder()
                .orderId(orderId)
                .paymentMethod("PayPal")
                .status("CREATED")
                .build();

        // ✅ Create initial PaymentTransaction (child)
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(orderId) // initial transaction uses orderId; update later with PayPal transaction ID
                .eventType("CREATED")
                .status("CREATED")
                .amount(Double.parseDouble(amount))
                .currency("USD")
                .paymentEntry(paymentEntry) // link child → parent
                .build();

        // ✅ Add child to parent's transaction list
        paymentEntry.getTransactions().add(transaction);

        // ✅ Save parent — cascades to child automatically
        paymentEntryRepo.save(paymentEntry);

        return response;
    }
}
