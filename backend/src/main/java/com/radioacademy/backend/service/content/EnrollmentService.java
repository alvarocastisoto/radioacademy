package com.radioacademy.backend.service.content;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor 
@Slf4j 
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    /**
     * Matricula un usuario en un curso tras un pago exitoso.
     * Es IDEMPOTENTE: Si ya existe, no duplica ni lanza error.
     */
    @Transactional
    public void enrollUser(UUID userId, UUID courseId, String paymentId) {

        
        boolean exists = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);

        if (exists) {
            
            log.warn("⚠️ Intento de re-matrícula: Usuario {} ya tenía el curso '{}'", userId,
                    courseId);
            return;
        }

        
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(userRepository.getReferenceById(userId));
        enrollment.setCourse(courseRepository.getReferenceById(courseId));
        enrollment.setAmountPaid(courseRepository.findById(courseId).get().getPrice());
        enrollment.setPaymentId(paymentId);
        enrollment.setEnrolledAt(LocalDateTime.now());

        
        enrollmentRepository.save(enrollment);

        
        log.info("✅ Nueva matrícula creada: {} -> {} (Pago: {})", userId, courseId, paymentId);
    }
}