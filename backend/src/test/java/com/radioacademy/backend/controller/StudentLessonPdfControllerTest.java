package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.student.StudentService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudentLessonPdfController.class)
class StudentLessonPdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

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
    @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
    void getLessonPdf_ShouldReturnPdfInline() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        byte[] pdfContent = "%PDF-1.4 test content".getBytes();
        Resource resource = new ByteArrayResource(pdfContent) {
            @Override
            public String getFilename() {
                return "lesson.pdf";
            }
        };

        when(studentService.getLessonPdf(any(), any())).thenReturn(resource);

        
        mockMvc.perform(get("/api/student/lessons/{lessonId}/pdf", lessonId)
                .param("download", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"lesson.pdf\""));
    }

    @Test
    @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
    void getLessonPdf_ShouldReturnPdfAsAttachment() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        byte[] pdfContent = "%PDF-1.4 test content".getBytes();
        Resource resource = new ByteArrayResource(pdfContent) {
            @Override
            public String getFilename() {
                return "lesson.pdf";
            }
        };

        when(studentService.getLessonPdf(any(), any())).thenReturn(resource);

        
        mockMvc.perform(get("/api/student/lessons/{lessonId}/pdf", lessonId)
                .param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lesson.pdf\""));
    }

    @Test
    @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
    void getLessonPdf_ShouldUseFallbackFilename_WhenResourceFilenameIsNull() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        byte[] pdfContent = "%PDF-1.4 test content".getBytes();
        Resource resource = new ByteArrayResource(pdfContent) {
            @Override
            public String getFilename() {
                return null; 
            }
        };

        when(studentService.getLessonPdf(any(), any())).thenReturn(resource);

        
        mockMvc.perform(get("/api/student/lessons/{lessonId}/pdf", lessonId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
