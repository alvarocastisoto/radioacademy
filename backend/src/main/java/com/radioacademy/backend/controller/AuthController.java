package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.AuthResponse;
import com.radioacademy.backend.dto.LoginRequest;
import com.radioacademy.backend.dto.RegisterRequest;
import com.radioacademy.backend.entity.PasswordResetToken;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.event.PasswordResetEvent;
import com.radioacademy.backend.event.UserRegistrationEvent;
import com.radioacademy.backend.repository.PasswordResetTokenRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.enums.Role;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    // Endpoint de Registro
    // Usamos ResponseEntity<?> (con interrogación) para poder devolver
    // AuthResponse si va bien, o un Map de error si va mal.
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        // 1. 🛡️ VALIDACIÓN DE DUPLICADOS
        // (Las validaciones de formato ya las hizo @Valid antes de entrar aquí)
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado. Por favor, inicia sesión."));
        }

        // 2. 🔄 MAPPING (DTO -> Entity)
        // Creamos el usuario nosotros mismos para tener CONTROL TOTAL
        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setDni(request.getDni());
        user.setPhone(request.getPhone());
        user.setRegion(request.getRegion());
        user.setTermsAccepted(request.isTermsAccepted());
        user.setCreatedAt(LocalDateTime.now());

        // 🔒 SEGURIDAD CRÍTICA: Asignamos el rol nosotros, ignorando lo que venga de
        // fuera
        user.setRole(Role.STUDENT);

        // 3. 🔐 CIFRADO DE CONTRASEÑA
        // Ya sabemos que es fuerte gracias a la anotación @Pattern del DTO
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. 💾 GUARDAR EN BD
        User savedUser = userRepository.save(user);

        // 5. 🎟️ GENERAR TOKEN Y EVENTOS
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        eventPublisher.publishEvent(new UserRegistrationEvent(this, savedUser));

        // 6. 🚀 RESPUESTA
        return ResponseEntity.ok(new AuthResponse(token, savedUser));
    }

    // Endpoint de Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Spring Security autentica (lanza excepción si falla)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // 2. Recuperamos la Entidad Usuario (para enviarla al Frontend)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado tras autenticación"));

        // 3. Generamos el token
        // Usamos el UserDetailsService para obtener la versión "Segura" del usuario
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        // 4. Devolvemos Token + Usuario completo
        return ResponseEntity.ok(new AuthResponse(token, user));
    }

    private boolean isPasswordStrong(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_])(?=\\S+$).{8,}$";
        return password.matches(regex);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // Buscamos usuario. Si no existe, NO decimos "no existe" por seguridad,
        // devolvemos OK igual para que los hackers no sepan qué emails tenemos.
        userRepository.findByEmail(email).ifPresent(user -> {
            // 1. Generar token aleatorio
            String token = UUID.randomUUID().toString();

            // 2. Guardarlo en BD (Borramos anteriores si hubiera)
            // Nota: En una app real, haz esto transaccional, pero para el prototipo vale.
            PasswordResetToken myToken = new PasswordResetToken(token, user);
            tokenRepository.save(myToken);

            // 3. Lanzar evento
            eventPublisher.publishEvent(new PasswordResetEvent(this, user, token));
        });

        return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un correo."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");

        // 1. Buscar token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token inválido"));

        // 2. Verificar caducidad
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "El token ha caducado"));
        }

        // 3. Cambiar contraseña
        User user = resetToken.getUser();
        if (isPasswordStrong(newPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Contraseña débil"));
        }

        // 4. Borrar el token usado (Para que no se pueda reusar)
        tokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida con éxito"));
    }

}