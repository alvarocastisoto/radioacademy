package com.radioacademy.backend.dto; // O donde lo tengas

import com.radioacademy.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user; // <--- AÑADIR ESTE CAMPO
}