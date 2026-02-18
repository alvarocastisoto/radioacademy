package com.radioacademy.backend.service.user;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.dto.student.UserProfileResponseDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.CustomUserDetails;
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

    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    
    
    
    
    
    
    
    
    

    
    public UserProfileResponseDTO getMyProfile(CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return new UserProfileResponseDTO(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getRole() != null ? user.getRole().name() : null);
    }

    
    @Transactional
    public Map<String, String> updateProfile(CustomUserDetails userDetails, UserProfileDTO profileData) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        boolean emailChanged = false;

        
        if (profileData.newPassword() != null && !profileData.newPassword().isBlank()) {

            
            if (!profileData.newPassword().matches(PASSWORD_PATTERN)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La nueva contraseña debe ser fuerte: Min 8 caracteres, Mayúscula, Minúscula, Número y Especial.");
            }

            
            if (profileData.currentPassword() == null || profileData.currentPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Debes introducir tu contraseña actual para confirmar el cambio.");
            }

            
            if (!passwordEncoder.matches(profileData.currentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta.");
            }

            
            user.setPassword(passwordEncoder.encode(profileData.newPassword()));
        }

        
        if (profileData.email() != null && !profileData.email().isBlank()
                && !profileData.email().equals(user.getEmail())) {

            if (userRepository.existsByEmail(profileData.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está en uso.");
            }
            user.setEmail(profileData.email());
            emailChanged = true;
        }

        
        if (profileData.name() != null)
            user.setName(profileData.name());
        if (profileData.surname() != null)
            user.setSurname(profileData.surname());
        if (profileData.phone() != null)
            user.setPhone(profileData.phone());

        
        if (profileData.avatar() != null && !profileData.avatar().equals(user.getAvatar())) {
            String oldAvatarUrl = user.getAvatar();
            user.setAvatar(profileData.avatar());

            
            deleteOldAvatar(oldAvatarUrl);
        }

        userRepository.save(user);
        log.info("✅ Perfil actualizado: {}", user.getEmail());

        
        if (emailChanged) {
            return Map.of(
                    "message", "Perfil actualizado. Email cambiado, inicia sesión de nuevo.",
                    "action", "LOGOUT_REQUIRED");
        }

        return Map.of("message", "Perfil actualizado correctamente");
    }

    

    private void deleteOldAvatar(String oldAvatarPathOrUrl) {
        if (oldAvatarPathOrUrl == null || oldAvatarPathOrUrl.isBlank())
            return;

        storageService.delete(oldAvatarPathOrUrl);
        log.info("🗑️ Avatar antiguo eliminado (si existía): {}", oldAvatarPathOrUrl);
    }

}