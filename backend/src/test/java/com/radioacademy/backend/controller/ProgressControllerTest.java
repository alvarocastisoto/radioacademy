package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.service.content.ProgressService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProgressController.class)
class ProgressControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ProgressService progressService;

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
        void toggleProgress_ShouldMarkLessonCompleted() throws Exception {
                
                UUID lessonId = UUID.randomUUID();
                ToggleProgressResponse response = new ToggleProgressResponse(
                                lessonId, true, "Lección marcada como completada");

                when(progressService.toggleProgress(any(), any())).thenReturn(response);

                
                mockMvc.perform(post("/api/progress/{lessonId}/toggle", lessonId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isCompleted").value(true))
                                .andExpect(jsonPath("$.message").value("Lección marcada como completada"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void toggleProgress_ShouldUnmarkLessonCompleted() throws Exception {
                
                UUID lessonId = UUID.randomUUID();
                ToggleProgressResponse response = new ToggleProgressResponse(
                                lessonId, false, "Lección desmarcada");

                when(progressService.toggleProgress(any(), any())).thenReturn(response);

                
                mockMvc.perform(post("/api/progress/{lessonId}/toggle", lessonId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isCompleted").value(false))
                                .andExpect(jsonPath("$.message").value("Lección desmarcada"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getCourseProgress_ShouldReturnProgress() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                UUID lessonId1 = UUID.randomUUID();
                UUID lessonId2 = UUID.randomUUID();
                Set<UUID> completedLessons = new HashSet<>(Arrays.asList(lessonId1, lessonId2));
                CourseProgressResponse response = new CourseProgressResponse(
                                courseId, 2, completedLessons);

                when(progressService.getCourseProgress(any(), any())).thenReturn(response);

                
                mockMvc.perform(get("/api/progress/course/{courseId}", courseId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completedCount").value(2));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getCourseProgress_ShouldReturnZeroProgress_WhenNoLessonsCompleted() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CourseProgressResponse response = new CourseProgressResponse(
                                courseId, 0, Collections.emptySet());

                when(progressService.getCourseProgress(any(), any())).thenReturn(response);

                
                mockMvc.perform(get("/api/progress/course/{courseId}", courseId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completedCount").value(0));
        }
}
