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
public class PayPalRefundService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepo;

    /**
     * Refunds a payment (full or partial).
     * @param orderId Internal order ID (links to PaymentTransaction)
     * @param amount  Optional refund amount (null = full refund)
     * @return PayPal API response map
     */
    public Map<String, Object> refundPayment(String orderId, String amount) {

        // ✅ Step 1: Find the PaymentTransaction (parent)
        PaymentTransaction paymentTransaction = paymentTransactionRepo.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("PaymentTransaction not found for orderId: " + orderId));

        // ✅ Step 2: Get the latest CAPTURED PaymentEntry
        PaymentEntry capturedEntry = paymentTransaction.getPaymentEntries().stream()
                .filter(e -> "CAPTURED".equalsIgnoreCase(e.getEventType()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RuntimeException("No CAPTURED entry found for orderId: " + orderId));

        String captureId = capturedEntry.getPaypalOrderId();
        if (captureId == null || captureId.isBlank()) {
            throw new RuntimeException("No valid captureId found for orderId: " + orderId);
        }

        // ✅ Step 3: Prepare PayPal refund API URL
        String url = config.getBaseUrl() + "/v2/payments/captures/" + captureId + "/refund";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ Step 4: Prepare request body
        String body = (amount != null && !amount.isBlank())
                ? String.format("""
                    {
                      "amount": {
                        "value": "%s",
                        "currency_code": "%s"
                      }
                    }
                """, amount, capturedEntry.getCurrency())
                : "{}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // ✅ Step 5: Call PayPal refund API
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> response = responseEntity.getBody();

        if (response == null) {
            throw new RuntimeException("Empty response from PayPal refund API");
        }

        String refundId = (String) response.get("id");
        String refundStatus = (String) response.get("status");

        // ✅ Step 6: Create new PaymentEntry for REFUNDED event
        PaymentEntry refundEntry = PaymentEntry.builder()
                .paypalOrderId(orderId)
                .eventType("REFUNDED")
                .status(refundStatus != null ? refundStatus : "REFUNDED")
                .amount(amount != null ? Double.parseDouble(amount) : capturedEntry.getAmount())
                .currency(capturedEntry.getCurrency())
                .createdAt(LocalDateTime.now())
                .paymentTransaction(paymentTransaction)
                .build();

        // ✅ Step 7: Attach to parent and update status
        paymentTransaction.getPaymentEntries().add(refundEntry);
        paymentTransaction.setStatus(refundEntry.getStatus());

        // ✅ Step 8: Save parent (cascade saves child)
        paymentTransactionRepo.save(paymentTransaction);

        return response;
    }
}
