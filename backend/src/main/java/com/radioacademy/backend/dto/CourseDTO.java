package com.radioacademy.backend.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record CourseDTO(

        UUID id,
        String title,
        String description,
        String coverImage,
        BigDecimal price,
        Integer hours,
        boolean isPurchased) {
}