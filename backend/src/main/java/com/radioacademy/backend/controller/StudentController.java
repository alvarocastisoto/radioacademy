package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.StudentCourseDTO;
import com.radioacademy.backend.entity.Course; // Importante
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository; // Importante
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.dto.CourseContentDTO;
import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.dto.CreateModuleRequest;
import com.radioacademy.backend.dto.LessonDTO;
import com.radioacademy.backend.dto.ModuleDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/student")
public class StudentController {

        @Autowired
        private UserRepository userRepository;

        @Autowired // <--- Inyectamos esto
        private CourseRepository courseRepository;

        @GetMapping("/my-courses")
        public ResponseEntity<List<StudentCourseDTO>> getMyCourses(Authentication authentication) {
                // 1. Obtenemos el email del token
                String email = authentication.getName();

                // 2. Buscamos al usuario solo para tener su ID
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // 3. 🔥 CORRECCIÓN: Usamos el Repositorio directamente
                // "Búscame los cursos donde esté matriculado este ID de usuario"
                // Esto evita el error de LazyInitializationException
                List<Course> courses = courseRepository.findByStudents_Id(user.getId());

                // 4. Convertimos a DTO
                List<StudentCourseDTO> dtos = courses.stream()
                                .map(course -> new StudentCourseDTO(
                                                course.getId(),
                                                course.getTitle(),
                                                course.getDescription(),
                                                0 // Progreso dummy por ahora
                                ))
                                .toList();

                return ResponseEntity.ok(dtos);
        }

        // ... imports

        @GetMapping("/course/{courseId}/content")
        @Transactional(readOnly = true)
        public ResponseEntity<CourseContentDTO> getCourseContent(
                        @PathVariable UUID courseId,
                        Authentication authentication) {
                String email = authentication.getName();

                // 1. Obtenemos el usuario SOLO para sacar su ID (es seguro)
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // 2. 🔥 CONSULTA BLINDADA:
                // Buscamos el curso Y verificamos matrícula al mismo tiempo.
                // Si no está matriculado, esto devolverá "empty" y lanzaremos error.
                Course course = courseRepository.findByIdAndStudents_Id(courseId, user.getId())
                                .orElseThrow(() -> new RuntimeException("No tienes acceso a este curso o no existe"));

                // 3. Mapeamos a DTO (Esto sigue igual que antes)
                // Recuerda usar course.getModules() o getSections() según tu entidad
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
                                                                                0, // Duration 0
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