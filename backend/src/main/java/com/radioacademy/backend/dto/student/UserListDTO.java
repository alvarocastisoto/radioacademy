package com.radioacademy.backend.dto.student;

import java.util.UUID;


public record UserListDTO(
                UUID id,
                String fullName,
                String email,
                String dni,
                String role,
                boolean active) {
}