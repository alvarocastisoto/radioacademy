package com.radioacademy.backend.dto;

import java.util.UUID;

public record CourseDashboardDTO(
                UUID id,
                String title,
                String description,
                String imageUrl,
                Integer progress

) {

}
