package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import com.dianastore.entities.PaymentEntry;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PayPalCaptureService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepo;

    public Map<String, Object> capturePayment(String orderId) {
        // ✅ Step 1: Find the parent PaymentTransaction
        PaymentTransaction paymentTransaction = paymentTransactionRepo.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("PaymentTransaction not found for orderId: " + orderId));

        // ✅ Step 2: Find the most recent AUTHORIZED entry
        PaymentEntry authorizedEntry = paymentTransaction.getPaymentEntries().stream()
                .filter(e -> "AUTHORIZED".equalsIgnoreCase(e.getEventType()))
                .reduce((first, second) -> second) // get last authorized entry
                .orElseThrow(() -> new RuntimeException("No AUTHORIZED entry found for orderId: " + orderId));

        String authorizationId = authorizedEntry.getPaypalTransactionId();
        String url = config.getBaseUrl() + "/v2/payments/authorizations/" + authorizationId + "/capture";

        // ✅ Step 3: Prepare headers for API call
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // ✅ Step 4: Call PayPal Capture API
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> response = responseEntity.getBody();

        if (response == null) {
            throw new RuntimeException("Empty response from PayPal Capture API");
        }

        // ✅ Step 5: Extract capture ID
        String captureId = (String) response.get("id");
        if (captureId == null) {
            throw new RuntimeException("Capture ID missing in PayPal response");
        }

        // ✅ Step 6: Create a new PaymentEntry for CAPTURED event
        PaymentEntry capturedEntry = PaymentEntry.builder()
                .paypalOrderId(orderId)
                .eventType("CAPTURED")
                .status("COMPLETED")
                .amount(authorizedEntry.getAmount())
                .currency(authorizedEntry.getCurrency())
                .createdAt(LocalDateTime.now())
                .paymentTransaction(paymentTransaction)
                .build();

        // ✅ Step 7: Attach new entry to transaction and update status
        paymentTransaction.getPaymentEntries().add(capturedEntry);
        paymentTransaction.setStatus("COMPLETED");

        // ✅ Step 8: Save parent transaction (cascades to entries)
        paymentTransactionRepo.save(paymentTransaction);

        return response;
    }
}
