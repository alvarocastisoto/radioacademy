package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Obtener todos (GET /api/users)
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Crear usuario (POST /api/users)
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return new ResponseEntity<>(userService.createUser(user), HttpStatus.CREATED);
    }

    // Obtener mi perfil
    @GetMapping("/profile")
    public ResponseEntity<User> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getMyProfile(auth.getName()));
    }

    // Actualizar perfil
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @Valid @RequestBody UserProfileDTO profileData,
            Authentication auth) {

        // El servicio maneja toda la lógica (passwords, emails, ficheros)
        return ResponseEntity.ok(userService.updateProfile(auth.getName(), profileData));
    }
}