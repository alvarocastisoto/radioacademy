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
import com.radioacademy.backend.security.CustomUserDetails;
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

    
    @Transactional
    public AuthResponseDTO register(RegisterRequest request) {
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado.");
        }

        
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

        
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegistrationEvent(this, savedUser));

        
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(token, toUserAuthDTO(savedUser));
    }

    
    public AuthResponseDTO login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(token, toUserAuthDTO(user));
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();

            
            PasswordResetToken reset = tokenRepository.findByUser_Id(user.getId())
                    .orElseGet(() -> new PasswordResetToken());

            reset.setUser(user);
            reset.setToken(token);
            reset.setExpiryDate(LocalDateTime.now().plusMinutes(15));

            tokenRepository.save(reset);

            eventPublisher.publishEvent(new PasswordResetEvent(this, user, token));
        });
    }

    
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

    
    private UserAuthDTO toUserAuthDTO(User user) {
        return new UserAuthDTO(
                user.getId(), user.getName(), user.getSurname(), user.getEmail(),
                user.getRole(), user.getAvatar(), user.getCreatedAt());
    }

    private boolean isPasswordStrong(String password) {
        
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_])(?=\\S+$).{8,}$");
    }
}