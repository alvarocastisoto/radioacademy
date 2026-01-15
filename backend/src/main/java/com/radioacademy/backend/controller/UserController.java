package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService; // 
import org.springframework.beans.factory.annotation.Autowired;
import com.radioacademy.backend.dto.UserProfileDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // 
import org.springframework.http.HttpStatus; // 
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {

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

    @PutMapping("profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody UserProfileDTO profileData) {

        System.out.println("🚩 1. INICIO CONTROLADOR");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Preparar datos básicos
        String newName = profileData.name() != null ? profileData.name() : user.getName();
        String newSurname = profileData.surname() != null ? profileData.surname() : user.getSurname();
        String newPhone = profileData.phone() != null ? profileData.phone() : user.getPhone();
        String newEmail = user.getEmail();
        String newPassword = user.getPassword();

        // --- LÓGICA DE AVATAR Y GARBAGE COLLECTION ---
        String newAvatar = user.getAvatar(); // Por defecto mantenemos el viejo

        // Si nos envían un avatar nuevo Y es diferente al que ya teníamos
        if (profileData.avatar() != null && !profileData.avatar().isEmpty()
                && !profileData.avatar().equals(user.getAvatar())) {

            // 1. Actualizamos la variable para la BD
            newAvatar = profileData.avatar();

            // 2. Borramos el archivo físico antiguo (Si existe)
            String oldAvatarUrl = user.getAvatar();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                try {
                    // La URL es tipo ".../uploads/images/foto.jpg". Extraemos "foto.jpg"
                    String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                    System.out.println("🗑️ Imagen antigua borrada: " + filename);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo borrar la imagen antigua: " + e.getMessage());
                }
            }
        }
        // ---------------------------------------------

        // Validaciones de Email
        if (profileData.email() != null && !profileData.email().isEmpty()
                && !profileData.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(profileData.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El email ya existe"));
            }
            newEmail = profileData.email();
        }

        // Validaciones de Password
        if (profileData.newPassword() != null && !profileData.newPassword().isEmpty()) {
            if (profileData.currentPassword() == null || profileData.currentPassword().isEmpty()
                    || !passwordEncoder.matches(profileData.currentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "La contraseña actual es incorrecta"));
            }
            newPassword = passwordEncoder.encode(profileData.newPassword());
        }

        System.out.println("🚩 2. LLAMANDO A BD...");

        // Usamos el método personalizado que incluye el avatar
        userRepository.updateProfileDirectly(
                user.getId(), newName, newSurname, newPhone, newEmail, newPassword, newAvatar);

        System.out.println("🚩 3. ÉXITO");

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}