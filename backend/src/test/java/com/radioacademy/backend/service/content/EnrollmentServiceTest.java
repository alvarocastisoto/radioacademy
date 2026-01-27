package com.radioacademy.backend.service.content;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void enrollUser_ShouldSave_WhenNotEnrolled() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@test.com");

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle("Java Course");
        course.setPrice(BigDecimal.TEN);

        String paymentId = "pay_123";

        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())).thenReturn(false);

        // Act
        enrollmentService.enrollUser(user, course, paymentId);

        // Assert
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enrollUser_ShouldDoNothing_WhenAlreadyEnrolled() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());

        Course course = new Course();
        course.setId(UUID.randomUUID());

        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())).thenReturn(true);

        // Act
        enrollmentService.enrollUser(user, course, "pay_123");

        // Assert
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }
}
