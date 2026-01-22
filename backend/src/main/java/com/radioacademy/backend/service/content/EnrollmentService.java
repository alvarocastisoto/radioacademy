package com.radioacademy.backend.service.content;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 👈 Para logs profesionales
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor // 👈 Inyección por constructor (Standard Pro)
@Slf4j // 👈 Inyecta automáticamente una variable 'log'
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Matricula un usuario en un curso tras un pago exitoso.
     * Es IDEMPOTENTE: Si ya existe, no duplica ni lanza error.
     */
    @Transactional
    public void enrollUser(User user, Course course, String paymentId) {

        // 1. Doble verificación de seguridad
        boolean exists = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId());

        if (exists) {
            // Usamos log.warn para advertencias (no es un error crítico, pero es raro)
            log.warn("⚠️ Intento de re-matrícula: Usuario {} ya tenía el curso '{}'", user.getEmail(),
                    course.getTitle());
            return;
        }

        // 2. Crear la matrícula
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setAmountPaid(course.getPrice());
        enrollment.setPaymentId(paymentId);
        enrollment.setEnrolledAt(LocalDateTime.now());

        // 3. Guardar
        enrollmentRepository.save(enrollment);

        // Usamos log.info para confirmar éxito
        log.info("✅ Nueva matrícula creada: {} -> {} (Pago: {})", user.getEmail(), course.getTitle(), paymentId);
    }
}