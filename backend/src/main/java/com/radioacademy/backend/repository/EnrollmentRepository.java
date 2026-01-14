package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    // 👇 AÑADE ESTO: Busca matrículas por el ID del usuario directamente en BD
    List<Enrollment> findByUserId(UUID userId);

    // Opcional: Para evitar duplicados en el momento de la compra
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);
}