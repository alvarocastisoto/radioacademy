package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.AuthResponse;
import com.radioacademy.backend.dto.LoginRequest;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
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

    // Endpoint de Registro
    // Usamos ResponseEntity<?> (con interrogación) para poder devolver
    // AuthResponse si va bien, o un Map de error si va mal.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // 1. 🛡️ VALIDACIÓN PREVIA: Comprobar si el email ya existe
        // Esto evita el error 500 de PostgreSQL
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict
                    .body(Map.of("error", "El email ya está registrado. Por favor, inicia sesión."));
        }

        // 2. Ciframos la contraseña
        System.out.println("📦 USUARIO RECIBIDO: " + user.getEmail());
        if (isPasswordStrong(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                    .body(Map.of("error", "La contraseña no es lo suficientemente fuerte. " +
                            "Debe tener al menos 8 caracteres, incluyendo mayúsculas, minúsculas, " +
                            "números y caracteres especiales."));
        }

        // 3. Ponemos fecha de creación si no viene
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        // 4. Guardamos en BD
        User savedUser = userRepository.save(user);

        // 5. Generamos token
        // IMPORTANTE: Cargamos el UserDetails desde el servicio para asegurar
        // compatibilidad
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        // Devuelve Token + Usuario
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado tras autenticación"));

        // 3. Generamos el token
        // Usamos el UserDetailsService para obtener la versión "Segura" del usuario
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        // 4. Devolvemos Token + Usuario completo
        return ResponseEntity.ok(new AuthResponse(token, user));
    }

    private boolean isPasswordStrong(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._*-])(?=\\S+$).{8,}$";
        return password.matches(regex);
    }
}