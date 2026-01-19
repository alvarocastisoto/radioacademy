package com.radioacademy.backend.repository.exams;

import org.springframework.data.jpa.repository.JpaRepository;
import com.radioacademy.backend.entity.Question;
import java.util.UUID;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    Optional<Question> findByQuizId(UUID quizId);
}
