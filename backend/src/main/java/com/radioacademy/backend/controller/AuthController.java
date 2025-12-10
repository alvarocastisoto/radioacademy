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

    // Endpoint de Registro (Para poder crear usuarios con contraseña cifrada)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) {
        // 1. Ciframos la contraseña antes de guardar
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Guardamos en BD
        userRepository.save(user);

        // 3. Generamos el token automáticamente para que ya quede logueado
        // Ojo: Aquí tenemos que convertir nuestro User a UserDetails para el token
        // Hacemos un truco rápido llamando al servicio o construyendo uno al vuelo
        // Para simplificar, devolvemos token vacío o hacemos login manual.
        // Mejor: Devolvemos un mensaje de éxito o el token generado.

        // Generamos token
        // Necesitamos castear o usar el UserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Spring Security intenta autenticar (comprueba usuario y contraseña)
        // Si falla, lanza una excepción automáticamente (403 Forbidden)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // 2. Si llegamos aquí, es que el login es correcto. Generamos el token.
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}