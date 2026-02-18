package com.radioacademy.backend.dto.lesson;

import java.util.UUID;

public record LessonResponse(
                UUID id,
                String title,
                String videoUrl,
                String pdfUrl,
                Integer orderIndex,
                UUID moduleId 
) {
}