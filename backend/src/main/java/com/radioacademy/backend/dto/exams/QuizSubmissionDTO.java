package com.radioacademy.backend.dto.exams;

import java.util.Map;
import java.util.UUID;

// Mapa: ID de Pregunta -> ID de la Opción elegida
public record QuizSubmissionDTO(
                UUID quizId,
                Map<UUID, UUID> answers) {
}