package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.service.exams.QuizService;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QuizController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createQuiz_ShouldReturnCreatedQuiz() throws Exception {
        // Arrange
        UUID quizId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        QuizDTO request = new QuizDTO(null, "Test Quiz", lessonId, Collections.emptyList());
        Quiz response = new Quiz();
        response.setId(quizId);
        response.setTitle("Test Quiz");

        when(quizService.createQuiz(any(QuizDTO.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Quiz"));
    }

    @Test
    void getQuizByLesson_ShouldReturnQuiz() throws Exception {
        // Arrange
        UUID lessonId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        QuizDTO quiz = new QuizDTO(quizId, "Lesson Quiz", lessonId, Collections.emptyList());

        when(quizService.getQuizByLessonId(lessonId)).thenReturn(Optional.of(quiz));

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/lesson/{lessonId}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lesson Quiz"));
    }

    @Test
    void getQuizByLesson_ShouldReturnNoContent_WhenNotFound() throws Exception {
        // Arrange
        UUID lessonId = UUID.randomUUID();
        when(quizService.getQuizByLessonId(lessonId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/lesson/{lessonId}", lessonId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getQuizById_ShouldReturnQuiz() throws Exception {
        // Arrange
        UUID quizId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        QuizDTO quiz = new QuizDTO(quizId, "Quiz by ID", lessonId, Collections.emptyList());

        when(quizService.getQuizById(quizId)).thenReturn(quiz);

        // Act & Assert
        mockMvc.perform(get("/api/quizzes/{id}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Quiz by ID"));
    }

    @Test
    void submitQuiz_ShouldReturnResult() throws Exception {
        // Arrange
        UUID quizId = UUID.randomUUID();
        Map<UUID, UUID> answers = new HashMap<>();
        answers.put(UUID.randomUUID(), UUID.randomUUID());
        QuizSubmissionDTO submission = new QuizSubmissionDTO(quizId, answers);
        QuizResultDTO result = new QuizResultDTO(90.0, true);

        when(quizService.submitQuiz(any(QuizSubmissionDTO.class))).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(90.0))
                .andExpect(jsonPath("$.passed").value(true));
    }
}
