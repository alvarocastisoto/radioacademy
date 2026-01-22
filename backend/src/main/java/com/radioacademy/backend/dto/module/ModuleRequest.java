package com.radioacademy.backend.dto.module;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModuleRequest(
                @NotNull(message = "El ID del módulo es obligatorio") UUID id,
                @NotBlank(message = "El título del módulo es obligatorio") String title,
                @NotNull(message = "El índice de orden es obligatorio") Integer orderIndex) {
}