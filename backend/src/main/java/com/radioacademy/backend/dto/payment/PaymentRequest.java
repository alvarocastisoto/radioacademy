package com.radioacademy.backend.dto.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private String name; // Nombre del curso
    private Long amount; // Precio en CÉNTIMOS (10€ = 1000)
}