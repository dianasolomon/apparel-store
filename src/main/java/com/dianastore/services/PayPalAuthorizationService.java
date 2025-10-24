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
public class PayPalAuthorizationService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;


    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepo;

    public Map<String, Object> authorizePayment(String orderId) {
        String url = config.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/authorize";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // ✅ Step 1: Call PayPal API
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> response = responseEntity.getBody();

        // ✅ Step 2: Extract PayPal Authorization ID
        String authorizationId = null;
        try {
            var purchaseUnits = (List<Map<String, Object>>) response.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                var payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
                if (payments != null) {
                    var authorizations = (List<Map<String, Object>>) payments.get("authorizations");
                    if (authorizations != null && !authorizations.isEmpty()) {
                        authorizationId = (String) authorizations.get(0).get("id");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse authorization ID from PayPal response", e);
        }

        // ✅ Step 3: Find parent PaymentTransaction
        PaymentTransaction paymentTransaction = paymentTransactionRepo.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("PaymentTransaction not found for orderId: " + orderId));

        // ✅ Step 4: Create new PaymentEntry for AUTHORIZATION event
        PaymentEntry paymentEntry = PaymentEntry.builder()
                .paypalOrderId(orderId)
                .paypalTransactionId(authorizationId)
                .eventType("AUTHORIZED")
                .status("AUTHORIZED")
                .amount(paymentTransaction.getTotalAmount())
                .currency(paymentTransaction.getCurrency())
                .createdAt(LocalDateTime.now())
                .paymentTransaction(paymentTransaction)
                .build();

        // ✅ Step 5: Attach entry to parent transaction
        paymentTransaction.getPaymentEntries().add(paymentEntry);
        paymentTransaction.setStatus("AUTHORIZED"); // update parent status

        // ✅ Step 6: Save parent (cascades to child)
        paymentTransactionRepo.save(paymentTransaction);

        return response;
    }
}
