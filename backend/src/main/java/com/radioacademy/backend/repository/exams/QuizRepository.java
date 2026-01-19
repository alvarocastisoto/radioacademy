package com.radioacademy.backend.repository.exams;

import com.radioacademy.backend.entity.Quiz;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Optional<Quiz> findByLessonId(UUID lessonId);
}
