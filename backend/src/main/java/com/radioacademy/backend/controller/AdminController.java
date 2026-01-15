package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseDropdownDTO;
import com.radioacademy.backend.dto.UserListDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment; // 👈 Usamos la Entidad
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository; // 👈 INYECCIÓN CLAVE
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;

        @Autowired
        private EnrollmentRepository enrollmentRepository; // 👈 NECESARIO

        // 1. LISTADO DE USUARIOS
        @GetMapping("/users")
        public ResponseEntity<List<UserListDTO>> getAllUsers() {
                List<User> users = userRepository.findAll();
                List<UserListDTO> dtos = users.stream()
                                .map(u -> new UserListDTO(
                                                u.getId(),
                                                u.getName() + " " + u.getSurname(),
                                                u.getEmail(),
                                                u.getDni(),
                                                u.getRole().toString(),
                                                u.isEnabled()))
                                .toList();
                return ResponseEntity.ok(dtos);
        }

        // 2. LISTADO DE CURSOS (DROPDOWN)
        @GetMapping("/courses-dropdown")
        public ResponseEntity<List<CourseDropdownDTO>> getCoursesForDropdown() {
                List<Course> courses = courseRepository.findAll();
                List<CourseDropdownDTO> dtos = courses.stream()
                                .map(c -> new CourseDropdownDTO(c.getId(), c.getTitle()))
                                .toList();
                return ResponseEntity.ok(dtos);
        }

        // =======================================================
        // 3. MATRICULAR (CORREGIDO: Usando Enrollment Entity)
        // =======================================================
        @PostMapping("/enroll")
        public ResponseEntity<?> enrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {

                // Validaciones
                User user = userRepository.findById(userId).orElse(null);
                Course course = courseRepository.findById(courseId).orElse(null);

                if (user == null || course == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Usuario o Curso no encontrados"));
                }

                // Validación de duplicados usando el Repository
                if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("message", "Este usuario YA está matriculado en este curso."));
                }

                try {
                        // ✅ FORMA CORRECTA: Crear objeto Enrollment
                        Enrollment enrollment = new Enrollment();
                        enrollment.setUser(user);
                        enrollment.setCourse(course);
                        enrollment.setAmountPaid(java.math.BigDecimal.ZERO); // Es admin, precio 0 o el del curso
                        enrollment.setEnrolledAt(LocalDateTime.now());
                        enrollment.setPaymentId("MANUAL_ADMIN"); // Marca para saber que fue manual

                        enrollmentRepository.save(enrollment);

                        return ResponseEntity.ok(Map.of("message", "Usuario matriculado correctamente"));

                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Error interno: " + e.getMessage()));
                }
        }

        // =======================================================
        // 4. DESMATRICULAR (CORREGIDO: Borrando Enrollment)
        // =======================================================
        @PostMapping("/unenroll")
        public ResponseEntity<?> unenrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {

                // Buscamos DIRECTAMENTE la matrícula específica (SQL hace el filtro)
                Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "El usuario no está matriculado en este curso."));

                try {
                        enrollmentRepository.delete(enrollment);
                        return ResponseEntity.ok(Map.of("message", "Matrícula cancelada correctamente"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Error al cancelar: " + e.getMessage()));
                }
        }

        // =======================================================
        // 5. OBTENER CURSOS DE UN USUARIO (CORREGIDO)
        // =======================================================
        // 👇 ESTE ES EL MÉTODO QUE TE FALLABA
        @GetMapping("/users/{userId}/courses")
        public ResponseEntity<List<CourseDropdownDTO>> getUserCourses(@PathVariable UUID userId) {

                // 1. Buscamos en la tabla ENROLLMENTS (Matrículas), no en Courses
                List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

                // 2. Extraemos los cursos de esas matrículas
                List<CourseDropdownDTO> userCourses = enrollments.stream()
                                .map(enrollment -> {
                                        Course c = enrollment.getCourse();
                                        return new CourseDropdownDTO(c.getId(), c.getTitle());
                                })
                                .toList();

                return ResponseEntity.ok(userCourses);
        }

        // 6. CAMBIAR ROL
        @PutMapping("/users/{userId}/role")
        public ResponseEntity<?> changeUserRole(@PathVariable UUID userId, @RequestParam String newRole) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));
                try {
                        user.setRole(Role.valueOf(newRole));
                        userRepository.save(user);
                        return ResponseEntity.ok(Map.of("message", "Rol actualizado"));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido"));
                }
        }
}