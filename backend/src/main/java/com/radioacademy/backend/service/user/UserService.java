package com.radioacademy.backend.service.user;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    // 1. OBTENER TODOS (ADMIN)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. CREAR USUARIO (ADMIN/REGISTRO MANUAL)
    @Transactional
    public User createUser(User user) {
        // Encriptamos pass si viene en plano
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // 3. OBTENER MI PERFIL
    public User getMyProfile(String email) {
        User user = getUser(email);
        user.setPassword(null); // Seguridad: nunca devolver el hash
        return user;
    }

    // 4. ACTUALIZAR PERFIL (Lógica Compleja)
    @Transactional
    public Map<String, String> updateProfile(String email, UserProfileDTO profileData) {
        User user = getUser(email);
        boolean emailChanged = false;

        // A. CAMBIO DE CONTRASEÑA
        if (profileData.newPassword() != null && !profileData.newPassword().isBlank()) {
            // Validar pass actual
            if (profileData.currentPassword() == null || profileData.currentPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes introducir tu contraseña actual.");
            }
            if (!passwordEncoder.matches(profileData.currentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta.");
            }
            user.setPassword(passwordEncoder.encode(profileData.newPassword()));
        }

        // B. CAMBIO DE EMAIL
        if (profileData.email() != null && !profileData.email().isBlank()
                && !profileData.email().equals(user.getEmail())) {

            if (userRepository.existsByEmail(profileData.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está en uso.");
            }
            user.setEmail(profileData.email());
            emailChanged = true;
        }

        // C. DATOS BÁSICOS
        if (profileData.name() != null)
            user.setName(profileData.name());
        if (profileData.surname() != null)
            user.setSurname(profileData.surname());
        if (profileData.phone() != null)
            user.setPhone(profileData.phone());

        // D. GESTIÓN DE AVATAR (Garbage Collection)
        if (profileData.avatar() != null && !profileData.avatar().equals(user.getAvatar())) {
            String oldAvatarUrl = user.getAvatar();
            user.setAvatar(profileData.avatar());

            // Borrado físico del antiguo
            deleteOldAvatar(oldAvatarUrl);
        }

        userRepository.save(user);
        log.info("✅ Perfil actualizado: {}", user.getEmail());

        // E. RESPUESTA INTELIGENTE
        if (emailChanged) {
            return Map.of(
                    "message", "Perfil actualizado. Email cambiado, inicia sesión de nuevo.",
                    "action", "LOGOUT_REQUIRED");
        }

        return Map.of("message", "Perfil actualizado correctamente");
    }

    // --- Helpers Privados ---

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void deleteOldAvatar(String oldAvatarUrl) {
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                // Extraer filename de la URL (asumiendo formato standard)
                String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                storageService.delete(filename);
                log.info("🗑️ Avatar antiguo eliminado: {}", filename);
            } catch (Exception e) {
                log.warn("⚠️ No se pudo borrar el avatar antiguo: {}", e.getMessage());
            }
        }
    }
}