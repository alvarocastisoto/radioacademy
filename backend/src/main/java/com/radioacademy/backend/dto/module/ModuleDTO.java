package com.radioacademy.backend.dto.module;

import java.util.UUID;

import com.radioacademy.backend.dto.lesson.LessonDTO;

import java.util.List;

public record ModuleDTO(
        UUID id,
        String title,
        Integer orderIndex,
        UUID quizId,
        List<LessonDTO> lessons 

) {
}