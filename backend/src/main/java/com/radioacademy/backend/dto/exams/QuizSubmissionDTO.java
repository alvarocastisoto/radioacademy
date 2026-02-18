package com.radioacademy.backend.dto.exams;

import java.util.Map;
import java.util.UUID;


public record QuizSubmissionDTO(
                UUID quizId,
                Map<UUID, UUID> answers) {
}