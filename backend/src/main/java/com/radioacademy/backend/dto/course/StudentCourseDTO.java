package com.radioacademy.backend.dto.course;

import java.util.UUID;

public record StudentCourseDTO(
        UUID id,
        String title,
        String description,
        Integer progress) {

}
