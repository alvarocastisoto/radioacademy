package com.radioacademy.backend.service.student;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.dto.internal.LessonPdfInfo;
import com.radioacademy.backend.entity.*;
import com.radioacademy.backend.repository.*;
import com.radioacademy.backend.repository.exams.QuizRepository;
import com.radioacademy.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository progressRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private StudentService studentService;

    @Test
    void getMyDashboard_ShouldReturnCorrectProgress() {
        // Arrange
        String email = "test@test.com";
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle("Java Course");

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByUserId(user.getId())).thenReturn(List.of(enrollment));
        when(lessonRepository.countByCourseId(course.getId())).thenReturn(10L);
        when(progressRepository.countCompletedLessons(user.getId(), course.getId())).thenReturn(5L);

        // Act
        List<CourseDashboardDTO> dashboard = studentService.getMyDashboard(email);

        // Assert
        assertEquals(1, dashboard.size());
        assertEquals(50, dashboard.get(0).progress());
        assertEquals("Java Course", dashboard.get(0).title());
    }

    @Test
    void getCourseContent_ShouldThrow_WhenNotEnrolled() {
        UUID courseId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> studentService.getCourseContent(courseId, "test@test.com"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getCourseContent_ShouldReturnStructure() {
        // Arrange
        UUID courseId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(courseId);
        course.setModules(new ArrayList<>());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(progressRepository.findCompletedLessonIdsByUserId(user.getId())).thenReturn(Collections.emptySet());

        // Act
        CourseContentDTO content = studentService.getCourseContent(courseId, "test@test.com");

        // Assert
        assertNotNull(content);
        assertEquals(courseId, content.id());
    }

    @Test
    void getLessonPdf_ShouldThrow_WhenPdfNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(lessonRepository.findPdfInfo(any())).thenReturn(Optional.of(new LessonPdfInfo(UUID.randomUUID(), null)));

        assertThrows(ResponseStatusException.class,
                () -> studentService.getLessonPdf(UUID.randomUUID(), "test@test.com"));
    }

    @Test
    void getLessonPdf_ShouldThrow_WhenNotEnrolled() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID courseId = UUID.randomUUID();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(lessonRepository.findPdfInfo(any())).thenReturn(Optional.of(new LessonPdfInfo(courseId, "test.pdf")));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> studentService.getLessonPdf(UUID.randomUUID(), "test@test.com"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getLessonPdf_ShouldReturnResource_WhenAuthorized() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID courseId = UUID.randomUUID();
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(lessonRepository.findPdfInfo(any())).thenReturn(Optional.of(new LessonPdfInfo(courseId, "test.pdf")));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)).thenReturn(true);
        when(storageService.loadAsResource("test.pdf")).thenReturn(mockResource);

        Resource result = studentService.getLessonPdf(UUID.randomUUID(), "test@test.com");

        assertNotNull(result);
    }

    @Test
    void submitQuiz_ShouldCalculateScoreCorrectly() {
        UUID quizId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();
        UUID optCorrectId = UUID.randomUUID();

        Option correct = new Option();
        correct.setId(optCorrectId);
        correct.setCorrect(true);

        Question q1 = new Question();
        q1.setId(q1Id);
        q1.setPoints(10);
        q1.setOptions(List.of(correct));

        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setQuestions(List.of(q1));

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        QuizSubmissionDTO sub = new QuizSubmissionDTO(quizId, Map.of(q1Id, optCorrectId));

        QuizResultDTO result = studentService.submitQuiz(sub);

        assertEquals(100.0, result.score());
        assertTrue(result.passed());
    }
}
