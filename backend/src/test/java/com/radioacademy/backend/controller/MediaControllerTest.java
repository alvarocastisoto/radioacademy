package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.media.MediaService;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

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
    @WithMockUser(username = "testuser@test.com", authorities = { "ADMIN" })
    void uploadFile_ShouldReturnPath() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        String storedPath = "uploads/images/test.pdf";

        when(mediaService.uploadMedia(any())).thenReturn(storedPath);

        // Act & Assert
        mockMvc.perform(multipart("/api/media/upload").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(storedPath));
    }

    @Test
    @WithMockUser(username = "testuser@test.com")
    void download_ShouldReturnResource() throws Exception {
        // Arrange
        String path = "uploads/images/test.jpg";
        byte[] content = "image content".getBytes();
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "test.jpg";
            }
        };

        when(mediaService.loadMediaResource(path)).thenReturn(resource);
        when(mediaService.determineMediaType("test.jpg")).thenReturn(MediaType.APPLICATION_OCTET_STREAM);

        // Act & Assert
        mockMvc.perform(get("/api/media/download")
                .param("path", path)
                .param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.jpg\""));
    }

    @Test
    @WithMockUser(username = "testuser@test.com")
    void download_ShouldReturnInlineResource() throws Exception {
        // Arrange
        String path = "uploads/images/test.jpg";
        byte[] content = "image content".getBytes();
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "test.jpg";
            }
        };

        when(mediaService.loadMediaResource(path)).thenReturn(resource);
        when(mediaService.determineMediaType("test.jpg")).thenReturn(MediaType.APPLICATION_OCTET_STREAM);

        // Act & Assert
        mockMvc.perform(get("/api/media/download")
                .param("path", path)
                .param("download", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test.jpg\""));
    }
}
