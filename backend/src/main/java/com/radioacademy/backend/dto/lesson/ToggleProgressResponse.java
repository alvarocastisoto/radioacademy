package com.radioacademy.backend.dto.lesson;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Respuesta al marcar/desmarcar
public record ToggleProgressResponse(
                @NotNull(message = "El ID de la lección no puede estar vacío") UUID lessonId,
                @NotNull(message = "El estado de completado no puede estar vacío") boolean isCompleted,
                @NotBlank(message = "El mensaje no puede estar vacío") String message) {
}