package com.radioacademy.backend.dto.student;

import java.util.UUID;

public record UserProfileResponseDTO(
        UUID id,
        String name,
        String surname,
        String email,
        String phone,
        String avatar,
        String role) {
}
