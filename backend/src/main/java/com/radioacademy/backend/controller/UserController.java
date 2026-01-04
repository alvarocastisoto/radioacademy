package com.radioacademy.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import com.radioacademy.backend.dto.UserProfileDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Obtener todos los usuarios (GET /api/users)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Crear un nuevo usuario (POST /api/users)
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            User savedUser = userRepository.save(user);
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody UserProfileDTO profileData) {

        System.out.println("🚩 1. INICIO CONTROLADOR (LIMPIO)");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Preparar datos
        String newName = profileData.name() != null ? profileData.name() : user.getName();
        String newSurname = profileData.surname() != null ? profileData.surname() : user.getSurname();
        String newPhone = profileData.phone() != null ? profileData.phone() : user.getPhone();
        String newEmail = user.getEmail();
        String newPassword = user.getPassword();

        // Validaciones
        if (profileData.email() != null && !profileData.email().isEmpty()
                && !profileData.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(profileData.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El email ya existe"));
            }
            newEmail = profileData.email();
        }
        if (profileData.newPassword() != null && !profileData.newPassword().isEmpty()) {
            newPassword = passwordEncoder.encode(profileData.newPassword());
        }

        System.out.println("🚩 2. LLAMANDO A BD (BYPASS)...");

        // ✅ MANTENEMOS ESTO: Es la clave para que no se cuelgue la BD
        userRepository.updateProfileDirectly(
                user.getId(), newName, newSurname, newPhone, newEmail, newPassword);

        System.out.println("🚩 3. BD TERMINADA. RETORNANDO JSON ESTÁNDAR...");

        // ✅ VOLVEMOS A ESTO: Spring gestionará las cabeceras CORS automáticamente
        // sin conflictos.
        return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
    }

    private void updateBasicFields(User user, UserProfileDTO dto) {
        if (dto.name() != null)
            user.setName(dto.name());
        if (dto.surname() != null)
            user.setSurname(dto.surname());
        if (dto.phone() != null)
            user.setPhone(dto.phone());
    }

    @GetMapping("profile")
    public ResponseEntity<User> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping("test-alive")
    public ResponseEntity<String> testAlive() {
        System.out.println("🚩 TEST LLAMADO");
        return ResponseEntity.ok("Estoy vivo");
    }
}