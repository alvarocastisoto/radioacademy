package com.radioacademy.backend.dto.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private String name; 
    private Long amount; 
}