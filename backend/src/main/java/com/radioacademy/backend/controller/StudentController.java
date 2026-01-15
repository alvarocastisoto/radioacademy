package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseContentDTO;
import com.radioacademy.backend.dto.CourseDashboardDTO;
import com.radioacademy.backend.dto.LessonDTO;
import com.radioacademy.backend.dto.ModuleDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
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
        private EnrollmentRepository enrollmentRepository;

        @Autowired
        private LessonProgressRepository progressRepository;

        @Autowired
        private LessonRepository lessonRepository;

        // ✅ DASHBOARD (MIS CURSOS) - CORREGIDO
        @GetMapping("/dashboard")
        public ResponseEntity<List<CourseDashboardDTO>> getMyDashboard() {

                // 1. Obtener Usuario
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = userRepository.findByEmail(auth.getName())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                List<CourseDashboardDTO> response = new ArrayList<>();

                // 2. Buscar Matrículas (Enrollments)
                List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

                // 3. Calcular progreso para cada curso
                for (Enrollment enrollment : enrollments) {
                        Course course = enrollment.getCourse();

                        // A. Contamos lecciones totales del curso
                        long totalLessons = lessonRepository.countByCourseId(course.getId());

                        // B. Contamos lecciones completadas por ESTE usuario en ESTE curso
                        long completedLessons = progressRepository.countCompletedLessons(user.getId(), course.getId());

                        // C. Calculamos porcentaje (evitando división por cero)
                        int percentage = 0;
                        if (totalLessons > 0) {
                                percentage = (int) ((completedLessons * 100) / totalLessons);
                        }

                        // Pequeña seguridad por si los datos se desincronizan
                        if (percentage > 100)
                                percentage = 100;

                        // D. Añadir al DTO
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

                // 1. Obtener Usuario
                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                // 2. Seguridad: ¿Está matriculado?
                boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
                if (!isEnrolled) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "No tienes acceso a este curso. Debes comprarlo primero.");
                }

                // 3. Obtener el curso
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Curso no encontrado"));

                // 4. 🚀 OPTIMIZACIÓN: Traer todos los IDs de lecciones completadas por el
                // usuario de golpe
                // Esto evita hacer una consulta por cada lección (N+1 problem)
                Set<UUID> completedLessonIds = progressRepository.findCompletedLessonIdsByUserId(user.getId());

                // 5. Calcular Progreso Global del Curso
                long totalLessons = lessonRepository.countByCourseId(courseId);
                // Filtramos las lecciones completadas que PERTENECEN a este curso
                long completedInThisCourse = progressRepository.countCompletedLessons(user.getId(), courseId);

                int progressPercentage = 0;
                if (totalLessons > 0) {
                        progressPercentage = (int) ((completedInThisCourse * 100) / totalLessons);
                        if (progressPercentage > 100)
                                progressPercentage = 100;
                }

                // 6. Mapear Módulos y Lecciones (Inyectando el estado 'isCompleted')
                List<ModuleDTO> sectionDTOs = course.getModules().stream()
                                .sorted((m1, m2) -> Integer.compare(m1.getOrderIndex(), m2.getOrderIndex())) // Asegurar
                                                                                                             // orden
                                .map(module -> new ModuleDTO(
                                                module.getId(),
                                                module.getTitle(),
                                                module.getOrderIndex(),
                                                module.getLessons().stream()
                                                                .sorted((l1, l2) -> Integer.compare(l1.getOrderIndex(),
                                                                                l2.getOrderIndex())) // Asegurar orden
                                                                                                     // lecciones
                                                                .map(lesson -> {
                                                                        // ⚡ Aquí comprobamos si está completada mirando
                                                                        // el Set (es instantáneo)
                                                                        boolean isCompleted = completedLessonIds
                                                                                        .contains(lesson.getId());

                                                                        return new LessonDTO(
                                                                                        lesson.getId(),
                                                                                        lesson.getTitle(),
                                                                                        lesson.getVideoUrl(),
                                                                                        lesson.getPdfUrl(),
                                                                                        lesson.getDuration(), // Usamos
                                                                                                              // la
                                                                                                              // duración
                                                                                                              // real de
                                                                                                              // la BD
                                                                                        isCompleted // ✅ Estado real
                                                                        );
                                                                })
                                                                .toList()))
                                .toList();

                // 7. Construir respuesta final
                CourseContentDTO content = new CourseContentDTO(
                                course.getId(),
                                course.getTitle(),
                                course.getDescription(),
                                sectionDTOs,
                                course.getCoverImage(),
                                progressPercentage // ✅ Progreso real
                );

                return ResponseEntity.ok(content);
        }
}