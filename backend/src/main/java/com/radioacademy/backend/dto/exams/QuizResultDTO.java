package com.radioacademy.backend.dto.exams;

import java.util.Map;
import java.util.UUID;

public record QuizResultDTO(
                double score,
                boolean passed,
                
                Map<UUID, Boolean> questionResults,
                
                Map<UUID, UUID> correctOptions) {
}