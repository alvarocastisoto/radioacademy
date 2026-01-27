package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.service.payment.PaymentService;
import com.radioacademy.backend.security.JwtAuthenticationFilter;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
    void createCheckoutSession_ShouldReturnUrl() throws Exception {
        // Arrange
        UUID courseId = UUID.randomUUID();
        Map<String, String> request = Map.of("courseId", courseId.toString());
        String checkoutUrl = "https://stripe.com/checkout/session123";

        when(paymentService.createCheckoutSession(any(), any())).thenReturn(checkoutUrl);

        // Act & Assert
        mockMvc.perform(post("/api/payment/checkout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(checkoutUrl));
    }

    @Test
    @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
    void confirmPayment_ShouldReturnSuccess() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("session_id", "session_123abc");
        doNothing().when(paymentService).confirmPayment(any(), any());

        // Act & Assert
        mockMvc.perform(post("/api/payment/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pago confirmado y curso activado"));

        verify(paymentService).confirmPayment(any(), any());
    }
}
