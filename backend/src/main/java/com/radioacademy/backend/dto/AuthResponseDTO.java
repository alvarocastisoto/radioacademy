package com.radioacademy.backend.dto;

public record AuthResponseDTO(
                String token,
                UserAuthDTO user) {
}
