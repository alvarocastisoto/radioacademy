package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.auth.AuthResponseDTO;
import com.radioacademy.backend.dto.auth.LoginRequest;
import com.radioacademy.backend.dto.auth.RegisterRequest;
import com.radioacademy.backend.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.radioacademy.backend.security.JwtAuthenticationFilter;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller testing
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturnToken_WhenSuccess() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@test.com");
        request.setPassword("Password123!"); // Valid: uppercase, lowercase, number, special char
        request.setName("New");
        request.setSurname("User");
        request.setDni("12345678Z");
        request.setPhone("612345678");
        request.setRegion("Test Region");
        request.setTermsAccepted(true);

        AuthResponseDTO response = new AuthResponseDTO("dummy-token", null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-token"));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsValid() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@test.com", "password");

        AuthResponseDTO response = new AuthResponseDTO("login-token", null);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailInvalid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email"); // Invalid format
        request.setPassword("pass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
