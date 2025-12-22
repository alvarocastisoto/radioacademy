package com.radioacademy.backend.dto;

import java.util.UUID;

public record CourseDropdownDTO(
        UUID id, // O UUID si tu curso usa UUID
        String title) {
}