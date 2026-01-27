package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.LessonProgress;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private LessonProgressRepository progressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void toggleProgress_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> progressService.toggleProgress(UUID.randomUUID(), "test@test.com"));
    }

    @Test
    void toggleProgress_ShouldThrow_WhenLessonNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(lessonRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> progressService.toggleProgress(UUID.randomUUID(), "test@test.com"));
    }

    @Test
    void toggleProgress_ShouldThrow_WhenNotEnrolled() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(UUID.randomUUID());

        Module module = new Module();
        module.setCourse(course);

        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setModule(module);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(lessonRepository.findById(any())).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())).thenReturn(false);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> progressService.toggleProgress(lesson.getId(), "test@test.com"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void toggleProgress_ShouldCreateAndComplete_WhenNoProgressExists() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(UUID.randomUUID());
        Module module = new Module();
        module.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setModule(module);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(lessonRepository.findById(any())).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.existsByUserIdAndCourseId(any(), any())).thenReturn(true);
        when(progressRepository.findByUserIdAndLessonId(any(), any())).thenReturn(Optional.empty());

        // Act
        ToggleProgressResponse response = progressService.toggleProgress(lesson.getId(), "test@test.com");

        // Assert
        assertTrue(response.isCompleted());
        verify(progressRepository).save(any(LessonProgress.class));
    }

    @Test
    void toggleProgress_ShouldToggleStatus_WhenProgressExists() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(UUID.randomUUID());
        Module module = new Module();
        module.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setModule(module);

        LessonProgress existingProgress = new LessonProgress();
        existingProgress.setCompleted(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(lessonRepository.findById(any())).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.existsByUserIdAndCourseId(any(), any())).thenReturn(true);
        when(progressRepository.findByUserIdAndLessonId(any(), any())).thenReturn(Optional.of(existingProgress));

        // Act
        ToggleProgressResponse response = progressService.toggleProgress(lesson.getId(), "test@test.com");

        // Assert
        assertFalse(response.isCompleted()); // Toggled from true to false
        verify(progressRepository).save(existingProgress);
    }

    @Test
    void getCourseProgress_ShouldReturnCorrectCount() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID courseId = UUID.randomUUID();

        Set<UUID> completedLessonIds = Set.of(UUID.randomUUID(), UUID.randomUUID());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(progressRepository.findCompletedLessonIdsByCourse(user.getId(), courseId))
                .thenReturn(completedLessonIds);

        // Act
        CourseProgressResponse response = progressService.getCourseProgress(courseId, "test@test.com");

        // Assert
        assertEquals(courseId, response.courseId());
        assertEquals(2, response.completedCount());
        assertEquals(completedLessonIds, response.completedLessonIds());
    }
}
