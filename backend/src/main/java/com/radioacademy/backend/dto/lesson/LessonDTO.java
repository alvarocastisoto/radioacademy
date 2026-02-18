package com.radioacademy.backend.dto.lesson;

import java.util.UUID;

public record LessonDTO(
                UUID id,
                String title,
                String videoUrl,
                String pdfUrl, 
                Integer duration, 
                boolean completed, 
                UUID quizId) {
}