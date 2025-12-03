package com.radioacademy.backend.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record CreateCourseRequest(
        String title,
        String description,
        BigDecimal price,
        Integer hours,
        UUID teacherId // Solo pedimos el ID del profe, no el usuario entero
) {
}