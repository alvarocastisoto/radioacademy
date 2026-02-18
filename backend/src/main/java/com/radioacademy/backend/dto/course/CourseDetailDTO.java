package com.radioacademy.backend.dto.course;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseDetailDTO(
                UUID id,
                String title,
                String description,
                String coverImage,
                BigDecimal price,
                Integer hours,
                boolean isActive 
) {
}