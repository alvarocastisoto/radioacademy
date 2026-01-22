package com.radioacademy.backend.dto.course;

import java.math.BigDecimal;
import java.util.List;
import com.radioacademy.backend.dto.lesson.CreateLessonRequest;

public record CreateCourseRequest(
        String title,
        String description,
        BigDecimal price,
        Integer hours,
        List<CreateLessonRequest> lesson,
        String level,
        String coverImage) {
}