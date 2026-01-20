package com.radioacademy.backend.service.student;

import org.springframework.stereotype.Service;
import com.radioacademy.backend.dto.exams.OptionDTO;
import com.radioacademy.backend.dto.exams.QuestionDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.student.QuizResultDTO;
import com.radioacademy.backend.dto.student.QuizSubmissionDTO;
import com.radioacademy.backend.entity.Option;
import com.radioacademy.backend.entity.Question;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.repository.exams.QuizRepository;
import com.radioacademy.backend.service.exams.QuizService;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final QuizRepository quizRepository;
    private final QuizService quizService; // Reutilizamos el servicio de exams para mapear

    // 1. Obtener Examen para el Alumno (SIN RESPUESTAS CORRECTAS) 🙈
    @Transactional(readOnly = true)
    public QuizDTO getQuizForStudent(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Examen no encontrado"));

        // Usamos el mismo DTO que en admin, pero ponemos isCorrect = false a todo
        // para que el alumno no pueda hacer trampas mirando el JSON.
        List<QuestionDTO> questions = quiz.getQuestions().stream().map(q -> new QuestionDTO(
                q.getId(),
                q.getContent(),
                q.getOptions().stream().map(o ->
                // ⚠️ TRUCO DE SEGURIDAD: Siempre false al enviarlo al alumno
                new OptionDTO(o.getId(), o.getText(), false)).toList(),
                q.getPoints())).toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getLesson().getId(), questions);
    }

    // 2. Corregir Examen 📝
    // 2. Corregir Examen 📝
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission) {
        System.out.println("🔎 --- INICIANDO CORRECCIÓN ---");
        System.out.println("📥 ID Examen recibido: " + submission.quizId());
        System.out.println("📥 Respuestas Usuario: " + submission.answers());

        Quiz quiz = quizRepository.findById(submission.quizId())
                .orElseThrow(() -> new EntityNotFoundException("Examen no encontrado"));

        int totalPoints = 0;
        int earnedPoints = 0;

        for (Question question : quiz.getQuestions()) {
            System.out.println("  ❓ Pregunta: " + question.getContent() + " (Pts: " + question.getPoints() + ")");
            totalPoints += question.getPoints();

            // Buscamos qué respondió el usuario
            UUID selectedOptionId = submission.answers().get(question.getId());
            System.out.println("     👉 Usuario eligió ID: " + selectedOptionId);

            if (selectedOptionId != null) {
                // Buscamos la opción en la BD
                Option selectedOption = question.getOptions().stream()
                        .filter(o -> o.getId().equals(selectedOptionId))
                        .findFirst()
                        .orElse(null);

                if (selectedOption != null) {
                    System.out.println("     ✅ Opción encontrada en BD: " + selectedOption.getText());
                    System.out.println("     🏆 ¿Es correcta?: " + selectedOption.isCorrect());

                    if (selectedOption.isCorrect()) {
                        earnedPoints += question.getPoints();
                        System.out.println("     🎉 ¡PUNTOS SUMADOS!");
                    }
                } else {
                    System.out.println("     ❌ ERROR: El ID que envió el usuario NO existe en esta pregunta.");
                    // Esto pasa si editaste el examen mientras el alumno lo hacía (IDs viejos vs
                    // nuevos)
                }
            } else {
                System.out.println("     ⚪ Usuario no respondió a esta pregunta.");
            }
        }

        int score = (totalPoints > 0) ? (int) ((earnedPoints * 100.0) / totalPoints) : 0;
        boolean passed = score >= 50;

        System.out.println("📊 NOTA FINAL: " + score + " (Aprobado: " + passed + ")");
        System.out.println("-----------------------------");

        return new QuizResultDTO(score, passed);
    }
}