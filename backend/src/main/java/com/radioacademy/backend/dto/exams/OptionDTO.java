package com.radioacademy.backend.dto.exams;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionDTO(

        @NotBlank String text,
        @NotNull boolean isCorrect) {
}
