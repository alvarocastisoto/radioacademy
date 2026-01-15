package com.radioacademy.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseDashboardDTO(
                @NotNull(message = "El ID del curso no puede estar vacío") UUID id,

                @NotBlank(message = "El título del curso no puede estar vacío") String title,

                @NotBlank(message = "La descripción del curso no puede estar vacía") String description,

                @NotBlank(message = "La imagen de portada no puede estar vacía") String coverImage,

                @NotNull(message = "El progreso no puede estar vacío") Integer progress

) {

}
