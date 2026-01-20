package com.radioacademy.backend.dto.exams;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionDTO(
                UUID id,
                @NotBlank String text,
                @NotNull boolean isCorrect) {
}
