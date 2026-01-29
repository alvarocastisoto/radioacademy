package com.radioacademy.backend.dto.exams;

import java.util.Map;
import java.util.UUID;

public record QuizResultDTO(
                double score,
                boolean passed,
                // Mapa: ID de Pregunta -> ¿Es Correcta?
                Map<UUID, Boolean> questionResults,
                // Mapa: ID de Pregunta -> ID de la Opción Correcta (Para mostrarla si falló)
                Map<UUID, UUID> correctOptions) {
}