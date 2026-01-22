package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // 1. CHECKOUT
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        UUID courseId = UUID.fromString(request.get("courseId"));

        // El servicio devuelve la URL directa
        String url = paymentService.createCheckoutSession(auth.getName(), courseId);

        return ResponseEntity.ok(Map.of("url", url));
    }

    // 2. CONFIRMAR PAGO
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String sessionId = request.get("session_id");

        // El servicio valida y matrícula, o lanza excepción si falla
        paymentService.confirmPayment(auth.getName(), sessionId);

        return ResponseEntity.ok(Map.of("message", "Pago confirmado y curso activado"));
    }
}