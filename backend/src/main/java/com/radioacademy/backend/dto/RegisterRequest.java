package com.radioacademy.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    private String surname;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    // (?=.*[\W_]) significa: "Cualquier cosa que no sea una letra o número, o un
    // guion bajo"
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_])(?=\\S+$).{8,}$", message = "La contraseña debe tener min 8 caracteres, mayúscula, minúscula, número y carácter especial")
    private String password;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 9, max = 9, message = "El DNI debe tener 9 caracteres")
    private String dni;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @NotBlank(message = "La región es obligatoria")
    private String region;

    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private boolean termsAccepted;
}