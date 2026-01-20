package com.radioacademy.backend.dto;

import java.util.UUID;

public record LessonDTO(
        UUID id,
        String title,
        String videoUrl,
        String pdfUrl, // La URL del video (YouTube/Vimeo/S3)
        Integer duration, // En segundos o minutos
        boolean completed, // Para saber si ponerle el check ✅
        UUID quizId) {
}