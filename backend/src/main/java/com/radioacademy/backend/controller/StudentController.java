package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseContentDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.exams.QuizDTO;
import com.radioacademy.backend.dto.exams.QuizResultDTO;
import com.radioacademy.backend.dto.exams.QuizSubmissionDTO;
import com.radioacademy.backend.dto.lesson.LessonDTO;
import com.radioacademy.backend.dto.module.ModuleDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.student.StudentService; // Asegúrate de tener este servicio creado

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private CourseRepository courseRepository;
        @Autowired
        private EnrollmentRepository enrollmentRepository;
        @Autowired
        private LessonProgressRepository progressRepository;
        @Autowired
        private LessonRepository lessonRepository;
        @Autowired
        private StudentService studentService; // 👈 Inyección del servicio de lógica de estudiante

        // ✅ 1. DASHBOARD
        @GetMapping("/dashboard")
        public ResponseEntity<List<CourseDashboardDTO>> getMyDashboard() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = userRepository.findByEmail(auth.getName())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                List<CourseDashboardDTO> response = new ArrayList<>();
                List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

                for (Enrollment enrollment : enrollments) {
                        Course course = enrollment.getCourse();
                        long totalLessons = lessonRepository.countByCourseId(course.getId());
                        long completedLessons = progressRepository.countCompletedLessons(user.getId(), course.getId());

                        int percentage = 0;
                        if (totalLessons > 0) {
                                percentage = (int) ((completedLessons * 100) / totalLessons);
                        }
                        if (percentage > 100)
                                percentage = 100;

                        response.add(new CourseDashboardDTO(
                                        course.getId(),
                                        course.getTitle(),
                                        course.getDescription(),
                                        course.getCoverImage(),
                                        percentage));
                }
                return ResponseEntity.ok(response);
        }

        // ✅ 2. COURSE PLAYER CONTENT
        @GetMapping("/course/{courseId}/content")
        @Transactional(readOnly = true)
        public ResponseEntity<CourseContentDTO> getCourseContent(
                        @PathVariable UUID courseId,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
                if (!isEnrolled) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso.");
                }

                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Curso no encontrado"));

                Set<UUID> completedLessonIds = progressRepository.findCompletedLessonIdsByUserId(user.getId());
                long totalLessons = lessonRepository.countByCourseId(courseId);
                long completedInThisCourse = progressRepository.countCompletedLessons(user.getId(), courseId);

                int progressPercentage = 0;
                if (totalLessons > 0) {
                        progressPercentage = (int) ((completedInThisCourse * 100) / totalLessons);
                        if (progressPercentage > 100)
                                progressPercentage = 100;
                }

                List<ModuleDTO> sectionDTOs = course.getModules().stream()
                                .sorted((m1, m2) -> Integer.compare(m1.getOrderIndex(), m2.getOrderIndex()))
                                .map(module -> new ModuleDTO(
                                                module.getId(),
                                                module.getTitle(),
                                                module.getOrderIndex(),
                                                module.getLessons().stream()
                                                                .distinct()
                                                                .sorted((l1, l2) -> Integer.compare(l1.getOrderIndex(),
                                                                                l2.getOrderIndex()))
                                                                .map(lesson -> {
                                                                        boolean isCompleted = completedLessonIds
                                                                                        .contains(lesson.getId());

                                                                        // Mapeo seguro del Quiz ID
                                                                        UUID quizId = (lesson.getQuiz() != null)
                                                                                        ? lesson.getQuiz().getId()
                                                                                        : null;

                                                                        return new LessonDTO(
                                                                                        lesson.getId(),
                                                                                        lesson.getTitle(),
                                                                                        lesson.getVideoUrl(),
                                                                                        lesson.getPdfUrl(),
                                                                                        lesson.getDuration(),
                                                                                        isCompleted,
                                                                                        quizId // ✅
                                                                        );
                                                                })
                                                                .collect(Collectors.toList())))
                                .collect(Collectors.toList());

                CourseContentDTO content = new CourseContentDTO(
                                course.getId(),
                                course.getTitle(),
                                course.getDescription(),
                                sectionDTOs,
                                course.getCoverImage(),
                                progressPercentage);

                return ResponseEntity.ok(content);
        }

        // ✅ 3. OBTENER EXAMEN (PARA ALUMNO)
        @GetMapping("/quiz/{quizId}")
        public ResponseEntity<QuizDTO> getQuizForStudent(@PathVariable UUID quizId) {
                return ResponseEntity.ok(studentService.getQuizForStudent(quizId));
        }

        // ✅ 4. ENVIAR RESPUESTAS
        @PostMapping("/quiz/submit")
        public ResponseEntity<QuizResultDTO> submitQuiz(@RequestBody QuizSubmissionDTO submission) {
                return ResponseEntity.ok(studentService.submitQuiz(submission));
        }
}