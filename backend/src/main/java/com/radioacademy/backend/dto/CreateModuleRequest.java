package com.radioacademy.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateModuleRequest(
                @NotBlank(message = "El título del módulo es obligatorio") String title,
                @NotNull(message = "El índice de orden es obligatorio") Integer orderIndex,
                @NotNull(message = "El ID del curso es obligatorio") UUID courseId)// A que curso pertenece
{
}
