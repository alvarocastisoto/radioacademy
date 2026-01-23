package com.radioacademy.backend.dto.exams;

public record QuizResultDTO(
        double score,
        boolean passed) {
}