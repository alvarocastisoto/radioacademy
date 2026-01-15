package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    // Buscar si una lección concreta está vista
    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    // Buscar TODAS las lecciones vistas de un curso (Para pintar el sidebar rápido)
    // Hacemos un JOIN con Lesson para filtrar por el CourseId
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.lesson.module.course.id = :courseId")
    List<LessonProgress> findByUserIdAndCourseId(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    // Contar cuántas lecciones lleva vistas de un curso (Para la barra de
    // porcentaje)
    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.lesson.module.course.id = :courseId AND lp.isCompleted = true")
    long countCompletedLessons(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    // 3. OPTIMIZACIÓN MÁXIMA (IDs de lecciones vistas SOLO de este curso)
    // Usamos esto para pintar los checks verdes en el frontend sin cargar objetos
    // pesados
    @Query("""
                SELECT lp.lesson.id
                FROM LessonProgress lp
                WHERE lp.user.id = :userId
                  AND lp.lesson.module.course.id = :courseId
                  AND lp.isCompleted = true
            """)
    Set<UUID> findCompletedLessonIdsByCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    // Obtenemos un SET de IDs para que la búsqueda sea ultra-rápida en memoria
    @Query("SELECT lp.lesson.id FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.isCompleted = true")
    Set<UUID> findCompletedLessonIdsByUserId(UUID userId);
}