package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseContentDTO;
import com.radioacademy.backend.dto.CourseDashboardDTO;
import com.radioacademy.backend.dto.LessonDTO;
import com.radioacademy.backend.dto.ModuleDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment; // 👈 Importante
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository; // 👈 Importante
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository; // 👈 INYECTAMOS ESTO

    @Autowired
    private LessonProgressRepository progressRepository;

    @Autowired
    private LessonRepository lessonRepository;

    // ✅ DASHBOARD (MIS CURSOS) - CORREGIDO
    @GetMapping("/dashboard")
    public ResponseEntity<List<CourseDashboardDTO>> getMyDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<CourseDashboardDTO> response = new ArrayList<>();

        // 🛑 CAMBIO CRÍTICO: Buscamos en las matrículas, no en la relación directa
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse(); // Obtenemos el curso desde la matrícula

            long totalLessons = lessonRepository.countByCourseId(course.getId());
            long completedLessons = progressRepository.countCompletedLessons(user.getId(), course.getId());

            int percentage = 0;
            if (totalLessons > 0) {
                percentage = (int) ((completedLessons * 100) / totalLessons);
            }
            if (percentage > 100) percentage = 100;

            response.add(new CourseDashboardDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    percentage));
        }

        return ResponseEntity.ok(response);
    }

    // ✅ PLAYER (CONTENIDO DEL CURSO) - CORREGIDO
    @GetMapping("/course/{courseId}/content")
    @Transactional(readOnly = true)
    public ResponseEntity<CourseContentDTO> getCourseContent(
            @PathVariable UUID courseId,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 🛑 CAMBIO CRÍTICO: Verificamos si existe la matrícula para dar acceso
        boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
        
        if (!isEnrolled) {
            throw new RuntimeException("⛔ No tienes permiso para ver este curso. Debes comprarlo primero.");
        }

        // Si tiene permiso, buscamos el curso normal
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        List<ModuleDTO> sectionDTOs = course.getModules().stream()
                .map(module -> new ModuleDTO(
                        module.getId(),
                        module.getTitle(),
                        module.getOrderIndex(),
                        module.getLessons().stream()
                                .map(lesson -> new LessonDTO(
                                        lesson.getId(),
                                        lesson.getTitle(),
                                        lesson.getVideoUrl(),
                                        lesson.getPdfUrl(),
                                        0,
                                        false))
                                .toList()))
                .toList();

        CourseContentDTO content = new CourseContentDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                sectionDTOs,
                course.getCoverImage(),
                0);

        return ResponseEntity.ok(content);
    }
}