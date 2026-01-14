package com.radioacademy.backend.service;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Matricula un usuario en un curso tras un pago exitoso.
     * Es IDEMPOTENTE: Si ya existe, no duplica ni lanza error, simplemente retorna
     * la existente.
     */
    @Transactional
    public void enrollUser(User user, Course course, String paymentId) {

        // 1. Doble verificación de seguridad
        boolean exists = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId());

        if (exists) {
            System.out.println("⚠️ El usuario " + user.getEmail() + " ya tenía el curso " + course.getTitle());
            return; // Salimos sin hacer nada
        }

        // 2. Crear la matrícula
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setAmountPaid(course.getPrice());
        enrollment.setPaymentId(paymentId);
        enrollment.setEnrolledAt(LocalDateTime.now()); // Asegúrate de tener este campo o quítalo si no existe

        // 3. Guardar
        enrollmentRepository.save(enrollment);
        System.out.println("✅ Nueva matrícula creada: " + user.getEmail() + " -> " + course.getTitle());
    }
}