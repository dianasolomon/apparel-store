package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        return restTemplate.postForEntity(url, entity, Map.class).getBody();
    }
}

