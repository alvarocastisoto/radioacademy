package com.radioacademy.backend.dto;

import java.util.UUID;

public record LessonResponse(
        UUID id,
        String title,
        String videoUrl,
        String pdfUrl,
        Integer orderIndex,
        UUID moduleId // 👈 Solo devolvemos el ID del padre, no el objeto entero
) {
}