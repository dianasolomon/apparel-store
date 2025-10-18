package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import com.dianastore.entities.PaymentEntry;
import com.dianastore.entities.PaymentTransaction;
import com.dianastore.repository.PaymentEntryRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PayPalRefundService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentEntryRepository entryRepo;

    @Autowired
    private PaymentTransactionRepository transactionRepo;

    /**
     * Refunds a payment — full or partial.
     * @param orderId Order ID to identify which capture to refund
     * @param amount Optional refund amount (if null or empty → full refund)
     * @return PayPal refund response
     */
    public Map<String, Object> refundPayment(String orderId, String amount) {
        // ✅ Step 1: Find PaymentEntry
        Optional<PaymentEntry> entryOpt = entryRepo.findByOrderId(orderId);
        if (entryOpt.isEmpty()) {
            throw new RuntimeException("No payment entry found for orderId: " + orderId);
        }

        PaymentEntry entry = entryOpt.get();

        // ✅ Step 2: Get the latest CAPTURED transaction
        PaymentTransaction capturedTx = entry.getTransactions()
                .stream()
                .filter(tx -> "CAPTURED".equalsIgnoreCase(tx.getEventType()))
                .reduce((first, second) -> second) // get last captured
                .orElseThrow(() -> new RuntimeException("No captured transaction found for orderId: " + orderId));

        String captureId = capturedTx.getTransactionId();
        if (captureId == null || captureId.isEmpty()) {
            throw new RuntimeException("No captureId found for orderId: " + orderId);
        }

        // ✅ Step 3: Prepare PayPal refund API URL
        String url = config.getBaseUrl() + "/v2/payments/captures/" + captureId + "/refund";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = (amount != null && !amount.isBlank())
                ? String.format("""
                    {
                      "amount": {
                        "value": "%s",
                        "currency_code": "USD"
                      }
                    }
                """, amount)
                : "{}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // ✅ Step 4: Send refund request to PayPal
        Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();

        // ✅ Step 5: Extract refund status
        String status = (String) response.get("status");

        // ✅ Step 6: Save refund transaction in DB
        PaymentTransaction refundTx = PaymentTransaction.builder()
                .transactionId((String) response.get("id")) // PayPal refund ID
                .eventType("REFUNDED")
                .status(status != null ? status : "REFUNDED")
                .amount(capturedTx.getAmount())
                .currency(capturedTx.getCurrency())
                .paymentEntry(entry)
                .build();

        entry.getTransactions().add(refundTx);
        entry.setStatus(refundTx.getStatus());

        entryRepo.save(entry); // cascade saves new transaction too

        return response;
    }
}
