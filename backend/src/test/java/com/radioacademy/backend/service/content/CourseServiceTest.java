package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.course.CourseDTO;
import com.radioacademy.backend.dto.course.CourseDetailDTO;
import com.radioacademy.backend.dto.course.CreateCourseRequest;
import com.radioacademy.backend.dto.lesson.CreateLessonRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getAllCourses_ShouldReturnCourses_WhenNoUserLoggedIn() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle("Java Basics");
        course.setPrice(BigDecimal.TEN);

        when(courseRepository.findAll()).thenReturn(List.of(course));

        // Act
        List<CourseDTO> result = courseService.getAllCourses(auth);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Basics", result.get(0).title());
        assertFalse(result.get(0).isPurchased());
    }

    @Test
    void getAllCourses_ShouldMarkPurchased_WhenUserHasEnrollment() {
        // Arrange
        String email = "student@test.com";
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Java Advanced");
        course.setPrice(BigDecimal.TEN);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUser(user);

        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of(enrollment));

        // Act
        List<CourseDTO> result = courseService.getAllCourses(auth);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPurchased());
    }

    @Test
    void createCourse_ShouldSaveCourse_WhenValidRequest() {
        // Arrange
        String teacherEmail = "teacher@test.com";
        CreateLessonRequest lessonReq = new CreateLessonRequest("Lesson 1", "url", "pdf", 1, UUID.randomUUID());
        // Constructor order: title, description, price, hours, lesson (List), level,
        // coverImage
        CreateCourseRequest request = new CreateCourseRequest(
                "New Course",
                "Desc",
                BigDecimal.valueOf(100),
                10,
                List.of(lessonReq),
                "BEGINNER",
                "img.jpg");

        User teacher = new User();
        teacher.setEmail(teacherEmail);

        Course savedCourse = new Course();
        savedCourse.setId(UUID.randomUUID());
        savedCourse.setTitle(request.title());
        savedCourse.setPrice(request.price());
        savedCourse.setCoverImage(request.coverImage());
        savedCourse.setActive(true);

        when(userRepository.findByEmail(teacherEmail)).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CourseDetailDTO result = courseService.createCourse(request, teacherEmail);

        // Assert
        assertNotNull(result);
        assertEquals(request.title(), result.title());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_ShouldThrowException_WhenTeacherNotFound() {
        // Arrange
        String teacherEmail = "unknown@test.com";
        CreateLessonRequest lessonReq = new CreateLessonRequest("Lesson 1", "url", "pdf", 1, UUID.randomUUID());
        CreateCourseRequest request = new CreateCourseRequest(
                "Title",
                "Desc",
                BigDecimal.TEN,
                5,
                List.of(lessonReq),
                "INTERMEDIATE",
                "img.jpg");

        when(userRepository.findByEmail(teacherEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request, teacherEmail));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourse_ShouldUpdateFieldsAndImage_WhenExists() {
        // Arrange
        UUID courseId = UUID.randomUUID();
        CreateLessonRequest lessonReq = new CreateLessonRequest("Lesson 1", "url", "pdf", 1, UUID.randomUUID());
        CreateCourseRequest request = new CreateCourseRequest(
                "Updated Title",
                "Updated Desc",
                BigDecimal.valueOf(50),
                20,
                List.of(lessonReq),
                "ADVANCED",
                "new-image.jpg");

        Course existingCourse = new Course();
        existingCourse.setId(courseId);
        existingCourse.setTitle("Old Title");
        existingCourse.setCoverImage("old-image.jpg");
        existingCourse.setActive(true);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(existingCourse);

        // Act
        CourseDetailDTO result = courseService.updateCourse(courseId, request);

        // Assert
        assertEquals("Updated Title", existingCourse.getTitle());
        assertEquals("new-image.jpg", existingCourse.getCoverImage());
        verify(storageService).delete("old-image.jpg"); // Verifies image cleanup
        verify(courseRepository).save(existingCourse);
    }

    @Test
    void deleteCourse_ShouldDelete_WhenCourseExists() {
        // Arrange
        UUID courseId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);
        course.setCoverImage("image.jpg");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // Act
        courseService.deleteCourse(courseId);

        // Assert
        verify(storageService).delete("image.jpg");
        verify(courseRepository).delete(course);
    }
}
