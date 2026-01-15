package com.radioacademy.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseDetailDTO(
        UUID id,
        String title,
        String description,
        String coverImage,
        BigDecimal price,
        Integer hours,
        boolean isActive // Para que el admin sepa si está visible
) {
}