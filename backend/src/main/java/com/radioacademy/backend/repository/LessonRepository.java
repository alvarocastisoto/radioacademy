package com.radioacademy.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.radioacademy.backend.entity.Lesson;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByModuleIdOrderByOrderIndexAsc(UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l Where l.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);
}
