package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseDropdownDTO;
import com.radioacademy.backend.dto.student.UserListDTO;
import com.radioacademy.backend.service.admin.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

        @Autowired
        private AdminService adminService; // 👈 Única inyección necesaria

        @GetMapping("/users")
        public ResponseEntity<List<UserListDTO>> getAllUsers() {
                return ResponseEntity.ok(adminService.getAllUsers());
        }

        @GetMapping("/courses-dropdown")
        public ResponseEntity<List<CourseDropdownDTO>> getCoursesForDropdown() {
                return ResponseEntity.ok(adminService.getCoursesForDropdown());
        }

        @PostMapping("/enroll")
        public ResponseEntity<?> enrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {
                try {
                        adminService.enrollUser(userId, courseId);
                        return ResponseEntity.ok(Map.of("message", "Usuario matriculado correctamente"));
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Error interno"));
                }
        }

        @PostMapping("/unenroll")
        public ResponseEntity<?> unenrollUser(@RequestParam UUID userId, @RequestParam UUID courseId) {
                try {
                        adminService.unenrollUser(userId, courseId);
                        return ResponseEntity.ok(Map.of("message", "Matrícula cancelada correctamente"));
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Error al cancelar"));
                }
        }

        @GetMapping("/users/{userId}/courses")
        public ResponseEntity<List<CourseDropdownDTO>> getUserCourses(@PathVariable UUID userId) {
                return ResponseEntity.ok(adminService.getUserCourses(userId));
        }

        @PutMapping("/users/{userId}/role")
        public ResponseEntity<?> changeUserRole(@PathVariable UUID userId, @RequestParam String newRole) {
                try {
                        adminService.changeUserRole(userId, newRole);
                        return ResponseEntity.ok(Map.of("message", "Rol actualizado"));
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
                }
        }
}