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

    
    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    
    
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.lesson.module.course.id = :courseId")
    List<LessonProgress> findByUserIdAndCourseId(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    
    
    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.lesson.module.course.id = :courseId AND lp.isCompleted = true")
    long countCompletedLessons(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    
    
    
    @Query("""
                SELECT lp.lesson.id
                FROM LessonProgress lp
                WHERE lp.user.id = :userId
                  AND lp.lesson.module.course.id = :courseId
                  AND lp.isCompleted = true
            """)
    Set<UUID> findCompletedLessonIdsByCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    
    @Query("SELECT lp.lesson.id FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.isCompleted = true")
    Set<UUID> findCompletedLessonIdsByUserId(UUID userId);
}