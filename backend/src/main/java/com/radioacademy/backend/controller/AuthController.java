package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.AuthResponse;
import com.radioacademy.backend.dto.LoginRequest;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200") // Añadido por seguridad para evitar problemas de CORS
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
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) {
        // 1. Ciframos la contraseña
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Guardamos en BD
        User savedUser = userRepository.save(user);

        // 3. Generamos token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        // 4. Limpiamos la password antes de enviarla al frontend por seguridad
        // (Esto solo afecta a la respuesta JSON, no a la base de datos)
        // savedUser.setPassword("");

        // Devuelve Token + Usuario
        return ResponseEntity.ok(new AuthResponse(token, savedUser));
    }

    // Endpoint de Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Spring Security autentica
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // 2. Generamos el token
        // NOTA: Aquí usamos userRepository directamente para obtener el objeto User
        // completo (con ID, Role, etc.)
        // userDetailsService a veces devuelve solo una versión simplificada.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado tras autenticación"));

        String token = jwtService.generateToken(user); // Asumiendo que User implementa UserDetails

        // 3. Devolvemos Token + Usuario completo
        return ResponseEntity.ok(new AuthResponse(token, user));
    }
}