package com.radioacademy.backend.service.exams;

import com.radioacademy.backend.dto.exams.*; // Puedes usar * para limpiar o listar uno a uno
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Option;
import com.radioacademy.backend.entity.Question;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.exams.QuizRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;

    // 1. CREAR O EDITAR (Upsert)
    @Transactional
    public Quiz createQuiz(@Valid QuizDTO request) {
        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        Quiz quiz = quizRepository.findByLessonId(lesson.getId())
                .orElse(new Quiz());

        if (quiz.getId() == null) {
            quiz.setLesson(lesson);
        }
        quiz.setTitle(request.title());

        if (quiz.getQuestions() != null) {
            quiz.getQuestions().clear(); // Limpieza para orpahRemoval
        }

        if (request.questions() != null) {
            for (QuestionDTO questionDTO : request.questions()) {
                Question question = new Question();
                question.setContent(questionDTO.question());
                question.setPoints(questionDTO.points());
                question.setQuiz(quiz);

                if (questionDTO.options() != null) {
                    for (OptionDTO optionDTO : questionDTO.options()) {
                        Option option = new Option();
                        option.setText(optionDTO.text());
                        option.setCorrect(optionDTO.isCorrect());
                        option.setQuestion(question);
                        question.getOptions().add(option);
                    }
                }
                quiz.getQuestions().add(question);
            }
        }
        return quizRepository.save(quiz);
    }

    // 2. OBTENER POR LECCIÓN (Para Admin/Edición)
    @Transactional(readOnly = true)
    public Optional<QuizDTO> getQuizByLessonId(UUID lessonId) {
        return quizRepository.findByLessonId(lessonId).map(quiz -> {
            List<QuestionDTO> questionDTOs = quiz.getQuestions().stream().map(q -> new QuestionDTO(
                    q.getId(),
                    q.getContent(),
                    q.getOptions().stream().map(o -> new OptionDTO(o.getId(), o.getText(), o.isCorrect())).toList(),
                    q.getPoints())).toList();

            return new QuizDTO(quiz.getId(), quiz.getTitle(), lessonId, questionDTOs);
        });
    }

    // 3. OBTENER POR ID (Para Alumno/Reproductor)
    @Transactional(readOnly = true)
    public QuizDTO getQuizById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen no encontrado"));

        List<QuestionDTO> questionDTOs = quiz.getQuestions().stream().map(q -> new QuestionDTO(
                q.getId(),
                q.getContent(),
                q.getOptions().stream().map(o -> new OptionDTO(o.getId(), o.getText(), o.isCorrect())).toList(),
                q.getPoints())).toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getLesson().getId(), questionDTOs);
    }

    // 4. CORREGIR EXAMEN (Lógica de servidor)
    @Transactional(readOnly = true)
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission) {
        Quiz quiz = quizRepository.findById(submission.quizId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));

        int totalQuestions = quiz.getQuestions().size();
        if (totalQuestions == 0)
            return new QuizResultDTO(100.0, true);

        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            UUID questionId = question.getId();
            UUID userOptionId = submission.answers().get(questionId);

            if (userOptionId != null) {
                boolean isCorrect = question.getOptions().stream()
                        .anyMatch(opt -> opt.getId().equals(userOptionId) && opt.isCorrect());

                if (isCorrect)
                    correctCount++;
            }
        }

        double score = ((double) correctCount / totalQuestions) * 100;
        score = Math.round(score * 100.0) / 100.0;
        boolean passed = score >= 50.0;

        return new QuizResultDTO(score, passed);
    }
}