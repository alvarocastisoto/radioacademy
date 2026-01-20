package com.radioacademy.backend.service.exams;

import org.springframework.stereotype.Service;

import com.radioacademy.backend.dto.exams.OptionDTO;
import com.radioacademy.backend.dto.exams.QuestionDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Option;
import com.radioacademy.backend.entity.Question;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.exams.QuizRepository;

import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;

    @Transactional // Importante: Maneja toda la operación como una única transacción
    public Quiz createQuiz(@Valid QuizDTO request) {

        // 1. Buscamos la lección asociada
        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // 2. BUSCAR SI YA EXISTE (Lógica de Edición)
        // Intentamos buscar un quiz existente para esta lección
        Quiz quiz = quizRepository.findByLessonId(lesson.getId())
                .orElse(new Quiz()); // Si no existe, crea una instancia nueva vacía

        // 3. Configuramos los datos básicos
        if (quiz.getId() == null) {
            // Solo si es nuevo asignamos la lección (si es edición, ya la tiene)
            quiz.setLesson(lesson);
        }
        quiz.setTitle(request.title());

        // 4. ACTUALIZAR PREGUNTAS (El truco para editar)
        // Limpiamos la lista actual. Gracias a orphanRemoval = true en la Entidad,
        // Hibernate borrará de la base de datos las preguntas viejas automáticamente.
        if (quiz.getQuestions() != null) {
            quiz.getQuestions().clear();
        }

        // 5. Mapeamos las nuevas preguntas y opciones
        if (request.questions() != null) {
            for (QuestionDTO questionDTO : request.questions()) {
                Question question = new Question();
                question.setContent(questionDTO.question());
                question.setPoints(questionDTO.points());
                question.setQuiz(quiz); // Enlazamos con el padre

                if (questionDTO.options() != null) {
                    for (OptionDTO optionDTO : questionDTO.options()) {
                        Option option = new Option();
                        option.setText(optionDTO.text());
                        option.setCorrect(optionDTO.isCorrect());
                        option.setQuestion(question); // Enlazamos con el padre

                        question.getOptions().add(option);
                    }
                }
                quiz.getQuestions().add(question);
            }
        }

        // 6. Guardamos (Hibernate decide si hace INSERT o UPDATE)
        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public Optional<QuizDTO> getQuizByLessonId(UUID lessonId) {
        return quizRepository.findByLessonId(lessonId).map(quiz -> {
            // Mapeo manual Entidad -> DTO (o usa un Mapper si tienes)
            List<QuestionDTO> questionDTOs = quiz.getQuestions().stream().map(q -> new QuestionDTO(
                    q.getId(),
                    q.getContent(),
                    q.getOptions().stream().map(o -> new OptionDTO(o.getId(), o.getText(), o.isCorrect())).toList(),
                    q.getPoints())).toList();

            return new QuizDTO(quiz.getId(), quiz.getTitle(), lessonId, questionDTOs);
        });
    }
}