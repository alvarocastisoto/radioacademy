package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.module.CreateModuleRequest;
import com.radioacademy.backend.dto.module.ModuleRequest;
import com.radioacademy.backend.service.content.ModuleService;
import com.radioacademy.backend.security.JwtAuthenticationFilter;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ModuleController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModuleService moduleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getModulesByCourse_ShouldReturnModuleList() throws Exception {
        // Arrange
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ModuleRequest module = new ModuleRequest(moduleId, "Module 1", 1);
        List<ModuleRequest> modules = Arrays.asList(module);

        when(moduleService.getModulesByCourse(courseId)).thenReturn(modules);

        // Act & Assert
        mockMvc.perform(get("/api/modules/course/{courseId}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Module 1"))
                .andExpect(jsonPath("$[0].orderIndex").value(1));
    }

    @Test
    void createModule_ShouldReturnCreatedModule() throws Exception {
        // Arrange
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        CreateModuleRequest request = new CreateModuleRequest("New Module", 1, courseId);
        ModuleRequest response = new ModuleRequest(moduleId, "New Module", 1);

        when(moduleService.createModule(any(CreateModuleRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Module"));
    }

    @Test
    void createModule_ShouldReturnBadRequest_WhenTitleMissing() throws Exception {
        // Arrange - Missing title (validation should fail)
        UUID courseId = UUID.randomUUID();
        String invalidJson = "{\"orderIndex\": 1, \"courseId\": \"" + courseId + "\"}";

        // Act & Assert
        mockMvc.perform(post("/api/modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteModule_ShouldReturnNoContent() throws Exception {
        // Arrange
        UUID moduleId = UUID.randomUUID();
        doNothing().when(moduleService).deleteModule(moduleId);

        // Act & Assert
        mockMvc.perform(delete("/api/modules/{id}", moduleId))
                .andExpect(status().isNoContent());

        verify(moduleService).deleteModule(moduleId);
    }
}
