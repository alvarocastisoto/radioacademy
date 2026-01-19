package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    // 👇 AÑADE ESTO: Busca matrículas por el ID del usuario directamente en BD
    List<Enrollment> findByUserId(UUID userId);

    // Opcional: Para evitar duplicados en el momento de la compra
    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    // Devuelve un Optional directamente si existe la matrícula de X usuario en Y
    // curso
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    void deleteByUser_IdAndCourse_Id(UUID userId, UUID courseId);

    @Query("select e.course from Enrollment e where e.user.id = :userId")
    List<Course> findCoursesByUserId(@Param("userId") UUID userId);

}