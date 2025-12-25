package com.radioacademy.backend.dto;

import java.util.UUID;

public record StudentCourseDTO(
                UUID id,
                String title,
                String description,
                Integer progress) {

}
