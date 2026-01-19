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

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;

    @Transactional // O se guarda todo o no se guarda nada
    public Quiz createQuiz(@Valid QuizDTO request) {
        // Buscamos la lección asociada
        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // Creamos el examen
        Quiz quiz = new Quiz();
        quiz.setTitle(request.title());
        quiz.setLesson(lesson);

        // Mapeamos las preguntas y opciones
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
}