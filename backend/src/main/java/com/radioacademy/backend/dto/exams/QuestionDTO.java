package com.radioacademy.backend.dto.exams;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionDTO(
        @NotBlank String question,
        @NotNull List<OptionDTO> options,
        @NotNull Integer points) {
}
