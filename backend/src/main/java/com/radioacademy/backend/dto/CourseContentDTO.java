package com.radioacademy.backend.dto;

import java.util.UUID;
import java.util.List;

public record CourseContentDTO(
        UUID id,
        String title,
        String description,
        List<ModuleDTO> sections,
        String coverImage, // 👈 Lista anidada de secciones
        Integer progress // Progreso global
) {
}