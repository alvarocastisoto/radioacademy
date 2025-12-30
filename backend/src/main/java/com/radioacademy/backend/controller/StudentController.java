package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseContentDTO;
import com.radioacademy.backend.dto.CourseDashboardDTO;
import com.radioacademy.backend.dto.LessonDTO;
import com.radioacademy.backend.dto.ModuleDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
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
        private LessonProgressRepository progressRepository;

        @Autowired
        private LessonRepository lessonRepository;

        // ✅ ESTE ES EL MÉTODO BUENO (DASHBOARD CON PROGRESO REAL)
        @GetMapping("/dashboard")
        public ResponseEntity<List<CourseDashboardDTO>> getMyDashboard() {
                // 1. Obtener usuario
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = userRepository.findByEmail(auth.getName())
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                List<CourseDashboardDTO> response = new ArrayList<>();

                // ⚠️ CAMBIO CLAVE AQUÍ:
                // En vez de usar user.getEnrolledCourses(), buscamos directamente en BD.
                // Esto evita que la lista salga vacía por problemas de sesión/transacción.
                List<Course> myCourses = courseRepository.findByStudents_Id(user.getId());

                // 2. Iteramos sobre la lista que acabamos de recuperar
                for (Course course : myCourses) {

                        // A. Contar lecciones totales
                        long totalLessons = lessonRepository.countByCourseId(course.getId());

                        // B. Contar lecciones vistas
                        long completedLessons = progressRepository.countCompletedLessons(user.getId(), course.getId());

                        // Debug
                        System.out.println("---- CURSO: " + course.getTitle() + " ----");
                        System.out.println("   Vistas: " + completedLessons + " / " + totalLessons);

                        // C. Calcular porcentaje
                        int percentage = 0;
                        if (totalLessons > 0) {
                                percentage = (int) ((completedLessons * 100) / totalLessons);
                        }
                        if (percentage > 100)
                                percentage = 100;

                        // D. Añadir al DTO (con null en imagen)
                        response.add(new CourseDashboardDTO(
                                        course.getId(),
                                        course.getTitle(),
                                        course.getDescription(),
                                        null,
                                        percentage));
                }

                return ResponseEntity.ok(response);
        }

        // ✅ MÉTODO PARA ENTRAR A VER EL CURSO (PLAYER)
        @GetMapping("/course/{courseId}/content")
        @Transactional(readOnly = true)
        public ResponseEntity<CourseContentDTO> getCourseContent(
                        @PathVariable UUID courseId,
                        Authentication authentication) {

                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // Buscamos curso y verificamos matrícula
                Course course = courseRepository.findByIdAndStudents_Id(courseId, user.getId())
                                .orElseThrow(() -> new RuntimeException("No tienes acceso a este curso o no existe"));

                // Mapeo a DTOs
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
                                                                                0, // Duration
                                                                                false))
                                                                .toList()))
                                .toList();

                CourseContentDTO content = new CourseContentDTO(
                                course.getId(),
                                course.getTitle(),
                                course.getDescription(),
                                sectionDTOs,
                                0);

                return ResponseEntity.ok(content);
        }
}