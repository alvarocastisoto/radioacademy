package com.radioacademy.backend.service.content;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 👈 Para logs profesionales
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor // 👈 Inyección por constructor (Standard Pro)
@Slf4j // 👈 Inyecta automáticamente una variable 'log'
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

        // 1. Doble verificación de seguridad
        boolean exists = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);

        if (exists) {
            // Usamos log.warn para advertencias (no es un error crítico, pero es raro)
            log.warn("⚠️ Intento de re-matrícula: Usuario {} ya tenía el curso '{}'", userId,
                    courseId);
            return;
        }

        // 2. Crear la matrícula
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(userRepository.getReferenceById(userId));
        enrollment.setCourse(courseRepository.getReferenceById(courseId));
        enrollment.setAmountPaid(courseRepository.findById(courseId).get().getPrice());
        enrollment.setPaymentId(paymentId);
        enrollment.setEnrolledAt(LocalDateTime.now());

        // 3. Guardar
        enrollmentRepository.save(enrollment);

        // Usamos log.info para confirmar éxito
        log.info("✅ Nueva matrícula creada: {} -> {} (Pago: {})", userId, courseId, paymentId);
    }
}