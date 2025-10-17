package com.dianastore.services;

import com.dianastore.config.PayPalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class PayPalCaptureService {

    @Autowired
    private PayPalAuthService authService;
    @Autowired
    private PayPalConfig config;
    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> capturePayment(String authorizationId) {
        String url = config.getBaseUrl() + "/v2/payments/authorizations/" + authorizationId + "/capture";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        return restTemplate.postForEntity(url, entity, Map.class).getBody();
    }
}

