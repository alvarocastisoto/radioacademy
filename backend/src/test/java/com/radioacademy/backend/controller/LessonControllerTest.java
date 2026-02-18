package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.lesson.LessonResponse;
import com.radioacademy.backend.service.content.LessonService;
import com.radioacademy.backend.security.JwtAuthenticationFilter;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LessonController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LessonService lessonService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getLessonsByModule_ShouldReturnLessonList() throws Exception {
        
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        LessonResponse lesson = new LessonResponse(
                lessonId, "Lesson 1", "https://video.url", "https://pdf.url", 1, moduleId);
        List<LessonResponse> lessons = Arrays.asList(lesson);

        when(lessonService.getLessonsByModule(moduleId)).thenReturn(lessons);

        
        mockMvc.perform(get("/api/lessons/module/{moduleId}", moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Lesson 1"))
                .andExpect(jsonPath("$[0].orderIndex").value(1));
    }

    @Test
    void getLessonById_ShouldReturnLesson() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonResponse lesson = new LessonResponse(
                lessonId, "Single Lesson", "https://video.url", "https://pdf.url", 1, moduleId);

        when(lessonService.getLessonById(lessonId)).thenReturn(lesson);

        
        mockMvc.perform(get("/api/lessons/{id}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Single Lesson"));
    }

    @Test
    void createLesson_ShouldReturnCreatedLesson() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonResponse response = new LessonResponse(
                lessonId, "New Lesson", "https://video.url", null, 1, moduleId);

        when(lessonService.createLesson(any(), any(), any(), any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        
        mockMvc.perform(multipart("/api/lessons")
                .file(file)
                .param("title", "New Lesson")
                .param("videoUrl", "https://video.url")
                .param("moduleId", moduleId.toString())
                .param("orderIndex", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Lesson"));
    }

    @Test
    void createLesson_WithoutFile_ShouldReturnCreatedLesson() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonResponse response = new LessonResponse(
                lessonId, "New Lesson", "https://video.url", null, 1, moduleId);

        when(lessonService.createLesson(any(), any(), any(), any(), any())).thenReturn(response);

        
        mockMvc.perform(multipart("/api/lessons")
                .param("title", "New Lesson")
                .param("videoUrl", "https://video.url")
                .param("moduleId", moduleId.toString())
                .param("orderIndex", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Lesson"));
    }

    @Test
    void deleteLesson_ShouldReturnNoContent() throws Exception {
        
        UUID lessonId = UUID.randomUUID();
        doNothing().when(lessonService).deleteLesson(lessonId);

        
        mockMvc.perform(delete("/api/lessons/{id}", lessonId))
                .andExpect(status().isNoContent());

        verify(lessonService).deleteLesson(lessonId);
    }
}
