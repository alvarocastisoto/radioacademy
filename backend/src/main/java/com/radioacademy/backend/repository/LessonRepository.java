package com.radioacademy.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.radioacademy.backend.entity.Lesson;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByModuleIdOrderByOrderIndexAsc(UUID courseId);

}
