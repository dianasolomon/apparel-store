package com.dianastore.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class TokenController {

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    @Value("${azure.tenant-id}")
    private String tenantId;

    // hard‑coded for demo – move to properties/env in real app
    private final String username = "test-user@dianapsolomongmail.onmicrosoft.com";
    private final String password = "DianaSolomon@2004";
    private final String scope = "api://d7bfeb72-41e3-4c12-936d-96022c8c5832/apparel-store";

    @GetMapping("/get-user-token")
    public String getUserToken() {
        RestTemplate rest = new RestTemplate();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        // scope can include openid/profile if needed
        body.add("scope", scope);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        String tokenUrl = "https://login.microsoftonline.com/" +
                tenantId + "/oauth2/v2.0/token";

        ResponseEntity<Map> response =
                rest.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }
}
