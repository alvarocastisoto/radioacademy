package com.radioacademy.backend.service.auth;

import com.radioacademy.backend.dto.auth.AuthResponseDTO;
import com.radioacademy.backend.dto.auth.LoginRequest;
import com.radioacademy.backend.dto.auth.RegisterRequest;
import com.radioacademy.backend.entity.PasswordResetToken;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.event.PasswordResetEvent;
import com.radioacademy.backend.event.UserRegistrationEvent;
import com.radioacademy.backend.repository.PasswordResetTokenRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldRegister_WhenEmailUnique() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName("Juan");
        request.setSurname("Perez");
        request.setEmail("juan@test.com");
        request.setPassword("123password");
        request.setDni("12345678A");
        request.setPhone("600000000");
        request.setRegion("Madrid");
        request.setTermsAccepted(true);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(Role.STUDENT);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(request.getEmail())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // Act
        AuthResponseDTO response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(request.getEmail(), response.user().email());
        verify(eventPublisher).publishEvent(any(UserRegistrationEvent.class));
    }

    @Test
    void register_ShouldThrow_WhenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName("Juan");
        request.setSurname("Perez");
        request.setEmail("juan@test.com");
        request.setPassword("123password");
        request.setDni("12345678A");
        request.setPhone("600000000");
        request.setRegion("Madrid");
        request.setTermsAccepted(true);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsValid() {
        // Arrange
        LoginRequest request = new LoginRequest("juan@test.com", "password");

        User user = new User();
        user.setEmail(request.email());
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(request.email())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // Act
        AuthResponseDTO response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ShouldThrow_WhenCredentialsInvalid() {
        // Arrange
        LoginRequest request = new LoginRequest("juan@test.com", "wrongpass");

        doThrow(new BadCredentialsException("Bad creds"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> authService.login(request));
    }

    @Test
    void forgotPassword_ShouldSendEvent_WhenUserExists() {
        String email = "juan@test.com";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        authService.forgotPassword(email);

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(PasswordResetEvent.class));
    }

    @Test
    void forgotPassword_ShouldDoNothing_WhenUserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        authService.forgotPassword("unknown@test.com");

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resetPassword_ShouldSuccess_WhenTokenValidAndStrongPassword() {
        String token = "valid-token";
        String newPass = "StrongPass1!";

        User user = new User();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(newPass)).thenReturn("encodedNewPass");

        authService.resetPassword(token, newPass);

        verify(userRepository).save(user);
        verify(tokenRepository).delete(resetToken);
    }

    @Test
    void resetPassword_ShouldThrow_WhenTokenNotFound() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> authService.resetPassword("invalid", "Pass123!"));
    }

    @Test
    void resetPassword_ShouldThrow_WhenTokenExpired() {
        String token = "expired-token";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThrows(ResponseStatusException.class, () -> authService.resetPassword(token, "Pass123!"));
    }

    @Test
    void resetPassword_ShouldThrow_WhenPasswordWeak() {
        String token = "valid-token";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThrows(ResponseStatusException.class, () -> authService.resetPassword(token, "weak"));
    }
}
