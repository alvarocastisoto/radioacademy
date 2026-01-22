package com.radioacademy.backend.dto.auth;

public record AuthResponseDTO(
                String token,
                UserAuthDTO user) {
}
