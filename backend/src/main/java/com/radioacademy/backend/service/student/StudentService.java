package com.radioacademy.backend.service.student;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.*;
import com.radioacademy.backend.dto.lesson.LessonDTO;
import com.radioacademy.backend.dto.module.ModuleDTO;
import com.radioacademy.backend.entity.*;
import com.radioacademy.backend.repository.*;
import com.radioacademy.backend.repository.exams.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        // Security Check
        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Progress Calculation
        Set<UUID> completedLessonIds = progressRepository.findCompletedLessonIdsByUserId(user.getId());
        long totalLessons = lessonRepository.countByCourseId(courseId);
        long completedInThisCourse = progressRepository.countCompletedLessons(user.getId(), courseId);
        int progressPercentage = calculatePercentage(completedInThisCourse, totalLessons);

        // Complex Mapping: Course -> Modules -> Lessons
        List<ModuleDTO> sectionDTOs = course.getModules().stream()
                .sorted(Comparator.comparingInt(m -> m.getOrderIndex())).map(module -> new ModuleDTO(
                        module.getId(),
                        module.getTitle(),
                        module.getOrderIndex(),
                        module.getLessons().stream()
                                .distinct()
                                .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                                .map(lesson -> {
                                    boolean isCompleted = completedLessonIds.contains(lesson.getId());
                                    UUID quizId = (lesson.getQuiz() != null) ? lesson.getQuiz().getId() : null;

                                    return new LessonDTO(
                                            lesson.getId(),
                                            lesson.getTitle(),
                                            lesson.getVideoUrl(),
                                            lesson.getPdfUrl(),
                                            lesson.getDuration(),
                                            isCompleted,
                                            quizId);
                                })
                                .collect(Collectors.toList())))
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

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getLesson().getId(), questions);
    }

    // ✅ 4. CORREGIR EXAMEN
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission) {
        Quiz quiz = quizRepository.findById(submission.quizId())
                .orElseThrow(() -> new EntityNotFoundException("Examen no encontrado"));

        int totalPoints = 0;
        int earnedPoints = 0;

        for (Question question : quiz.getQuestions()) {
            totalPoints += question.getPoints();

            // Look up student's answer
            UUID selectedOptionId = submission.answers().get(question.getId());

            if (selectedOptionId != null) {
                Option selectedOption = question.getOptions().stream()
                        .filter(o -> o.getId().equals(selectedOptionId))
                        .findFirst()
                        .orElse(null);

                if (selectedOption != null && selectedOption.isCorrect()) {
                    earnedPoints += question.getPoints();
                }
            }
        }

        int score = (totalPoints > 0) ? (int) ((earnedPoints * 100.0) / totalPoints) : 0;
        boolean passed = score >= 50;

        // Optional: Log result
        log.info("Quiz submitted: ID={} | Score={} | Passed={}", quiz.getId(), score, passed);

        return new QuizResultDTO(score, passed);
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
}