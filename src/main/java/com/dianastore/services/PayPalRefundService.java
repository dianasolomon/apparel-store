package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PayPalRefundService {

    @Autowired
    private PayPalAuthService authService;

    @Autowired
    private PayPalConfig config;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Refunds a payment — full or partial.
     * @param captureId PayPal Capture ID (from the capture response)
     * @param amount Optional refund amount (if null or empty, full refund is processed)
     * @return PayPal refund response
     */
    public Map<String, Object> refundPayment(String captureId, String amount) {
        String url = config.getBaseUrl() + "/v2/payments/captures/" + captureId + "/refund";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body;

        // 🔹 If amount is provided → partial refund
        if (amount != null && !amount.isBlank()) {
            body = String.format("""
                {
                  "amount": {
                    "value": "%s",
                    "currency_code": "USD"
                  }
                }
                """, amount);
        }
        // 🔹 Otherwise → full refund
        else {
            body = "{}";
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, entity, Map.class).getBody();
    }
}
