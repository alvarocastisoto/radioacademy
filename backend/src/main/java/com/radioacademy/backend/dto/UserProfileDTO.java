package com.radioacademy.backend.dto;

import java.util.UUID;

public record UserProfileDTO(

        UUID id,
        String name,
        String surname,
        String email,
        String phone,
        String currentPassword,
        String newPassword,
        String avatar

) {

}
