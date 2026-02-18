package com.radioacademy.backend.repository.exams;

import com.radioacademy.backend.entity.QuizAttempt;
import com.radioacademy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findTopByUserAndQuizIdOrderByCompletedAtDesc(User user, UUID quizId);

    
    
    
    
    
    @Query("""
                SELECT qa.question.id
                FROM QuizAnswer qa
                JOIN qa.attempt att
                WHERE att.user = :user AND att.quiz.id = :quizId
                GROUP BY qa.question.id
                HAVING bool_or(qa.isCorrect) = false
                   OR (
                       SELECT qa2.isCorrect
                       FROM QuizAnswer qa2
                       JOIN qa2.attempt att2
                       WHERE qa2.question.id = qa.question.id AND att2.user = :user
                       ORDER BY att2.completedAt DESC LIMIT 1
                   ) = false
            """)
    List<UUID> findFailedQuestionIds(@Param("user") User user, @Param("quizId") UUID quizId);
    
    
}