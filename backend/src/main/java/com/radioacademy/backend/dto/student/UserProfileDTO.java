package com.radioacademy.backend.dto.student;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.UUID;

public record UserProfileDTO(

        UUID id, // Opcional, ya que lo sacamos del Token

        @NotBlank(message = "El nombre no puede estar vacío") String name,

        @NotBlank(message = "El apellido no puede estar vacío") String surname,

        @NotBlank(message = "El email es obligatorio") @Email(message = "El formato del email no es válido") String email,

        @NotBlank(message = "El teléfono es obligatorio") @Size(min = 9, max = 9, message = "El teléfono debe tener 9 caracteres") String phone,

        // Contraseña actual: Opcional (solo si quiere cambiar la pass)
        String currentPassword,

        // Nueva contraseña: Opcional, PERO si la envía, validamos el patrón
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$", message = "La nueva contraseña debe ser fuerte (Min 8 car, Mayús, Minús, Num, Especial)") String newPassword,

        String avatar) {
}