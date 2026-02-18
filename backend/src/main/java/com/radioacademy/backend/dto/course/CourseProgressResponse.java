package com.radioacademy.backend.dto.course;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;


public record CourseProgressResponse(
                @NotNull(message = "El ID del curso no puede estar vacío") UUID courseId,
                @NotNull(message = "El número de lecciones completadas no puede estar vacío") int completedCount,
                @NotNull(message = "El conjunto de lecciones completadas no puede estar vacío") Set<UUID> completedLessonIds) {
}