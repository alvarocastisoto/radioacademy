package com.radioacademy.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateCourseRequest(
        String title,
        String description,
        BigDecimal price,
        Integer hours,
        List<CreateLessonRequest> lesson,
        String level) {
}