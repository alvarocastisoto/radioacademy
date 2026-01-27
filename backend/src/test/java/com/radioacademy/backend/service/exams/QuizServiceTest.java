package com.radioacademy.backend.service.exams;

import com.radioacademy.backend.dto.exams.OptionDTO;
import com.radioacademy.backend.dto.exams.QuestionDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Option;
import com.radioacademy.backend.entity.Question;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.exams.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private QuizService quizService;

    @Test
    void createQuiz_ShouldCreateNew_WhenNoneExists() {
        // Arrange
        UUID lessonId = UUID.randomUUID();
        QuestionDTO qDto = new QuestionDTO(null, "¿Es Java genial?",
                List.of(new OptionDTO(null, "Si", true)), 10);

        QuizDTO request = new QuizDTO(null, "Test Quiz", lessonId, List.of(qDto));

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.empty());
        when(quizRepository.save(any(Quiz.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Quiz result = quizService.createQuiz(request);

        // Assert
        assertNotNull(result);
        assertEquals("Test Quiz", result.getTitle());
        assertEquals(1, result.getQuestions().size());
        assertEquals("Si", result.getQuestions().get(0).getOptions().get(0).getText());
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void createQuiz_ShouldUpdateExisting_WhenOneExists() {
        UUID lessonId = UUID.randomUUID();
        Quiz existingQuiz = new Quiz();
        existingQuiz.setId(UUID.randomUUID());
        existingQuiz.setTitle("Old Title");
        existingQuiz.setQuestions(new ArrayList<>());

        QuizDTO request = new QuizDTO(null, "New Title", lessonId, Collections.emptyList());

        Lesson existingLesson = new Lesson();
        existingLesson.setId(lessonId);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        when(quizRepository.findByLessonId(lessonId)).thenReturn(Optional.of(existingQuiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(i -> i.getArguments()[0]);

        Quiz result = quizService.createQuiz(request);

        assertEquals("New Title", result.getTitle()); // Should update title
        assertEquals(existingQuiz.getId(), result.getId()); // Should keep ID
    }

    @Test
    void getQuizById_ShouldThrow_WhenNotFound() {
        when(quizRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> quizService.getQuizById(UUID.randomUUID()));
    }

    @Test
    void submitQuiz_ShouldPass_WhenAnswersCorrect() {
        // Arrange
        UUID quizId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();
        UUID optCorrectId = UUID.randomUUID();

        Option correctOpt = new Option();
        correctOpt.setId(optCorrectId);
        correctOpt.setCorrect(true);

        Question q1 = new Question();
        q1.setId(q1Id);
        q1.setOptions(List.of(correctOpt));

        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setQuestions(List.of(q1));

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        QuizSubmissionDTO submission = new QuizSubmissionDTO(quizId, Map.of(q1Id, optCorrectId));

        // Act
        QuizResultDTO result = quizService.submitQuiz(submission);

        // Assert
        assertEquals(100.0, result.score());
        assertTrue(result.passed());
    }

    @Test
    void submitQuiz_ShouldFail_WhenAnswersWrong() {
        // Arrange
        UUID quizId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();
        UUID optWrongId = UUID.randomUUID();

        Option correctOpt = new Option();
        correctOpt.setId(UUID.randomUUID());
        correctOpt.setCorrect(true);

        Option wrongOpt = new Option();
        wrongOpt.setId(optWrongId);
        wrongOpt.setCorrect(false);

        Question q1 = new Question();
        q1.setId(q1Id);
        q1.setOptions(List.of(correctOpt, wrongOpt));

        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setQuestions(List.of(q1));

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        QuizSubmissionDTO submission = new QuizSubmissionDTO(quizId, Map.of(q1Id, optWrongId));

        // Act
        QuizResultDTO result = quizService.submitQuiz(submission);

        // Assert
        assertEquals(0.0, result.score());
        assertFalse(result.passed());
    }

    @Test
    void submitQuiz_ShouldCalculatePartialScore() {
        // 2 Questions, 1 right, 1 wrong => 50%
        UUID quizId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        UUID opt1Correct = UUID.randomUUID();
        UUID opt2Wrong = UUID.randomUUID();

        // Q1 Correcto
        Option o1 = new Option();
        o1.setId(opt1Correct);
        o1.setCorrect(true);
        Question q1 = new Question();
        q1.setId(q1Id);
        q1.setOptions(List.of(o1));

        // Q2 Incorrecto
        Option o2T = new Option();
        o2T.setId(UUID.randomUUID());
        o2T.setCorrect(true);
        Option o2F = new Option();
        o2F.setId(opt2Wrong);
        o2F.setCorrect(false);
        Question q2 = new Question();
        q2.setId(q2Id);
        q2.setOptions(List.of(o2T, o2F));

        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setQuestions(List.of(q1, q2));

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        QuizSubmissionDTO sub = new QuizSubmissionDTO(quizId, Map.of(
                q1Id, opt1Correct,
                q2Id, opt2Wrong));

        QuizResultDTO result = quizService.submitQuiz(sub);

        assertEquals(50.0, result.score());
        assertTrue(result.passed()); // >= 50 is Pass
    }

    @Test
    void submitQuiz_ShouldHandleEmptyQuiz() {
        UUID quizId = UUID.randomUUID();
        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setQuestions(Collections.emptyList());

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        QuizResultDTO result = quizService.submitQuiz(new QuizSubmissionDTO(quizId, Map.of()));

        assertEquals(100.0, result.score()); // As per current service logic
    }
}
