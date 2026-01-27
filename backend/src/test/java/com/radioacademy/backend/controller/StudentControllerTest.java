package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudentController.class)
class StudentControllerTest {

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
        void getMyDashboard_ShouldReturnDashboard() throws Exception {
                // Arrange
                UUID courseId = UUID.randomUUID();
                CourseDashboardDTO dashboard = new CourseDashboardDTO(
                                courseId, "My Course", "Description", "cover.jpg", 50);
                List<CourseDashboardDTO> courses = Arrays.asList(dashboard);

                when(studentService.getMyDashboard(any())).thenReturn(courses);

                // Act & Assert
                mockMvc.perform(get("/api/student/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("My Course"))
                                .andExpect(jsonPath("$[0].progress").value(50));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getCourseContent_ShouldReturnCourseContent() throws Exception {
                // Arrange
                UUID courseId = UUID.randomUUID();
                CourseContentDTO content = new CourseContentDTO(
                                courseId, "Course Title", "Description", Collections.emptyList(), "cover.jpg", 25);

                when(studentService.getCourseContent(any(), any())).thenReturn(content);

                // Act & Assert
                mockMvc.perform(get("/api/student/course/{courseId}/content", courseId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Course Title"))
                                .andExpect(jsonPath("$.progress").value(25));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void getQuizForStudent_ShouldReturnQuiz() throws Exception {
                // Arrange
                UUID quizId = UUID.randomUUID();
                UUID lessonId = UUID.randomUUID();
                QuizDTO quiz = new QuizDTO(quizId, "Quiz Title", lessonId, Collections.emptyList());

                when(studentService.getQuizForStudent(quizId)).thenReturn(quiz);

                // Act & Assert
                mockMvc.perform(get("/api/student/quiz/{quizId}", quizId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Quiz Title"));
        }

        @Test
        @WithMockUser(username = "testuser@test.com", authorities = { "STUDENT" })
        void submitQuiz_ShouldReturnResult() throws Exception {
                // Arrange
                UUID quizId = UUID.randomUUID();
                Map<UUID, UUID> answers = new HashMap<>();
                answers.put(UUID.randomUUID(), UUID.randomUUID());
                QuizSubmissionDTO submission = new QuizSubmissionDTO(quizId, answers);
                QuizResultDTO result = new QuizResultDTO(85.0, true);

                when(studentService.submitQuiz(any(QuizSubmissionDTO.class))).thenReturn(result);

                // Act & Assert
                mockMvc.perform(post("/api/student/quiz/submit")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(submission)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.score").value(85.0))
                                .andExpect(jsonPath("$.passed").value(true));
        }
}
