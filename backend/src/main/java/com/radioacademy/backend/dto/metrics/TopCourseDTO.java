package com.radioacademy.backend.dto.metrics;

import java.util.UUID;

public record TopCourseDTO(UUID courseId, long enrollments, String revenue) {

}
