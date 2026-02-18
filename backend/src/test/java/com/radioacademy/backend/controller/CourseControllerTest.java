package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.course.CourseDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.course.CourseDetailDTO;
import com.radioacademy.backend.dto.course.CreateCourseRequest;
import com.radioacademy.backend.service.content.CourseService;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
class CourseControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CourseService courseService;

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
        @WithMockUser(username = "testuser@test.com")
        void getAllCourses_ShouldReturnCourseList() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CourseDTO course = new CourseDTO(
                                courseId, "Test Course", "Description", "cover.jpg",
                                new BigDecimal("99.99"), 10, false);
                List<CourseDTO> courses = Arrays.asList(course);

                when(courseService.getAllCourses(any())).thenReturn(courses);

                
                mockMvc.perform(get("/api/courses"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Test Course"))
                                .andExpect(jsonPath("$[0].description").value("Description"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "ADMIN" })
        void createCourse_ShouldReturnCreatedCourse() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CreateCourseRequest request = new CreateCourseRequest(
                                "New Course", "New Description", new BigDecimal("49.99"),
                                5, null, "Beginner", "cover.jpg");
                CourseDetailDTO response = new CourseDetailDTO(
                                courseId, "New Course", "New Description", "cover.jpg",
                                new BigDecimal("49.99"), 5, true);

                when(courseService.createCourse(any(CreateCourseRequest.class), any())).thenReturn(response);

                
                mockMvc.perform(post("/api/courses")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("New Course"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "ADMIN" })
        void updateCourse_ShouldReturnUpdatedCourse() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CreateCourseRequest request = new CreateCourseRequest(
                                "Updated Course", "Updated Description", new BigDecimal("59.99"),
                                6, null, "Intermediate", "updated-cover.jpg");
                CourseDetailDTO response = new CourseDetailDTO(
                                courseId, "Updated Course", "Updated Description", "updated-cover.jpg",
                                new BigDecimal("59.99"), 6, true);

                when(courseService.updateCourse(eq(courseId), any(CreateCourseRequest.class))).thenReturn(response);

                
                mockMvc.perform(put("/api/courses/{id}", courseId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Updated Course"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "ADMIN" })
        void deleteCourse_ShouldReturnNoContent() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                doNothing().when(courseService).deleteCourse(courseId);

                
                mockMvc.perform(delete("/api/courses/{id}", courseId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(courseService).deleteCourse(courseId);
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getMyCourses_ShouldReturnDashboardCourses() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CourseDashboardDTO dashboard = new CourseDashboardDTO(
                                courseId, "My Course", "Course description", "cover.jpg", 75);
                List<CourseDashboardDTO> courses = Arrays.asList(dashboard);

                when(courseService.getMyCourses(any())).thenReturn(courses);

                
                mockMvc.perform(get("/api/courses/mine"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("My Course"))
                                .andExpect(jsonPath("$[0].progress").value(75));
        }

        @Test
        @WithMockUser(username = "testuser@test.com")
        void getCourseById_ShouldReturnCourseDetail() throws Exception {
                
                UUID courseId = UUID.randomUUID();
                CourseDetailDTO course = new CourseDetailDTO(
                                courseId, "Single Course", "Description", "cover.jpg",
                                new BigDecimal("29.99"), 8, true);

                when(courseService.getCourseById(courseId)).thenReturn(course);

                
                mockMvc.perform(get("/api/courses/{id}", courseId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Single Course"))
                                .andExpect(jsonPath("$.hours").value(8));
        }
}
