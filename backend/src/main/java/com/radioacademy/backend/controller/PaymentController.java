package com.radioacademy.backend.controller;

import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID courseId = UUID.fromString(request.get("courseId"));

        
        String url = paymentService.createCheckoutSession(userDetails, courseId);

        return ResponseEntity.ok(Map.of("url", url));
    }

    
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String sessionId = request.get("session_id");

        
        paymentService.confirmPayment(userDetails, sessionId);

        return ResponseEntity.ok(Map.of("message", "Pago confirmado y curso activado"));
    }
}