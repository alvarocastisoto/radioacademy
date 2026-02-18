package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.dto.student.UserProfileResponseDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.service.user.UserService;
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

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

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
        void getAllUsers_ShouldReturnUserList() throws Exception {
                
                User user = new User();
                user.setId(UUID.randomUUID());
                user.setEmail("test@test.com");
                user.setName("Test");
                user.setSurname("User");
                List<User> users = Arrays.asList(user);

                when(userService.getAllUsers()).thenReturn(users);

                
                mockMvc.perform(get("/api/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].email").value("test@test.com"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getMyProfile_ShouldReturnProfile() throws Exception {
                
                UUID userId = UUID.randomUUID();
                UserProfileResponseDTO profile = new UserProfileResponseDTO(
                                userId, "John", "Doe", "test@test.com", "612345678", "avatar.jpg", "STUDENT");

                when(userService.getMyProfile(any())).thenReturn(profile);

                
                mockMvc.perform(get("/api/users/profile"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("test@test.com"))
                                .andExpect(jsonPath("$.name").value("John"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void updateProfile_ShouldReturnSuccess() throws Exception {
                
                UUID userId = UUID.randomUUID();
                UserProfileDTO profileData = new UserProfileDTO(
                                userId, "John", "Doe", "test@test.com", "612345678", null, null, null);
                Map<String, String> response = Map.of("message", "Perfil actualizado");

                when(userService.updateProfile(any(), any(UserProfileDTO.class))).thenReturn(response);

                
                mockMvc.perform(put("/api/users/profile")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profileData)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Perfil actualizado"));
        }
}
