package com.radioacademy.backend.service.student;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.*;
import com.radioacademy.backend.dto.internal.LessonPdfInfo;
import com.radioacademy.backend.dto.lesson.LessonDTO;
import com.radioacademy.backend.dto.module.ModuleDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.entity.QuizAttempt;
import com.radioacademy.backend.entity.Question;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.QuizAnswer;
import com.radioacademy.backend.entity.Quiz;
import com.radioacademy.backend.entity.Option;
import com.radioacademy.backend.repository.*;
import com.radioacademy.backend.repository.exams.*;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final QuizAttemptRepository attemptRepository;
    private final StorageService storageService;

    // ✅ 1. DASHBOARD: Cursos del alumno con su progreso real
    @Transactional(readOnly = true)
    public List<CourseDashboardDTO> getMyDashboard(CustomUserDetails userDetails) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userDetails.getId());

        return enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();
            long totalLessons = lessonRepository.countByCourseId(course.getId());
            long completedLessons = progressRepository.countCompletedLessons(userDetails.getId(), course.getId());

            return new CourseDashboardDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    calculatePercentage(completedLessons, totalLessons));
        }).toList();
    }

    // ✅ 2. CONTENIDO DEL CURSO (Video Player & Estructura)
    @Transactional(readOnly = true)
    public CourseContentDTO getCourseContent(UUID courseId, CustomUserDetails userDetails) {
        // Seguridad: ¿Está matriculado?
        if (!enrollmentRepository.existsByUserIdAndCourseId(userDetails.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        Set<UUID> completedLessonIds = progressRepository.findCompletedLessonIdsByUserId(userDetails.getId());
        long totalLessons = lessonRepository.countByCourseId(courseId);
        long completedInThisCourse = progressRepository.countCompletedLessons(userDetails.getId(), courseId);

        List<ModuleDTO> sectionDTOs = course.getModules().stream()
                .sorted(Comparator.comparingInt(Module::getOrderIndex))
                .map(module -> {
                    UUID quizId = (module.getQuiz() != null) ? module.getQuiz().getId() : null;

                    return new ModuleDTO(
                            module.getId(),
                            module.getTitle(),
                            module.getOrderIndex(),
                            quizId,
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
                                            null))
                                    .collect(Collectors.toList()));
                }).toList();

        return new CourseContentDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                sectionDTOs,
                course.getCoverImage(),
                calculatePercentage(completedInThisCourse, totalLessons));
    }

    // ✅ 3. OBTENER EXAMEN (Sin respuestas correctas para el alumno)
    @Transactional(readOnly = true)
    public QuizDTO getQuizForStudent(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen no encontrado"));

        List<QuestionDTO> questions = quiz.getQuestions().stream()
                .filter(Question::isActive)
                .map(q -> new QuestionDTO(
                        q.getId(),
                        q.getContent(),
                        q.getOptions().stream()
                                .filter(Option::isActive)
                                .map(o -> new OptionDTO(o.getId(), o.getText(), false)) // No chivamos la correcta
                                .toList(),
                        q.getPoints()))
                .toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questions);
    }

    // ✅ 4. CORREGIR EXAMEN (Optimizado con Proxy de Usuario)
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission, CustomUserDetails userDetails) {
        Quiz quiz = quizRepository.findById(submission.quizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen no encontrado"));

        QuizAttempt attempt = new QuizAttempt();
        // OPTIMIZACIÓN: getReferenceById evita cargar el usuario entero de la DB
        attempt.setUser(userRepository.getReferenceById(userDetails.getId()));
        attempt.setQuiz(quiz);
        attempt.setCompletedAt(LocalDateTime.now());

        Map<UUID, Boolean> questionResults = new HashMap<>();
        Map<UUID, UUID> correctOptions = new HashMap<>();

        List<Question> activeQuestions = quiz.getQuestions().stream()
                .filter(Question::isActive).toList();

        int correctCount = 0;

        for (Question question : activeQuestions) {
            UUID userOptionId = submission.answers().get(question.getId());
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;
            if (userOptionId != null) {
                Option selectedOpt = question.getOptions().stream()
                        .filter(o -> o.getId().equals(userOptionId)).findFirst().orElse(null);

                answer.setSelectedOption(selectedOpt);
                isCorrect = selectedOpt != null && selectedOpt.isCorrect();
            }

            answer.setCorrect(isCorrect);
            attempt.getAnswers().add(answer);
            if (isCorrect)
                correctCount++;

            // Feedback para el DTO
            questionResults.put(question.getId(), isCorrect);
            question.getOptions().stream()
                    .filter(Option::isCorrect)
                    .findFirst()
                    .ifPresent(opt -> correctOptions.put(question.getId(), opt.getId()));
        }

        double score = activeQuestions.isEmpty() ? 0 : ((double) correctCount / activeQuestions.size()) * 100;
        attempt.setScore(score);
        attempt.setPassed(score >= 50.0);

        attemptRepository.save(attempt);

        return new QuizResultDTO(score, attempt.isPassed(), questionResults, correctOptions);
    }

    // ✅ 5. DESCARGAR PDF SEGURO (Validando matrícula desde el Token)
    @Transactional(readOnly = true)
    public Resource getLessonPdf(UUID lessonId, CustomUserDetails userDetails) {
        LessonPdfInfo info = lessonRepository.findPdfInfo(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        if (info.pdfUrl() == null || info.pdfUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta lección no tiene PDF");
        }

        // El ID del token decide si tienes permiso
        if (!enrollmentRepository.existsByUserIdAndCourseId(userDetails.getId(), info.courseId())) {
            log.warn("⛔ Intento de acceso no autorizado al PDF {} por usuario {}", lessonId, userDetails.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No estás matriculado en este curso.");
        }

        Resource resource = storageService.loadAsResource(info.pdfUrl());
        if (resource == null || !resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no existe.");
        }

        return resource;
    }

    // --- Helpers Privados ---
    private int calculatePercentage(long completed, long total) {
        if (total <= 0)
            return 0;
        return (int) Math.min(((completed * 100) / total), 100);
    }
}