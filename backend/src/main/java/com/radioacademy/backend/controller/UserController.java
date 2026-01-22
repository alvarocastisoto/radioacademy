package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 👇 Inyectamos el servicio para poder borrar archivos
    @Autowired
    private StorageService storageService;

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

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@Valid @RequestBody UserProfileDTO profileData) {

        // 1. OBTENER USUARIO AUTENTICADO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean emailChanged = false;

        // 2. VALIDACIÓN Y CAMBIO DE CONTRASEÑA (Prioridad Alta)
        if (profileData.newPassword() != null && !profileData.newPassword().isBlank()) {

            // Validar que envió la actual
            if (profileData.currentPassword() == null || profileData.currentPassword().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debes introducir tu contraseña actual para cambiarla."));
            }

            // Validar que la actual coincida
            if (!passwordEncoder.matches(profileData.currentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "La contraseña actual es incorrecta."));
            }

            // Aplicar cambio
            user.setPassword(passwordEncoder.encode(profileData.newPassword()));
        }

        // 3. VALIDACIÓN Y CAMBIO DE EMAIL
        // Si el email cambia, marcamos la bandera para avisar al frontend
        if (profileData.email() != null && !profileData.email().isBlank()
                && !profileData.email().equals(user.getEmail())) {

            if (userRepository.existsByEmail(profileData.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ese email ya está en uso por otro usuario."));
            }
            user.setEmail(profileData.email());
            emailChanged = true;
        }

        // 4. ACTUALIZACIÓN DE DATOS BÁSICOS
        if (profileData.name() != null)
            user.setName(profileData.name());
        if (profileData.surname() != null)
            user.setSurname(profileData.surname());
        if (profileData.phone() != null)
            user.setPhone(profileData.phone());

        // 5. GESTIÓN DE AVATAR (Garbage Collection)
        if (profileData.avatar() != null && !profileData.avatar().equals(user.getAvatar())) {

            String oldAvatarUrl = user.getAvatar();

            // Asignamos el nuevo
            user.setAvatar(profileData.avatar());

            // Borrado físico del antiguo
            if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
                try {
                    String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                    logger.info("🗑️ Avatar antiguo eliminado: {}", filename);
                } catch (Exception e) {
                    logger.warn("⚠️ No se pudo borrar el avatar antiguo: {}", e.getMessage());
                }
            }
        }

        // 6. GUARDADO FINAL
        userRepository.save(user);
        logger.info("✅ Perfil actualizado para usuario: {}", user.getEmail());

        // 7. RESPUESTA CONDICIONAL
        if (emailChanged) {
            // Avisamos a Angular para que cierre sesión
            return ResponseEntity.ok(Map.of(
                    "message", "Perfil actualizado. Email cambiado, inicia sesión de nuevo.",
                    "action", "LOGOUT_REQUIRED"));
        }

        return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
    }

    @GetMapping("profile")
    public ResponseEntity<User> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}