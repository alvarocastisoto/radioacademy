package com.radioacademy.backend.dto.exams;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuizDTO(
                UUID id,
                @NotBlank String title,
                @NotNull UUID moduleId,
                @NotNull List<QuestionDTO> questions) {

}
