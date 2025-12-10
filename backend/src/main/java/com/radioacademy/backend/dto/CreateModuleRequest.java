package com.radioacademy.backend.dto;

import java.util.UUID;

public record CreateModuleRequest(
        String title,
        Integer orderIndex,
        UUID courseId)// A que curso pertenece
{
}
