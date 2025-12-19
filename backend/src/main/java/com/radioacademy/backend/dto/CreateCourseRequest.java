package com.radioacademy.backend.dto;

import java.math.BigDecimal;

public record CreateCourseRequest(
                String title,
                String description,
                BigDecimal price,
                Integer hours,
                String level) {
}