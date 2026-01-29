package com.radioacademy.backend.service.student;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.*;
import com.radioacademy.backend.dto.internal.LessonPdfInfo;
import com.radioacademy.backend.dto.lesson.LessonDTO;
import com.radioacademy.backend.dto.module.ModuleDTO;
import com.radioacademy.backend.entity.*;
import com.radioacademy.backend.repository.*;
import com.radioacademy.backend.repository.exams.*;
import com.radioacademy.backend.service.StorageService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final QuizRepository quizRepository;
    private final StorageService storageService;

    private final QuizAttemptRepository attemptRepository;

    // ✅ 1. DASHBOARD
    @Transactional(readOnly = true)
    public List<CourseDashboardDTO> getMyDashboard(String userEmail) {
        User user = getUser(userEmail);
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());
        List<CourseDashboardDTO> response = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            long totalLessons = lessonRepository.countByCourseId(course.getId());
            long completedLessons = progressRepository.countCompletedLessons(user.getId(), course.getId());

            int percentage = calculatePercentage(completedLessons, totalLessons);

            response.add(new CourseDashboardDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    percentage));
        }
        return response;
    }

    // ✅ 2. CONTENIDO DEL CURSO (PLAYER)
    @Transactional(readOnly = true)
    public CourseContentDTO getCourseContent(UUID courseId, String userEmail) {
        User user = getUser(userEmail);

        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        Set<UUID> completedLessonIds = progressRepository.findCompletedLessonIdsByUserId(user.getId());
        long totalLessons = lessonRepository.countByCourseId(courseId);
        long completedInThisCourse = progressRepository.countCompletedLessons(user.getId(), courseId);
        int progressPercentage = calculatePercentage(completedInThisCourse, totalLessons);

        List<ModuleDTO> sectionDTOs = course.getModules().stream()
                .sorted(Comparator.comparingInt(m -> m.getOrderIndex())).map(module -> {

                    // 🆕 LÓGICA NUEVA: El examen pertenece al Módulo
                    UUID quizId = (module.getQuiz() != null) ? module.getQuiz().getId() : null;

                    return new ModuleDTO(
                            module.getId(),
                            module.getTitle(),
                            module.getOrderIndex(),
                            quizId, // 👈 AÑADIDO: Pasamos el QuizID aquí (necesitas actualizar ModuleDTO)
                            module.getLessons().stream()
                                    .distinct()
                                    .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                                    .map(lesson -> new LessonDTO(
                                            lesson.getId(),
                                            lesson.getTitle(),
                                            lesson.getVideoUrl(),
                                            lesson.getPdfUrl(),
                                            lesson.getDuration(),
                                            completedLessonIds.contains(lesson.getId()),
                                            null // ❌ YA NO HAY QUIZ EN LECCIÓN (Ponemos null o quitamos el campo)
                    ))
                                    .collect(Collectors.toList()));
                })
                .collect(Collectors.toList());

        return new CourseContentDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                sectionDTOs,
                course.getCoverImage(),
                progressPercentage);
    }

    // ✅ 3. OBTENER EXAMEN (PARA ALUMNO)
    @Transactional(readOnly = true)
    public QuizDTO getQuizForStudent(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new EntityNotFoundException("Examen no encontrado"));

        List<QuestionDTO> questions = quiz.getQuestions().stream().map(q -> new QuestionDTO(
                q.getId(),
                q.getContent(),
                q.getOptions().stream().map(o ->
                // Security Trick: Always false sent to student
                new OptionDTO(o.getId(), o.getText(), false)).toList(),
                q.getPoints())).toList();

        // 🆕 CAMBIO: Devolvemos moduleId, no lessonId
        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questions);
    }

    // ✅ 4. CORREGIR EXAMEN
    // 3. SUBMIT (Corregido para el nuevo DTO y guardando historial)
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission, String userEmail) {
        // 1. Buscamos usuario y examen
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Quiz quiz = quizRepository.findById(submission.quizId())
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));

        // 2. Preparamos el intento (Historial)
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setCompletedAt(LocalDateTime.now());

        // 3. Mapas para el feedback visual (Lo que te faltaba)
        Map<UUID, Boolean> questionResults = new HashMap<>();
        Map<UUID, UUID> correctOptions = new HashMap<>();

        // Calculamos sobre las preguntas ACTIVAS
        List<Question> activeQuestions = quiz.getQuestions().stream()
                .filter(Question::isActive).toList();

        int totalQuestions = activeQuestions.size();
        int correctCount = 0;

        for (Question question : activeQuestions) {
            UUID userOptionId = submission.answers().get(question.getId());

            // Guardamos la respuesta en BD
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;
            Option selectedOpt = null;

            if (userOptionId != null) {
                selectedOpt = question.getOptions().stream()
                        .filter(o -> o.getId().equals(userOptionId)).findFirst().orElse(null);

                answer.setSelectedOption(selectedOpt);
                isCorrect = selectedOpt != null && selectedOpt.isCorrect();
            }

            answer.setCorrect(isCorrect);
            attempt.getAnswers().add(answer);

            if (isCorrect)
                correctCount++;

            // --- LLENAMOS LOS DATOS PARA EL DTO ---

            // 1. Decimos si acertó o falló esta pregunta
            questionResults.put(question.getId(), isCorrect);

            // 2. Buscamos cuál era la opción correcta para chivársela al usuario
            question.getOptions().stream()
                    .filter(Option::isCorrect)
                    .findFirst()
                    .ifPresent(opt -> correctOptions.put(question.getId(), opt.getId()));
        }

        // 4. Cálculos finales
        double score = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100 : 0;
        attempt.setScore(score);
        attempt.setPassed(score >= 50.0);

        // 5. Guardamos en BD
        attemptRepository.save(attempt);

        // 6. Retornamos el DTO con los 4 argumentos
        return new QuizResultDTO(
                score,
                attempt.isPassed(),
                questionResults, // ✅ Mapa de aciertos/fallos
                correctOptions // ✅ Mapa de respuestas correctas
        );
    }

    // --- Private Helpers ---
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private int calculatePercentage(long completed, long total) {
        if (total == 0)
            return 0;
        int percentage = (int) ((completed * 100) / total);
        return Math.min(percentage, 100);
    }

    // ✅ 5. DESCARGAR PDF DE LECCIÓN (SECURE)
    @Transactional(readOnly = true)
    public Resource getLessonPdf(UUID lessonId, String userEmail) {
        User user = getUser(userEmail);

        // 1. Buscamos info del PDF y Curso (Optimizado con DTO/Proyección)
        // Asumo que tienes el método findPdfInfo en LessonRepository
        LessonPdfInfo info = lessonRepository.findPdfInfo(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        // 2. Validar que existe archivo
        if (info.pdfUrl() == null || info.pdfUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta lección no tiene PDF asociado");
        }

        // 3. Validar Matrícula (El núcleo de la seguridad)
        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), info.courseId())) {
            log.warn("⛔ Acceso denegado al PDF: Usuario {} intentó acceder a lección {} sin pagar.", userEmail,
                    lessonId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No estás matriculado en este curso.");
        }

        // 4. Cargar recurso físico
        Resource resource = storageService.loadAsResource(info.pdfUrl());

        if (resource == null || !resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no existe en el servidor");
        }

        return resource;
    }

}