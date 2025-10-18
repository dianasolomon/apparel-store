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
import java.util.Optional;

@Service
public class PayPalCaptureService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentEntryRepository paymentEntryRepo;

    public Map<String, Object> capturePayment(String orderId) {
        // ✅ Step 1: Get the PaymentEntry for this orderId
        Optional<PaymentEntry> entryOpt = paymentEntryRepo.findByOrderId(orderId);
        if (entryOpt.isEmpty()) {
            throw new RuntimeException("Payment entry not found for order ID: " + orderId);
        }

        PaymentEntry paymentEntry = entryOpt.get();

        // ✅ Step 2: Get the latest AUTHORIZED transaction
        PaymentTransaction lastTx = paymentEntry.getTransactions()
                .stream()
                .filter(tx -> "AUTHORIZED".equalsIgnoreCase(tx.getEventType()))
                .reduce((first, second) -> second) // get last authorized
                .orElseThrow(() -> new RuntimeException("No authorized transaction found for orderId: " + orderId));

        String authorizationId = lastTx.getTransactionId();
        String url = config.getBaseUrl() + "/v2/payments/authorizations/" + authorizationId + "/capture";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // ✅ Step 3: Capture payment via PayPal API
        Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();

        // ✅ Step 4: Extract capture ID from response
        String captureId = null;
        try {
            captureId = (String) response.get("id");
        } catch (Exception ignored) {}

        // ✅ Step 5: Create new PaymentTransaction for CAPTURED event
        PaymentTransaction capturedTx = PaymentTransaction.builder()
                .transactionId(captureId)
                .eventType("CAPTURED")
                .status("COMPLETED")
                .amount(lastTx.getAmount())
                .currency(lastTx.getCurrency())
                .paymentEntry(paymentEntry)
                .build();

        // ✅ Step 6: Add child to parent + update parent status
        paymentEntry.getTransactions().add(capturedTx);
        paymentEntry.setStatus("COMPLETED");

        // ✅ Step 7: Save parent — cascades to child
        paymentEntryRepo.save(paymentEntry);

        return response;
    }
}
