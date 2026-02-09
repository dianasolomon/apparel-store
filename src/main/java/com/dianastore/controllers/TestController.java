package com.dianastore.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/testing")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public OK";
    }

    @GetMapping("/api/payment")
    @PreAuthorize("hasAuthority('SCOPE_apparel-store')")
    public String paymentEndpoint(Authentication auth) {
        return "✅ Payment access granted for user: " + auth.getName();
    }
}

