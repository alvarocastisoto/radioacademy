package com.radioacademy.backend.dto.lesson;

import java.util.UUID;

public record CreateLessonRequest(
                String title,
                String videoUrl,
                String pdfUrl,
                Integer orderIndex,
                UUID moduleId // A que módulo pertenece
) {
}
