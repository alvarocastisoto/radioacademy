package com.radioacademy.backend.dto;

import java.util.UUID;
import java.util.List;

public record ModuleDTO(
                UUID id,
                String title,
                Integer orderIndex,
                List<LessonDTO> lessons // 👈 Lista anidada
) {
}