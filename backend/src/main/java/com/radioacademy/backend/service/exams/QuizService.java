package com.radioacademy.backend.service.exams;

import com.radioacademy.backend.dto.exams.*;
import com.radioacademy.backend.entity.*;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.ModuleRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.repository.exams.QuizAttemptRepository;
import com.radioacademy.backend.repository.exams.QuizRepository;
import com.radioacademy.backend.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // 1. GESTIÓN (ADMIN): CREAR / EDITAR CON SOFT DELETE
    // =========================================================================
    @Transactional
    public QuizDTO createQuiz(QuizDTO request) {
        Module module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));

        Quiz quiz = quizRepository.findByModuleId(module.getId())
                .orElse(new Quiz());

        if (quiz.getId() == null) {
            quiz.setModule(module);
        }
        quiz.setTitle(request.title());

        List<Question> currentQuestions = quiz.getQuestions();
        Set<UUID> incomingQuestionIds = new HashSet<>();

        if (request.questions() != null) {
            for (QuestionDTO qDto : request.questions()) {
                Question question;

                // 1. Buscar si existe en la lista actual
                Optional<Question> existingQ = currentQuestions.stream()
                        .filter(q -> q.getId() != null && q.getId().equals(qDto.id()))
                        .findFirst();

                if (existingQ.isPresent()) {
                    // EDITAR EXISTENTE
                    question = existingQ.get();
                    question.setActive(true);
                    incomingQuestionIds.add(question.getId());
                } else {
                    // CREAR NUEVA
                    question = new Question();
                    question.setActive(true);
                    question.setQuiz(quiz); // Enlazar al padre
                    // IMPORTANTE: Añadir a la lista INMEDIATAMENTE para que Hibernate sepa que
                    // pertenece al grafo
                    currentQuestions.add(question);
                }

                // Actualizar datos
                question.setContent(qDto.content());
                question.setPoints(qDto.points());

                // 2. Gestionar Opciones (Delegar)
                mergeOptions(question, qDto.options());
            }
        }

        // 3. Soft Delete de preguntas no enviadas
        currentQuestions.forEach(q -> {
            if (q.getId() != null && !incomingQuestionIds.contains(q.getId())) {
                q.setActive(false);
            }
        });

        // Guardamos el Quiz completo (Cascada guardará preguntas y opciones)
        Quiz savedQuiz = quizRepository.save(quiz);

        // Convertimos a DTO dentro de la transacción para evitar
        // LazyInitializationException
        return mapQuizToDTO(savedQuiz);
    }

    private void mergeOptions(Question question, List<OptionDTO> optionDTOs) {
        if (optionDTOs == null)
            return;

        List<Option> currentOptions = question.getOptions();
        Set<UUID> incomingOptionIds = new HashSet<>();

        for (OptionDTO oDto : optionDTOs) {
            Option option;

            Optional<Option> existingOpt = currentOptions.stream()
                    .filter(o -> o.getId() != null && o.getId().equals(oDto.id()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                option = existingOpt.get();
                option.setActive(true);
                incomingOptionIds.add(option.getId());
            } else {
                option = new Option();
                option.setActive(true);
                option.setQuestion(question); // Enlazar al padre
                currentOptions.add(option); // Añadir a la lista
            }

            option.setText(oDto.text());
            option.setCorrect(oDto.isCorrect());
        }

        currentOptions.forEach(o -> {
            if (o.getId() != null && !incomingOptionIds.contains(o.getId())) {
                o.setActive(false);
            }
        });
    }

    // =========================================================================
    // 2. ALUMNO: OBTENER EXAMEN (ALEATORIO 50 PREGUNTAS)
    // =========================================================================
    @Transactional(readOnly = true)
    public QuizDTO getQuizById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 1. Obtener TODAS las activas
        List<Question> allQuestions = quiz.getQuestions().stream()
                .filter(Question::isActive)
                .collect(Collectors.toList()); // Lista mutable para shuffle

        // 2. Barajar
        Collections.shuffle(allQuestions);

        // 3. Recortar a 50 (o el máximo disponible)
        int limit = Math.min(allQuestions.size(), 50);
        List<Question> randomQuestions = allQuestions.subList(0, limit);

        // 4. Mapear a DTO (Ocultando respuestas correctas)
        List<QuestionDTO> questionDTOs = randomQuestions.stream().map(q -> new QuestionDTO(
                q.getId(),
                q.getContent(),
                q.getOptions().stream()
                        .filter(Option::isActive)
                        .map(o -> new OptionDTO(o.getId(), o.getText(), false)) // false para ocultar respuesta
                        .toList(),
                q.getPoints())).toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questionDTOs);
    }

    // =========================================================================
    // 3. ALUMNO: CORREGIR (FEEDBACK DETALLADO)
    // =========================================================================
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission, CustomUserDetails userDetails) {
        Quiz quiz = quizRepository.findById(submission.quizId()).orElseThrow();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(userRepository.getReferenceById(userDetails.getId()));
        attempt.setQuiz(quiz);
        attempt.setCompletedAt(LocalDateTime.now());

        // Mapas para el feedback visual
        Map<UUID, Boolean> questionResults = new HashMap<>();
        Map<UUID, UUID> correctOptions = new HashMap<>();

        int correctCount = 0;
        int totalQuestions = 0;

        // Iteramos sobre las respuestas enviadas por el usuario
        for (Map.Entry<UUID, UUID> entry : submission.answers().entrySet()) {
            UUID questionId = entry.getKey();
            UUID optionId = entry.getValue();

            // Buscamos la pregunta en el grafo del Quiz (más eficiente que ir a BD una por
            // una)
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElse(null);

            if (question == null)
                continue; // Seguridad
            totalQuestions++;

            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            Option selectedOpt = question.getOptions().stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElse(null);

            // Verificamos corrección
            boolean isCorrect = selectedOpt != null && selectedOpt.isCorrect();
            answer.setSelectedOption(selectedOpt);
            answer.setCorrect(isCorrect);

            attempt.getAnswers().add(answer);

            // Datos para el Feedback
            questionResults.put(questionId, isCorrect);
            if (isCorrect)
                correctCount++;

            // Guardamos la opción correcta para mostrársela si falló
            question.getOptions().stream()
                    .filter(Option::isCorrect)
                    .findFirst()
                    .ifPresent(opt -> correctOptions.put(questionId, opt.getId()));
        }

        // Cálculo de nota
        double score = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100 : 0;
        attempt.setScore(score);
        attempt.setPassed(score >= 50.0);

        attemptRepository.save(attempt);

        return new QuizResultDTO(score, attempt.isPassed(), questionResults, correctOptions);
    }

    // =========================================================================
    // 4. ALUMNO: SMART RETRY (BANCO DE FALLOS)
    // =========================================================================
    @Transactional(readOnly = true)
    public QuizDTO getSmartFailedQuiz(UUID quizId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();

        // 1. Obtener IDs que el usuario debe (último intento fue fallo)
        List<UUID> failedIds = attemptRepository.findFailedQuestionIds(user, quizId);

        if (failedIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No tienes fallos pendientes.");
        }

        // 2. Filtrar preguntas del Quiz original
        List<QuestionDTO> failedQuestions = quiz.getQuestions().stream()
                .filter(q -> failedIds.contains(q.getId()))
                .map(q -> new QuestionDTO(
                        q.getId(),
                        q.getContent(),
                        q.getOptions().stream()
                                .filter(Option::isActive)
                                .map(o -> new OptionDTO(o.getId(), o.getText(), false))
                                .toList(),
                        q.getPoints()))
                .toList();

        return new QuizDTO(
                quizId,
                "Repaso: " + quiz.getTitle(),
                quiz.getModule().getId(),
                failedQuestions);
    }

    // =========================================================================
    // 5. ADMIN: OBTENER POR MÓDULO (Para Edición)
    // =========================================================================
    @Transactional(readOnly = true)
    public Optional<QuizDTO> getQuizByModuleId(UUID moduleId) {
        return quizRepository.findByModuleId(moduleId).map(this::mapQuizToDTO);
    }

    // Mapper Auxiliar
    private QuizDTO mapQuizToDTO(Quiz quiz) {
        List<QuestionDTO> questions = quiz.getQuestions().stream()
                .filter(Question::isActive)
                .map(q -> new QuestionDTO(
                        q.getId(),
                        q.getContent(),
                        q.getOptions().stream()
                                .filter(Option::isActive)
                                .map(o -> new OptionDTO(o.getId(), o.getText(), o.isCorrect())) // Aquí SÍ mostramos
                                                                                                // correctas (es para
                                                                                                // admin/guardado)
                                .toList(),
                        q.getPoints()))
                .toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questions);
    }
}