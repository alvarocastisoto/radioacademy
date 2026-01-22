package com.radioacademy.backend.dto.student;

import java.util.UUID;

// Un objeto simple, sin lógica, solo datos para la tabla
public record UserListDTO(
                UUID id,
                String fullName,
                String email,
                String dni,
                String role,
                boolean active) {
}