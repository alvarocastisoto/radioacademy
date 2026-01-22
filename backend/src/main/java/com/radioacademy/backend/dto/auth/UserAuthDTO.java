package com.radioacademy.backend.dto.auth;

import com.radioacademy.backend.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserAuthDTO(
                UUID id,
                String name,
                String surname,
                String email,
                Role role,
                String avatar,
                LocalDateTime createdAt) {
}
