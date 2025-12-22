package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseDropdownDTO;
import com.radioacademy.backend.dto.UserListDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;

        // ==========================================
        // 1. LISTADO DE USUARIOS (OPTIMIZADO)
        // ==========================================
        @GetMapping("/users")
        public ResponseEntity<List<UserListDTO>> getAllUsers() {
                // 1. Hibernate hace una sola consulta SELECT * FROM users
                List<User> users = userRepository.findAll();

                // 2. Transformamos la entidad pesada (User) en un objeto ligero (DTO)
                // Esto evita que se envíen contraseñas, fechas innecesarias o relaciones
                // cíclicas.
                List<UserListDTO> dtos = users.stream()
                                .map(u -> new UserListDTO(
                                                u.getId(),
                                                u.getName() + " " + u.getSurname(), // Concatenamos para facilitar la
                                                                                    // vista
                                                u.getEmail(),
                                                u.getDni(),
                                                u.getRole().toString(),
                                                u.isEnabled()))
                                .toList();

                return ResponseEntity.ok(dtos);
        }

        // ==========================================
        // 2. LISTADO DE CURSOS LIGERO (NUEVO)
        // ==========================================
        // Este endpoint es vital para el <select> del frontend.
        // Evita cargar módulos, lecciones y videos cuando solo queremos el Título.
        @GetMapping("/courses-dropdown")
        public ResponseEntity<List<CourseDropdownDTO>> getCoursesForDropdown() {
                List<Course> courses = courseRepository.findAll();

                List<CourseDropdownDTO> dtos = courses.stream()
                                .map(c -> new CourseDropdownDTO(
                                                c.getId(), // ID del curso (UUID)
                                                c.getTitle() // Título del curso
                                ))
                                .toList();

                return ResponseEntity.ok(dtos);
        }

        // ==========================================
        // 3. MATRICULAR (ENROLL)
        // ==========================================
        @PostMapping("/enroll")
        public ResponseEntity<?> enrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {
                // Buscamos ambos o lanzamos error si no existen
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

                // Añadimos el usuario al Set de estudiantes del curso.
                // Al ser un Set, si ya estaba matriculado, no se duplica (genial, ¿no?).
                course.getStudents().add(user);

                // Guardamos el CURSO (él es el dueño de la relación en el @JoinTable)
                courseRepository.save(course);

                return ResponseEntity.ok().body("{\"message\": \"Usuario matriculado correctamente\"}");
        }

        // ==========================================
        // 4. DESMATRICULAR (UNENROLL)
        // ==========================================
        @PostMapping("/unenroll")
        public ResponseEntity<?> unenrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {
                try {
                        // Ejecutamos el borrado directo
                        courseRepository.deleteEnrollment(userId, courseId);
                        return ResponseEntity.ok().body("{\"message\": \"Matrícula cancelada correctamente\"}");
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body("{\"message\": \"Error al cancelar matrícula: " + e.getMessage() + "\"}");
                }
        }

        @GetMapping("/users/{userId}/courses")
        public ResponseEntity<List<CourseDropdownDTO>> getUserCourses(@PathVariable UUID userId) {
                List<Course> courses = courseRepository.findByStudents_Id(userId);

                // Convertimos a DTO
                List<CourseDropdownDTO> userCourses = courses.stream()
                                .map(c -> new CourseDropdownDTO(c.getId(), c.getTitle()))
                                .toList();

                return ResponseEntity.ok(userCourses);
        }

        // 6. CAMBIAR ROL DE USUARIO
        @PutMapping("/users/{userId}/role")
        public ResponseEntity<?> changeUserRole(@PathVariable UUID userId, @RequestParam String newRole) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                try {
                        // Convertimos el String (ej: "ADMIN") al Enum (Role.ADMIN)
                        user.setRole(com.radioacademy.backend.enums.Role.valueOf(newRole));
                        userRepository.save(user);
                        return ResponseEntity.ok().body("{\"message\": \"Rol actualizado correctamente\"}");
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("{\"message\": \"Rol no válido\"}");
                }
        }
}