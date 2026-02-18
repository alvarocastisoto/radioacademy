package com.radioacademy.backend.dto.student;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.UUID;

public record UserProfileDTO(

                UUID id, 

                @NotBlank(message = "El nombre no puede estar vacío") String name,

                @NotBlank(message = "El apellido no puede estar vacío") String surname,

                @NotBlank(message = "El email es obligatorio") @Email(message = "El formato del email no es válido") String email,

                @NotBlank(message = "El teléfono es obligatorio") @Size(min = 9, max = 9, message = "El teléfono debe tener 9 caracteres") String phone,

                
                String currentPassword,

                
                String newPassword,

                String avatar) {
}