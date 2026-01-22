package com.radioacademy.backend.dto.course;

import java.util.UUID;

import com.radioacademy.backend.dto.module.ModuleDTO;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseContentDTO(
        @NotNull(message = "El ID del curso es obligatorio") UUID id,

        @NotBlank(message = "El título es obligatorio") String title,

        @NotBlank(message = "La descripción es obligatoria") String description,

        @NotNull(message = "Las secciones no pueden ser nulas") // 👈 @NotNull para Listas
        List<ModuleDTO> sections,

        @NotBlank(message = "La imagen de portada es obligatoria") String coverImage,

        @NotNull(message = "El progreso es obligatorio") Integer progress) {
}