package com.radioacademy.backend.service.payment;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.content.EnrollmentService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createCheckoutSession_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession("test@test.com", UUID.randomUUID()));
    }

    @Test
    void createCheckoutSession_ShouldThrow_WhenCourseNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(courseRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession("test@test.com", UUID.randomUUID()));
    }

    @Test
    void createCheckoutSession_ShouldThrow_WhenAlreadyEnrolled() {
        User user = new User();
        user.setId(UUID.randomUUID());
        Course course = new Course();
        course.setId(UUID.randomUUID());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(courseRepository.findById(any(UUID.class))).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession("test@test.com", course.getId()));
    }

    @Test
    void createCheckoutSession_ShouldReturnUrl_WhenValid() {
        // Arrange
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_123");
        ReflectionTestUtils.setField(paymentService, "successUrl", "http://success");
        ReflectionTestUtils.setField(paymentService, "cancelUrl", "http://cancel");

        User user = new User();
        user.setId(UUID.randomUUID());
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setPrice(new BigDecimal("10.00"));
        course.setTitle("Java Course");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(courseRepository.findById(any(UUID.class))).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(any(), any())).thenReturn(false);

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/test");

            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // Act
            String url = paymentService.createCheckoutSession("test@test.com", course.getId());

            // Assert
            assertEquals("https://checkout.stripe.com/test", url);
        }
    }

    @Test
    void confirmPayment_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> paymentService.confirmPayment("test@test.com", "sess_123"));
    }

    @Test
    void confirmPayment_ShouldThrow_WhenSecurityCheckFails() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getClientReferenceId()).thenReturn("different-uuid");

            sessionMock.when(() -> Session.retrieve("sess_123")).thenReturn(mockSession);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> paymentService.confirmPayment("test@test.com", "sess_123"));

            assertEquals(403, ex.getStatusCode().value());
        }
    }

    @Test
    void confirmPayment_ShouldEnroll_WhenPaymentSuccessful() {
        User user = new User();
        user.setId(UUID.randomUUID());

        UUID courseId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getClientReferenceId()).thenReturn(user.getId().toString());
            when(mockSession.getPaymentStatus()).thenReturn("paid");
            when(mockSession.getMetadata()).thenReturn(Map.of("course_id", courseId.toString()));
            when(mockSession.getPaymentIntent()).thenReturn("pi_123");

            sessionMock.when(() -> Session.retrieve("sess_123")).thenReturn(mockSession);

            // Act
            paymentService.confirmPayment("test@test.com", "sess_123");

            // Assert
            verify(enrollmentService).enrollUser(user, course, "pi_123");
        }
    }
}
