package com.radioacademy.backend.service.auth;

import com.radioacademy.backend.dto.auth.AuthResponseDTO;
import com.radioacademy.backend.dto.auth.LoginRequest;
import com.radioacademy.backend.dto.auth.RegisterRequest;
import com.radioacademy.backend.dto.auth.UserAuthDTO;
import com.radioacademy.backend.entity.PasswordResetToken;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.event.PasswordResetEvent;
import com.radioacademy.backend.event.UserRegistrationEvent;
import com.radioacademy.backend.repository.PasswordResetTokenRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetTokenRepository tokenRepository;

    // ✅ REGISTRO
    @Transactional
    public AuthResponseDTO register(RegisterRequest request) {
        // 1. Validación: Si existe, lanzamos 409 CONFLICT
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado.");
        }

        // 2. Crear Entidad
        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setDni(request.getDni());
        user.setPhone(request.getPhone());
        user.setRegion(request.getRegion());
        user.setTermsAccepted(request.isTermsAccepted());
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.STUDENT);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Guardar y Eventos
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegistrationEvent(this, savedUser));

        // 4. Generar Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(token, toUserAuthDTO(savedUser));
    }

    // ✅ LOGIN
    public AuthResponseDTO login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            // Convertimos el error genérico en un 401 UNAUTHORIZED claro
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(token, toUserAuthDTO(user));
    }

    // ✅ FORGOT PASSWORD
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken myToken = new PasswordResetToken(token, user);
            tokenRepository.save(myToken);
            eventPublisher.publishEvent(new PasswordResetEvent(this, user, token));
        });
        // Si no existe, no hacemos nada (silencioso por seguridad), el controller
        // devuelve OK.
    }

    // ✅ RESET PASSWORD
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token inválido"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token ha caducado");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es muy débil");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
    }

    // --- Helpers Privados ---
    private UserAuthDTO toUserAuthDTO(User user) {
        return new UserAuthDTO(
                user.getId(), user.getName(), user.getSurname(), user.getEmail(),
                user.getRole(), user.getAvatar(), user.getCreatedAt());
    }

    private boolean isPasswordStrong(String password) {
        // Regex simple: 8 chars, 1 numero, 1 mayus, 1 minus, 1 especial
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_])(?=\\S+$).{8,}$");
    }
}