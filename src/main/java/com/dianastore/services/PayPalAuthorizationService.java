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
public class PayPalAuthorizationService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentEntryRepository paymentEntryRepo; // ✅ We now use PaymentEntryRepo

    public Map<String, Object> authorizePayment(String orderId) {
        String url = config.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/authorize";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // ✅ Step 1: Call PayPal API to authorize
        Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();

        // ✅ Step 2: Extract Authorization ID from PayPal response
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
        } catch (Exception ignored) {}

        // ✅ Step 3: Find the corresponding PaymentEntry by orderId
        Optional<PaymentEntry> entryOpt = paymentEntryRepo.findByOrderId(orderId);
        if (entryOpt.isPresent()) {
            PaymentEntry paymentEntry = entryOpt.get();

            // ✅ Step 4: Create new PaymentTransaction for AUTHORIZED event
            PaymentTransaction authorizedTx = PaymentTransaction.builder()
                    .transactionId(authorizationId)
                    .eventType("AUTHORIZED")
                    .status("AUTHORIZED")
                    .amount(paymentEntry.getTransactions().get(0).getAmount()) // same as initial
                    .currency("USD")
                    .paymentEntry(paymentEntry)
                    .build();

            // ✅ Step 5: Add to transaction list and update parent status
            paymentEntry.getTransactions().add(authorizedTx);
            paymentEntry.setStatus("AUTHORIZED");

            // ✅ Step 6: Save parent (cascades to child)
            paymentEntryRepo.save(paymentEntry);
        }

        return response;
    }
}
