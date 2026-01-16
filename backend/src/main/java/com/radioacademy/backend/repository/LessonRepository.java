package com.radioacademy.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.radioacademy.backend.repository.projection.LessonPdfInfo;

import com.radioacademy.backend.entity.Lesson;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    @Query("SELECT COUNT(l) FROM Lesson l Where l.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);

    @Query("""
                SELECT l.module.course.id AS courseId, l.pdfUrl AS pdfUrl
                FROM Lesson l
                WHERE l.id = :lessonId
            """)
    Optional<LessonPdfInfo> findPdfInfo(@Param("lessonId") UUID lessonId);

}
